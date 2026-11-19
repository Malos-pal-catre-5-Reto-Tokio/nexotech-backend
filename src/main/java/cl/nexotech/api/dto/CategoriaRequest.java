package cl.nexotech.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para crear o editar una categoría")
public record CategoriaRequest(
        @Schema(example = "monitores", description = "Identificador para URLs: minúsculas, números y guiones")
        @NotBlank(message = "El identificador (slug) es obligatorio.")
        @Pattern(regexp = "^[a-z0-9-]{2,40}$",
                message = "El identificador (slug) debe tener 2-40 letras minúsculas, números o guiones.")
        String slug,

        @Schema(example = "Monitores")
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 60, message = "El nombre no puede superar 60 caracteres.")
        String nombre) {

    public CategoriaRequest {
        if (slug != null) slug = slug.trim().toLowerCase();
        if (nombre != null) nombre = nombre.trim();
    }
}
