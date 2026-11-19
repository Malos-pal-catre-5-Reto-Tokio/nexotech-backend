package cl.nexotech.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para crear una cuenta de cliente. El rol nunca se envía: siempre es cliente.")
public record RegistroRequest(
        @Schema(example = "19011022K", description = "Sin puntos ni guion")
        @Pattern(regexp = "^[0-9]{6,8}[0-9K]$", message = "El RUN no es válido (sin puntos ni guion, ej: 19011022K).")
        String run,

        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 50, message = "El nombre no puede superar 50 caracteres.")
        String nombre,

        @NotBlank(message = "Los apellidos son obligatorios.")
        @Size(max = 100, message = "Los apellidos no pueden superar 100 caracteres.")
        String apellidos,

        @Schema(example = "nombre@gmail.com", description = "Debe ser @duoc.cl, @profesor.duoc.cl o @gmail.com")
        @Size(max = 100, message = "El correo no puede superar 100 caracteres.")
        @Pattern(regexp = "^[^\\s@]+@(duoc\\.cl|profesor\\.duoc\\.cl|gmail\\.com)$",
                message = "El correo debe ser @duoc.cl, @profesor.duoc.cl o @gmail.com.")
        String correo,

        @Schema(description = "Entre 4 y 10 caracteres")
        @Size(min = 4, max = 10, message = "La contraseña debe tener entre 4 y 10 caracteres.")
        String password,

        @NotBlank(message = "La región es obligatoria.")
        @Size(max = 80, message = "La región no puede superar 80 caracteres.")
        String region,

        @NotBlank(message = "La comuna es obligatoria.")
        @Size(max = 60, message = "La comuna no puede superar 60 caracteres.")
        String comuna,

        @NotBlank(message = "La dirección es obligatoria.")
        @Size(max = 150, message = "La dirección no puede superar 150 caracteres.")
        String direccion) {

    public RegistroRequest {
        if (run != null) run = run.trim().toUpperCase();
        if (nombre != null) nombre = nombre.trim();
        if (apellidos != null) apellidos = apellidos.trim();
        if (correo != null) correo = correo.trim().toLowerCase();
        if (region != null) region = region.trim();
        if (comuna != null) comuna = comuna.trim();
        if (direccion != null) direccion = direccion.trim();
    }
}
