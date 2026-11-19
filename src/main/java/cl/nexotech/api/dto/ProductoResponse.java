package cl.nexotech.api.dto;

import cl.nexotech.api.modelo.Producto;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Producto del catálogo")
public record ProductoResponse(
        Long id,
        String codigo,
        String nombre,
        String descripcion,
        Integer precio,
        Integer stock,
        Integer stockCritico,
        String imagen,
        Integer descuento,
        Long categoriaId,
        @Schema(description = "Slug de la categoría") String categoria) {

    public static ProductoResponse de(Producto p) {
        return new ProductoResponse(
                p.getId(), p.getCodigo(), p.getNombre(), p.getDescripcion(), p.getPrecio(), p.getStock(),
                p.getStockCritico(), p.getImagen(), p.getDescuentoPct(),
                p.getCategoria().getId(), p.getCategoria().getSlug());
    }
}
