package cl.nexotech.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para crear o editar un producto")
public record ProductoRequest(
        @Schema(example = "MN-001")
        @NotBlank(message = "El código es obligatorio.")
        @Size(max = 20, message = "El código no puede superar 20 caracteres.")
        String codigo,

        @Schema(example = "Monitor 24\" Full HD")
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres.")
        String nombre,

        @Size(max = 1000, message = "La descripción no puede superar 1000 caracteres.")
        String descripcion,

        @Schema(example = "129990", description = "Precio normal en pesos chilenos")
        @NotNull(message = "El precio es obligatorio.")
        @Min(value = 0, message = "El precio debe ser un número mayor o igual a 0.")
        Integer precio,

        @Schema(example = "10")
        @NotNull(message = "El stock es obligatorio.")
        @Min(value = 0, message = "El stock debe ser un número mayor o igual a 0.")
        Integer stock,

        @Schema(example = "3", description = "Con este stock o menos el producto está en nivel crítico (opcional, por defecto 0)")
        @Min(value = 0, message = "El stock crítico debe ser un número mayor o igual a 0.")
        Integer stockCritico,

        @Schema(example = "/img/productos/monitor.jpg")
        @Size(max = 255, message = "La imagen no puede superar 255 caracteres.")
        String imagen,

        @Schema(example = "0", description = "Porcentaje de descuento de la oferta, 0 a 90 (opcional, por defecto 0)")
        @Min(value = 0, message = "El descuento debe estar entre 0 y 90.")
        @Max(value = 90, message = "El descuento debe estar entre 0 y 90.")
        Integer descuento,

        @Schema(example = "1")
        @NotNull(message = "La categoría es obligatoria.")
        Long categoriaId) {

    public ProductoRequest {
        if (codigo != null) codigo = codigo.trim();
        if (nombre != null) nombre = nombre.trim();
        if (descripcion != null) descripcion = descripcion.trim();
        if (imagen != null) imagen = imagen.trim();
    }
}
