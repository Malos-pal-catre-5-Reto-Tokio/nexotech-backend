package cl.nexotech.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Indicadores del negocio para el panel de administración")
public record ReporteResponse(
        Resumen resumen,
        List<ProductoVendido> masVendidos,
        List<StockCategoria> stockPorCategoria,
        List<OrdenesEstado> ordenesPorEstado,
        @Schema(description = "Ventas pagadas de los últimos 30 días, por día") List<VentaDia> ventasPorDia) {

    public record Resumen(
            long ordenesPagadas,
            long ordenesFallidas,
            long ingresos,
            long productos,
            long unidadesInventario,
            long productosCriticos,
            long usuarios,
            long usuariosNuevosMes) {
    }
}
