package cl.nexotech.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Compra del carrito. Los precios nunca se envían: el servidor los lee de la base de datos.")
public record OrdenRequest(
        @NotNull(message = "Faltan los datos del cliente.") @Valid Cliente cliente,
        @NotNull(message = "Faltan los datos de entrega.") @Valid Entrega entrega,
        @NotEmpty(message = "La compra no tiene productos.") @Valid List<@NotNull(message = "Hay un producto con datos inválidos.") @Valid Item> items,

        @Schema(example = "NEXO10", description = "Cupón de descuento (opcional). NEXO10 descuenta 10%.")
        String cupon,

        @Schema(example = "aprobado", description = "Simula la respuesta de la pasarela de pago: aprobado (por defecto) o rechazado")
        @Pattern(regexp = "aprobado|rechazado", message = "El resultado del pago no es válido.")
        String pago) {

    public OrdenRequest {
        if (cupon != null) cupon = cupon.trim().toUpperCase();
    }

    public record Cliente(
            @NotBlank(message = "El nombre es obligatorio.")
            @Size(max = 50, message = "El nombre no puede superar 50 caracteres.")
            String nombre,

            @NotBlank(message = "Los apellidos son obligatorios.")
            @Size(max = 100, message = "Los apellidos no pueden superar 100 caracteres.")
            String apellidos,

            @NotBlank(message = "El correo es obligatorio.")
            @Email(message = "El correo no es válido.")
            @Size(max = 100, message = "El correo no puede superar 100 caracteres.")
            String correo) {

        public Cliente {
            if (nombre != null) nombre = nombre.trim();
            if (apellidos != null) apellidos = apellidos.trim();
            if (correo != null) correo = correo.trim();
        }
    }

    public record Entrega(
            @NotBlank(message = "La calle es obligatoria.")
            @Size(max = 150, message = "La calle no puede superar 150 caracteres.")
            String calle,

            @Size(max = 30, message = "El departamento no puede superar 30 caracteres.")
            String departamento,

            @NotBlank(message = "La región es obligatoria.")
            @Size(max = 80, message = "La región no puede superar 80 caracteres.")
            String region,

            @NotBlank(message = "La comuna es obligatoria.")
            @Size(max = 60, message = "La comuna no puede superar 60 caracteres.")
            String comuna,

            @Size(max = 300, message = "Las indicaciones no pueden superar 300 caracteres.")
            String indicaciones) {

        public Entrega {
            if (calle != null) calle = calle.trim();
            if (departamento != null) departamento = departamento.trim();
            if (region != null) region = region.trim();
            if (comuna != null) comuna = comuna.trim();
            if (indicaciones != null) indicaciones = indicaciones.trim();
        }
    }

    public record Item(
            @Schema(description = "Id del producto", example = "1")
            @NotNull(message = "Hay un producto con datos inválidos.")
            @Positive(message = "Hay un producto con datos inválidos.")
            Long id,

            @Schema(example = "2")
            @NotNull(message = "Hay un producto con datos inválidos.")
            @Min(value = 1, message = "Hay un producto con datos inválidos.")
            Integer cantidad) {
    }
}
