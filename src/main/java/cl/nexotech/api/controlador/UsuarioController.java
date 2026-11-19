package cl.nexotech.api.controlador;

import cl.nexotech.api.dto.UsuarioRequest;
import cl.nexotech.api.dto.UsuarioResponse;
import cl.nexotech.api.excepcion.ErrorRespuesta;
import cl.nexotech.api.seguridad.UsuarioAutenticado;
import cl.nexotech.api.servicio.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Usuarios", description = "Gestión de usuarios y roles (solo administrador)")
@ApiResponse(responseCode = "401", description = "Sin sesión iniciada",
        content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
@ApiResponse(responseCode = "403", description = "Solo el administrador puede usar este recurso",
        content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
public class UsuarioController {

    private final UsuarioService servicio;

    public UsuarioController(UsuarioService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Listar usuarios")
    public List<UsuarioResponse> listar() {
        return servicio.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un usuario por id")
    @ApiResponse(responseCode = "404", description = "El usuario no existe",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public UsuarioResponse obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear un usuario", description = "El administrador elige el rol. La contraseña es obligatoria.")
    @ApiResponse(responseCode = "409", description = "El correo o RUN ya existe",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public UsuarioResponse crear(@Valid @RequestBody UsuarioRequest datos) {
        return servicio.crear(datos);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar un usuario",
            description = "La contraseña solo cambia si se envía una nueva. Un administrador no puede quitarse su propio rol.")
    @ApiResponse(responseCode = "404", description = "El usuario no existe",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "409", description = "Correo o RUN repetido, o intento de quitarse el rol de administrador",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public UsuarioResponse actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioRequest datos,
                                      @AuthenticationPrincipal UsuarioAutenticado actual) {
        return servicio.actualizar(id, datos, actual);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar un usuario", description = "No se puede eliminar la propia cuenta. Sus órdenes se conservan.")
    @ApiResponse(responseCode = "404", description = "El usuario no existe",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "409", description = "Intento de eliminar la propia cuenta",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public void eliminar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado actual) {
        servicio.eliminar(id, actual);
    }
}
