package cl.nexotech.api.seguridad;

import cl.nexotech.api.modelo.Rol;
import cl.nexotech.api.modelo.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Genera y verifica los tokens JWT.
 * <ul>
 *   <li><b>Token de acceso</b>: dura pocos minutos y se envía en cada petición (Authorization: Bearer ...).</li>
 *   <li><b>Token de renovación (refresh)</b>: dura días, viaja en una cookie HttpOnly y solo sirve para pedir un nuevo token de acceso.</li>
 * </ul>
 */
@Service
public class JwtService {

    private static final String TIPO_ACCESO = "access";
    private static final String TIPO_RENOVACION = "refresh";

    /** Datos que se leen de un token ya verificado. */
    public record DatosToken(Long usuarioId, String correo, Rol rol, String tipo) {
        public boolean esAcceso() {
            return TIPO_ACCESO.equals(tipo);
        }

        public boolean esRenovacion() {
            return TIPO_RENOVACION.equals(tipo);
        }
    }

    private final SecretKey clave;
    private final Duration duracionAcceso;
    private final Duration duracionRenovacion;

    public JwtService(@Value("${jwt.secret}") String secreto,
                      @Value("${jwt.access-minutos}") long accesoMinutos,
                      @Value("${jwt.refresh-dias}") long renovacionDias) {
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 32 caracteres. "
                    + "Genera una con: openssl rand -base64 48");
        }
        this.clave = Keys.hmacShaKeyFor(bytes);
        this.duracionAcceso = Duration.ofMinutes(accesoMinutos);
        this.duracionRenovacion = Duration.ofDays(renovacionDias);
    }

    public String generarAcceso(Usuario usuario) {
        return generar(usuario, TIPO_ACCESO, duracionAcceso);
    }

    public String generarRenovacion(Usuario usuario) {
        return generar(usuario, TIPO_RENOVACION, duracionRenovacion);
    }

    public long segundosAcceso() {
        return duracionAcceso.toSeconds();
    }

    public Duration duracionRenovacion() {
        return duracionRenovacion;
    }

    /**
     * Verifica la firma y la fecha de expiración del token y devuelve sus datos.
     *
     * @throws JwtException si el token está alterado, vencido o mal formado
     */
    public DatosToken leer(String token) {
        Claims claims = Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).getPayload();
        try {
            return new DatosToken(
                    Long.valueOf(claims.getSubject()),
                    claims.get("correo", String.class),
                    Rol.desde(claims.get("rol", String.class)),
                    claims.get("tipo", String.class));
        } catch (IllegalArgumentException e) {
            throw new JwtException("El contenido del token no es válido.", e);
        }
    }

    private String generar(Usuario usuario, String tipo, Duration duracion) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(usuario.getId().toString())
                .claim("correo", usuario.getCorreo())
                .claim("rol", usuario.getRol().getValor())
                .claim("tipo", tipo)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(duracion)))
                .signWith(clave)
                .compact();
    }
}
