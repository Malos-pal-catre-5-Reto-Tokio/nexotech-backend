package cl.nexotech.api.seguridad;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Se ejecuta en cada petición: si trae "Authorization: Bearer <token>" con un token de acceso válido,
 * deja al usuario autenticado con su rol. Si el token falta o es inválido no autentica nada y Spring Security
 * responde 401 en las rutas que exigen sesión.
 */
public class FiltroJwt extends OncePerRequestFilter {

    private static final String PREFIJO = "Bearer ";

    private final JwtService jwt;

    public FiltroJwt(JwtService jwt) {
        this.jwt = jwt;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain cadena)
            throws ServletException, IOException {
        String cabecera = request.getHeader("Authorization");
        if (cabecera != null && cabecera.startsWith(PREFIJO)) {
            try {
                JwtService.DatosToken datos = jwt.leer(cabecera.substring(PREFIJO.length()).trim());
                if (datos.esAcceso()) {
                    UsuarioAutenticado principal = new UsuarioAutenticado(datos.usuarioId(), datos.correo(), datos.rol());
                    var autenticacion = new UsernamePasswordAuthenticationToken(
                            principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + datos.rol().name())));
                    SecurityContextHolder.getContext().setAuthentication(autenticacion);
                }
            } catch (JwtException | IllegalArgumentException e) {
                SecurityContextHolder.clearContext();
            }
        }
        cadena.doFilter(request, response);
    }
}
