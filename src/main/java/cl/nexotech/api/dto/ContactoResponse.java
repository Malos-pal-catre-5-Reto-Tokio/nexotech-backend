package cl.nexotech.api.dto;

import cl.nexotech.api.modelo.MensajeContacto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Mensaje de contacto recibido")
public record ContactoResponse(Long id, String nombre, String correo, String comentario, Instant creadoEn) {

    public static ContactoResponse de(MensajeContacto m) {
        return new ContactoResponse(m.getId(), m.getNombre(), m.getCorreo(), m.getComentario(), m.getCreadoEn());
    }
}
