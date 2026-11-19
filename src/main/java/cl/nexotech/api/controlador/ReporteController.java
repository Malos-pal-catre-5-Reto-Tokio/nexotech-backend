package cl.nexotech.api.controlador;

import cl.nexotech.api.dto.ReporteResponse;
import cl.nexotech.api.excepcion.ErrorRespuesta;
import cl.nexotech.api.servicio.ReporteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reportes")
@PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Reportes", description = "Indicadores del negocio (admin y vendedor)")
public class ReporteController {

    private final ReporteService servicio;

    public ReporteController(ReporteService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Obtener los reportes",
            description = "Resumen general, productos más vendidos, stock por categoría, órdenes por estado y ventas de los últimos 30 días.")
    @ApiResponse(responseCode = "403", description = "Solo admin y vendedor",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public ReporteResponse obtener() {
        return servicio.generar();
    }
}
