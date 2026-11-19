package cl.nexotech.api.dto;

import cl.nexotech.api.modelo.EstadoOrden;
import cl.nexotech.api.modelo.Orden;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Fila de un listado de órdenes")
public record OrdenResumenResponse(
        Long id,
        Long usuarioId,
        String codigo,
        String nombre,
        String apellidos,
        String correo,
        Integer total,
        EstadoOrden estado,
        Instant creadoEn) {

    public static OrdenResumenResponse de(Orden o) {
        return new OrdenResumenResponse(
                o.getId(), o.getUsuario() == null ? null : o.getUsuario().getId(), o.codigo(),
                o.getNombre(), o.getApellidos(), o.getCorreo(), o.getTotal(), o.getEstado(), o.getCreadoEn());
    }
}
