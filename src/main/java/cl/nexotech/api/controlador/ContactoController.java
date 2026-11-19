package cl.nexotech.api.controlador;

import cl.nexotech.api.dto.ContactoRequest;
import cl.nexotech.api.dto.ContactoResponse;
import cl.nexotech.api.excepcion.ErrorRespuesta;
import cl.nexotech.api.servicio.ContactoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contacto")
@Tag(name = "Contacto", description = "Formulario de contacto de la tienda")
public class ContactoController {

    private final ContactoService servicio;

    public ContactoController(ContactoService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Enviar un mensaje de contacto", description = "Público: no requiere iniciar sesión.")
    @ApiResponse(responseCode = "400", description = "Datos inválidos",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public Map<String, Boolean> enviar(@Valid @RequestBody ContactoRequest datos) {
        servicio.recibir(datos);
        return Map.of("ok", true);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Listar los mensajes recibidos (admin y vendedor)")
    @ApiResponse(responseCode = "403", description = "Solo admin y vendedor",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public List<ContactoResponse> listar() {
        return servicio.listar();
    }
}
