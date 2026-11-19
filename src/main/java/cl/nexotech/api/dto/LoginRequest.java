package cl.nexotech.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credenciales para iniciar sesión")
public record LoginRequest(
        @Schema(example = "admin@duoc.cl")
        @NotBlank(message = "Ingresa tu correo y contraseña.")
        String correo,

        @Schema(example = "admin123")
        @NotBlank(message = "Ingresa tu correo y contraseña.")
        String password) {

    public LoginRequest {
        if (correo != null) correo = correo.trim().toLowerCase();
    }
}
