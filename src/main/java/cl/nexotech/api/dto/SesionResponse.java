package cl.nexotech.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Sesión iniciada: token de acceso y datos del usuario. El token de renovación va aparte, en una cookie HttpOnly.")
public record SesionResponse(
        @Schema(description = "JWT para enviar en la cabecera Authorization: Bearer <token>") String accessToken,
        @Schema(description = "Segundos que dura el token de acceso", example = "900") long expiraEnSegundos,
        UsuarioResponse usuario) {
}
