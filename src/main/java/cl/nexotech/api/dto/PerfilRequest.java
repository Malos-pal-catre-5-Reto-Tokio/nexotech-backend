package cl.nexotech.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos que el usuario puede cambiar de su propio perfil")
public record PerfilRequest(
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 50, message = "El nombre no puede superar 50 caracteres.")
        String nombre,

        @NotBlank(message = "Los apellidos son obligatorios.")
        @Size(max = 100, message = "Los apellidos no pueden superar 100 caracteres.")
        String apellidos,

        @NotBlank(message = "La región es obligatoria.")
        @Size(max = 80, message = "La región no puede superar 80 caracteres.")
        String region,

        @NotBlank(message = "La comuna es obligatoria.")
        @Size(max = 60, message = "La comuna no puede superar 60 caracteres.")
        String comuna,

        @NotBlank(message = "La dirección es obligatoria.")
        @Size(max = 150, message = "La dirección no puede superar 150 caracteres.")
        String direccion,

        @Schema(description = "Solo si se quiere cambiar la contraseña (entre 4 y 10 caracteres)")
        String passwordNueva,

        @Schema(description = "Contraseña actual; obligatoria cuando se envía passwordNueva")
        String passwordActual) {

    public PerfilRequest {
        if (nombre != null) nombre = nombre.trim();
        if (apellidos != null) apellidos = apellidos.trim();
        if (region != null) region = region.trim();
        if (comuna != null) comuna = comuna.trim();
        if (direccion != null) direccion = direccion.trim();
    }
}
