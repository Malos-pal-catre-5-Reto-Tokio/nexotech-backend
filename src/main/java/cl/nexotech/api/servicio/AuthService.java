package cl.nexotech.api.servicio;

import cl.nexotech.api.dto.LoginRequest;
import cl.nexotech.api.dto.PerfilRequest;
import cl.nexotech.api.dto.RegistroRequest;
import cl.nexotech.api.dto.SesionResponse;
import cl.nexotech.api.dto.UsuarioResponse;
import cl.nexotech.api.excepcion.ApiException;
import cl.nexotech.api.modelo.Rol;
import cl.nexotech.api.modelo.Usuario;
import cl.nexotech.api.repositorio.UsuarioRepository;
import cl.nexotech.api.seguridad.JwtService;
import io.jsonwebtoken.JwtException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final int PASSWORD_MIN = 4;
    private static final int PASSWORD_MAX = 10;

    /** Sesión lista para responder: el token de renovación lo envía el controlador en una cookie. */
    public record Resultado(SesionResponse sesion, String tokenRenovacion) {
    }

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final String hashFalso;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder encoder, JwtService jwt) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.jwt = jwt;
        // Se compara contra este hash cuando el correo no existe, para que la respuesta tarde lo mismo
        // y no se pueda descubrir qué correos tienen cuenta midiendo el tiempo.
        this.hashFalso = encoder.encode("contrasena-inexistente");
    }

    @Transactional(readOnly = true)
    public Resultado login(LoginRequest datos) {
        Usuario usuario = usuarios.findByCorreo(datos.correo()).orElse(null);
        boolean correcta = encoder.matches(datos.password(), usuario != null ? usuario.getPasswordHash() : hashFalso);
        if (usuario == null || !correcta) {
            throw ApiException.noAutorizado("Correo o contraseña incorrectos.");
        }
        return emitir(usuario);
    }

    @Transactional
    public Resultado registrar(RegistroRequest datos) {
        if (usuarios.existsByCorreo(datos.correo()) || usuarios.existsByRun(datos.run())) {
            throw ApiException.conflicto("Ya existe una cuenta con ese correo o RUN.");
        }
        Usuario usuario = new Usuario();
        usuario.setRun(datos.run());
        usuario.setNombre(datos.nombre());
        usuario.setApellidos(datos.apellidos());
        usuario.setCorreo(datos.correo());
        usuario.setPasswordHash(encoder.encode(datos.password()));
        usuario.setRol(Rol.CLIENTE); // el rol nunca viene del navegador
        usuario.setRegion(datos.region());
        usuario.setComuna(datos.comuna());
        usuario.setDireccion(datos.direccion());
        return emitir(usuarios.save(usuario));
    }

    /** Cambia un token de renovación válido por un nuevo par de tokens (el de renovación también se rota). */
    @Transactional(readOnly = true)
    public Resultado renovar(String tokenRenovacion) {
        if (tokenRenovacion == null || tokenRenovacion.isBlank()) {
            throw ApiException.noAutorizado("Debes iniciar sesión.");
        }
        JwtService.DatosToken datos;
        try {
            datos = jwt.leer(tokenRenovacion);
        } catch (JwtException | IllegalArgumentException e) {
            throw ApiException.noAutorizado("Tu sesión expiró. Inicia sesión nuevamente.");
        }
        if (!datos.esRenovacion()) {
            throw ApiException.noAutorizado("Tu sesión expiró. Inicia sesión nuevamente.");
        }
        // Se vuelve a leer el usuario: si lo eliminaron o le cambiaron el rol, el nuevo token lo refleja.
        Usuario usuario = usuarios.findById(datos.usuarioId())
                .orElseThrow(() -> ApiException.noAutorizado("Tu sesión expiró. Inicia sesión nuevamente."));
        return emitir(usuario);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse perfil(Long usuarioId) {
        return UsuarioResponse.de(buscar(usuarioId));
    }

    @Transactional
    public UsuarioResponse actualizarPerfil(Long usuarioId, PerfilRequest datos) {
        Usuario usuario = buscar(usuarioId);
        String nueva = datos.passwordNueva();
        if (nueva != null && !nueva.isEmpty()) {
            if (nueva.length() < PASSWORD_MIN || nueva.length() > PASSWORD_MAX) {
                throw ApiException.solicitudInvalida(
                        "La contraseña debe tener entre " + PASSWORD_MIN + " y " + PASSWORD_MAX + " caracteres.");
            }
            String actual = datos.passwordActual() == null ? "" : datos.passwordActual();
            if (!encoder.matches(actual, usuario.getPasswordHash())) {
                throw ApiException.solicitudInvalida("La contraseña actual no es correcta.");
            }
            usuario.setPasswordHash(encoder.encode(nueva));
        }
        usuario.setNombre(datos.nombre());
        usuario.setApellidos(datos.apellidos());
        usuario.setRegion(datos.region());
        usuario.setComuna(datos.comuna());
        usuario.setDireccion(datos.direccion());
        return UsuarioResponse.de(usuario);
    }

    private Usuario buscar(Long id) {
        return usuarios.findById(id).orElseThrow(() -> ApiException.noAutorizado("Debes iniciar sesión."));
    }

    private Resultado emitir(Usuario usuario) {
        SesionResponse sesion = new SesionResponse(
                jwt.generarAcceso(usuario), jwt.segundosAcceso(), UsuarioResponse.de(usuario));
        return new Resultado(sesion, jwt.generarRenovacion(usuario));
    }
}
