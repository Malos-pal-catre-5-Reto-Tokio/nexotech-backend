package cl.nexotech.api.dto;

import cl.nexotech.api.modelo.Categoria;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Categoría del catálogo")
public record CategoriaResponse(Long id, String slug, String nombre) {

    public static CategoriaResponse de(Categoria c) {
        return new CategoriaResponse(c.getId(), c.getSlug(), c.getNombre());
    }
}
