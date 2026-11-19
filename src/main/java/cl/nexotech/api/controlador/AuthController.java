package cl.nexotech.api.controlador;

import cl.nexotech.api.dto.LoginRequest;
import cl.nexotech.api.dto.PerfilRequest;
import cl.nexotech.api.dto.RegistroRequest;
import cl.nexotech.api.dto.SesionResponse;
import cl.nexotech.api.dto.UsuarioResponse;
import cl.nexotech.api.excepcion.ErrorRespuesta;
import cl.nexotech.api.seguridad.JwtService;
import cl.nexotech.api.seguridad.UsuarioAutenticado;
import cl.nexotech.api.servicio.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Registro, inicio de sesión con JWT, renovación del token y perfil propio")
public class AuthController {

    static final String COOKIE_RENOVACION = "refresh_token";
    private static final String RUTA_COOKIE = "/api/auth";

    private final AuthService servicio;
    private final JwtService jwt;
    private final boolean cookieSegura;
    private final String sameSite;

    public AuthController(AuthService servicio,
                          JwtService jwt,
                          @Value("${jwt.cookie-secure}") boolean cookieSegura,
                          @Value("${jwt.cookie-same-site}") String sameSite) {
        this.servicio = servicio;
        this.jwt = jwt;
        this.cookieSegura = cookieSegura;
        this.sameSite = sameSite;
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión",
            description = "Devuelve el token de acceso (JWT) y deja el token de renovación en una cookie HttpOnly.")
    @ApiResponse(responseCode = "401", description = "Correo o contraseña incorrectos",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public ResponseEntity<SesionResponse> login(@Valid @RequestBody LoginRequest datos) {
        return respuestaConCookie(HttpStatus.OK, servicio.login(datos));
    }

    @PostMapping("/registro")
    @Operation(summary = "Crear una cuenta de cliente",
            description = "Crea la cuenta con rol cliente e inicia sesión de inmediato.")
    @ApiResponse(responseCode = "400", description = "Datos inválidos",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "409", description = "El correo o RUN ya tiene cuenta",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public ResponseEntity<SesionResponse> registro(@Valid @RequestBody RegistroRequest datos) {
        return respuestaConCookie(HttpStatus.CREATED, servicio.registrar(datos));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renovar el token de acceso",
            description = "Usa la cookie de renovación (la envía el navegador sola). Sirve para restaurar la sesión al recargar la página.")
    @ApiResponse(responseCode = "401", description = "No hay sesión o el token de renovación venció",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public ResponseEntity<SesionResponse> renovar(
            @Parameter(hidden = true) @CookieValue(name = COOKIE_RENOVACION, required = false) String tokenRenovacion) {
        return respuestaConCookie(HttpStatus.OK, servicio.renovar(tokenRenovacion));
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesión", description = "Elimina la cookie del token de renovación.")
    public ResponseEntity<Map<String, Boolean>> logout() {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString())
                .body(Map.of("ok", true));
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Ver mi perfil", description = "Devuelve los datos del usuario del token.")
    public UsuarioResponse yo(@AuthenticationPrincipal UsuarioAutenticado actual) {
        return servicio.perfil(actual.id());
    }

    @PutMapping("/perfil")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Editar mi perfil",
            description = "Cambia nombre, apellidos y dirección. Para cambiar la contraseña se envían passwordActual y passwordNueva.")
    @ApiResponse(responseCode = "400", description = "Datos inválidos o contraseña actual incorrecta",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public UsuarioResponse actualizarPerfil(@AuthenticationPrincipal UsuarioAutenticado actual,
                                            @Valid @RequestBody PerfilRequest datos) {
        return servicio.actualizarPerfil(actual.id(), datos);
    }

    private ResponseEntity<SesionResponse> respuestaConCookie(HttpStatus estado, AuthService.Resultado resultado) {
        ResponseCookie cookie = cookie(resultado.tokenRenovacion(), jwt.duracionRenovacion());
        return ResponseEntity.status(estado)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(resultado.sesion());
    }

    private ResponseCookie cookie(String valor, Duration duracion) {
        return ResponseCookie.from(COOKIE_RENOVACION, valor)
                .httpOnly(true)          // JavaScript no puede leerla: protege el token de renovación ante XSS
                .secure(cookieSegura)
                .sameSite(sameSite)
                .path(RUTA_COOKIE)       // el navegador solo la envía a /api/auth
                .maxAge(duracion)
                .build();
    }

}
