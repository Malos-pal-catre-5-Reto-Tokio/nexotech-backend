package cl.nexotech.api.servicio;

import cl.nexotech.api.dto.ProductoRequest;
import cl.nexotech.api.dto.ProductoResponse;
import cl.nexotech.api.excepcion.ApiException;
import cl.nexotech.api.modelo.Categoria;
import cl.nexotech.api.modelo.Producto;
import cl.nexotech.api.repositorio.CategoriaRepository;
import cl.nexotech.api.repositorio.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductoService {

    private final ProductoRepository productos;
    private final CategoriaRepository categorias;

    public ProductoService(ProductoRepository productos, CategoriaRepository categorias) {
        this.productos = productos;
        this.categorias = categorias;
    }

    /** Lista el catálogo; todos los filtros son opcionales. */
    @Transactional(readOnly = true)
    public List<ProductoResponse> listar(String categoria, boolean oferta, boolean critico, String texto) {
        String slug = categoria == null ? "" : categoria.trim();
        String patron = "%" + (texto == null ? "" : escaparLike(texto.trim().toLowerCase())) + "%";
        return productos.buscar(slug, oferta, critico, patron).stream().map(ProductoResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public ProductoResponse obtener(Long id) {
        return ProductoResponse.de(buscar(id));
    }

    @Transactional
    public ProductoResponse crear(ProductoRequest datos) {
        if (productos.existsByCodigo(datos.codigo())) {
            throw ApiException.conflicto("Ya existe un producto con ese código.");
        }
        Producto producto = new Producto();
        copiar(datos, producto);
        return ProductoResponse.de(productos.save(producto));
    }

    @Transactional
    public ProductoResponse actualizar(Long id, ProductoRequest datos) {
        Producto producto = buscar(id);
        if (productos.existsByCodigoAndIdNot(datos.codigo(), id)) {
            throw ApiException.conflicto("Ya existe un producto con ese código.");
        }
        copiar(datos, producto);
        return ProductoResponse.de(producto);
    }

    @Transactional
    public void eliminar(Long id) {
        productos.delete(buscar(id));
    }

    private Producto buscar(Long id) {
        return productos.findConCategoria(id).orElseThrow(() -> ApiException.noEncontrado("Producto no encontrado."));
    }

    private void copiar(ProductoRequest datos, Producto producto) {
        Categoria categoria = categorias.findById(datos.categoriaId())
                .orElseThrow(() -> ApiException.solicitudInvalida("La categoría indicada no existe."));
        producto.setCodigo(datos.codigo());
        producto.setNombre(datos.nombre());
        producto.setDescripcion(datos.descripcion());
        producto.setPrecio(datos.precio());
        producto.setStock(datos.stock());
        producto.setStockCritico(datos.stockCritico() == null ? 0 : datos.stockCritico());
        producto.setImagen(datos.imagen());
        producto.setDescuentoPct(datos.descuento() == null ? 0 : datos.descuento());
        producto.setCategoria(categoria);
    }

    /** Evita que % y _ escritos por el usuario actúen como comodines en el LIKE. */
    private String escaparLike(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
