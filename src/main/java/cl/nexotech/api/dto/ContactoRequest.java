package cl.nexotech.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Mensaje del formulario de contacto")
public record ContactoRequest(
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres.")
        String nombre,

        @Size(max = 100, message = "El correo no puede superar 100 caracteres.")
        @Pattern(regexp = "^[^\\s@]+@(duoc\\.cl|profesor\\.duoc\\.cl|gmail\\.com)$",
                message = "El correo debe ser @duoc.cl, @profesor.duoc.cl o @gmail.com.")
        String correo,

        @NotBlank(message = "El comentario es obligatorio.")
        @Size(max = 500, message = "El comentario no puede superar 500 caracteres.")
        String comentario) {

    public ContactoRequest {
        if (nombre != null) nombre = nombre.trim();
        if (correo != null) correo = correo.trim().toLowerCase();
        if (comentario != null) comentario = comentario.trim();
    }
}
