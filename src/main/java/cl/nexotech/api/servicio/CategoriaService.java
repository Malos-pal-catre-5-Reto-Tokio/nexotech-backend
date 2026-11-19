package cl.nexotech.api.servicio;

import cl.nexotech.api.dto.CategoriaRequest;
import cl.nexotech.api.dto.CategoriaResponse;
import cl.nexotech.api.excepcion.ApiException;
import cl.nexotech.api.modelo.Categoria;
import cl.nexotech.api.repositorio.CategoriaRepository;
import cl.nexotech.api.repositorio.ProductoRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoriaService {

    private final CategoriaRepository categorias;
    private final ProductoRepository productos;

    public CategoriaService(CategoriaRepository categorias, ProductoRepository productos) {
        this.categorias = categorias;
        this.productos = productos;
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar() {
        return categorias.findAll(Sort.by("nombre")).stream().map(CategoriaResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public CategoriaResponse obtener(Long id) {
        return CategoriaResponse.de(buscar(id));
    }

    @Transactional
    public CategoriaResponse crear(CategoriaRequest datos) {
        if (categorias.existsBySlug(datos.slug())) {
            throw ApiException.conflicto("Ya existe una categoría con ese identificador (slug).");
        }
        Categoria categoria = new Categoria();
        categoria.setSlug(datos.slug());
        categoria.setNombre(datos.nombre());
        return CategoriaResponse.de(categorias.save(categoria));
    }

    @Transactional
    public CategoriaResponse actualizar(Long id, CategoriaRequest datos) {
        Categoria categoria = buscar(id);
        if (categorias.existsBySlugAndIdNot(datos.slug(), id)) {
            throw ApiException.conflicto("Ya existe una categoría con ese identificador (slug).");
        }
        categoria.setSlug(datos.slug());
        categoria.setNombre(datos.nombre());
        return CategoriaResponse.de(categoria);
    }

    @Transactional
    public void eliminar(Long id) {
        Categoria categoria = buscar(id);
        if (productos.existsByCategoriaId(id)) {
            throw ApiException.conflicto("No se puede eliminar: la categoría tiene productos asociados.");
        }
        categorias.delete(categoria);
    }

    private Categoria buscar(Long id) {
        return categorias.findById(id).orElseThrow(() -> ApiException.noEncontrado("Categoría no encontrada."));
    }
}
