package cl.nexotech.api.controlador;

import cl.nexotech.api.dto.OrdenRequest;
import cl.nexotech.api.dto.OrdenResponse;
import cl.nexotech.api.dto.OrdenResumenResponse;
import cl.nexotech.api.dto.PagoRequest;
import cl.nexotech.api.excepcion.ErrorRespuesta;
import cl.nexotech.api.seguridad.UsuarioAutenticado;
import cl.nexotech.api.servicio.OrdenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ordenes")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Órdenes", description = "Compras, boletas y reintento de pago. Comprar exige haber iniciado sesión.")
@ApiResponse(responseCode = "401", description = "Sin sesión iniciada",
        content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
public class OrdenController {

    private final OrdenService servicio;

    public OrdenController(OrdenService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear una orden (comprar)",
            description = "Calcula precios, oferta y cupón en el servidor. Con pago aprobado descuenta el stock y la orden queda "
                    + "pagada; con pago rechazado queda fallida y se puede reintentar.")
    @ApiResponse(responseCode = "400", description = "Datos inválidos",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "409", description = "Stock insuficiente o producto inexistente",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public OrdenResponse crear(@Valid @RequestBody OrdenRequest datos, @AuthenticationPrincipal UsuarioAutenticado actual) {
        return servicio.crear(datos, actual);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    @Operation(summary = "Listar todas las órdenes (admin y vendedor)",
            description = "Con el parámetro usuario se obtiene solo el historial de compras de ese usuario.")
    @ApiResponse(responseCode = "403", description = "Solo admin y vendedor",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public List<OrdenResumenResponse> listar(
            @Parameter(description = "Id del usuario cuyo historial se quiere ver")
            @RequestParam(required = false) Long usuario) {
        return servicio.listar(usuario);
    }

    @GetMapping("/mias")
    @Operation(summary = "Listar mis compras", description = "Historial de órdenes del usuario que inició sesión.")
    public List<OrdenResumenResponse> mias(@AuthenticationPrincipal UsuarioAutenticado actual) {
        return servicio.listarPropias(actual);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Ver una orden (boleta)",
            description = "La ve su dueño o el personal. Para cualquier otro usuario responde 404.")
    @ApiResponse(responseCode = "404", description = "La orden no existe o no es tuya",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public OrdenResponse obtener(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado actual) {
        return servicio.obtener(id, actual);
    }

    @PutMapping("/{id}/pago")
    @Operation(summary = "Reintentar el pago de una orden fallida",
            description = "Si el pago se aprueba, descuenta el stock y la orden pasa a pagada.")
    @ApiResponse(responseCode = "404", description = "La orden no existe o no es tuya",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "409", description = "La orden no está fallida o ya no hay stock",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public OrdenResponse reintentarPago(@PathVariable Long id, @Valid @RequestBody(required = false) PagoRequest datos,
                                        @AuthenticationPrincipal UsuarioAutenticado actual) {
        return servicio.reintentarPago(id, datos == null ? null : datos.pago(), actual);
    }
}
