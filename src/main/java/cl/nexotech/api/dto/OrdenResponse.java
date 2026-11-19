package cl.nexotech.api.dto;

import cl.nexotech.api.modelo.EstadoOrden;
import cl.nexotech.api.modelo.Orden;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Orden completa con sus líneas, lista para mostrar la boleta")
public record OrdenResponse(
        Long id,
        Long usuarioId,
        @Schema(example = "ORD-000012") String codigo,
        EstadoOrden estado,
        Instant creadoEn,
        Cliente cliente,
        Entrega entrega,
        List<Item> items,
        Integer descuento,
        Integer total) {

    public record Cliente(String nombre, String apellidos, String correo) {
    }

    public record Entrega(String calle, String departamento, String region, String comuna, String indicaciones) {
    }

    public record Item(Long productoId, String nombre, Integer precio, Integer cantidad, Integer subtotal) {
    }

    public static OrdenResponse de(Orden o) {
        List<Item> items = o.getItems().stream()
                .map(i -> new Item(i.getProducto() == null ? null : i.getProducto().getId(),
                        i.getNombre(), i.getPrecio(), i.getCantidad(), i.subtotal()))
                .toList();
        return new OrdenResponse(
                o.getId(),
                o.getUsuario() == null ? null : o.getUsuario().getId(),
                o.codigo(), o.getEstado(), o.getCreadoEn(),
                new Cliente(o.getNombre(), o.getApellidos(), o.getCorreo()),
                new Entrega(o.getCalle(), o.getDepartamento(), o.getRegion(), o.getComuna(), o.getIndicaciones()),
                items, o.getDescuento(), o.getTotal());
    }
}
