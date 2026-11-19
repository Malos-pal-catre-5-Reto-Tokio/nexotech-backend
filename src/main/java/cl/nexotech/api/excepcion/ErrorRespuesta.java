package cl.nexotech.api.excepcion;

import io.swagger.v3.oas.annotations.media.Schema;

/** Cuerpo de todas las respuestas de error: {"error": "mensaje para mostrar al usuario"}. */
@Schema(description = "Respuesta de error")
public record ErrorRespuesta(
        @Schema(example = "Producto no encontrado.") String error) {
}
