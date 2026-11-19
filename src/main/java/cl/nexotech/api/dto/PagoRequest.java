package cl.nexotech.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Resultado simulado de la pasarela de pago")
public record PagoRequest(
        @Schema(example = "aprobado", description = "aprobado (por defecto) o rechazado")
        @Pattern(regexp = "aprobado|rechazado", message = "El resultado del pago no es válido.")
        String pago) {
}
