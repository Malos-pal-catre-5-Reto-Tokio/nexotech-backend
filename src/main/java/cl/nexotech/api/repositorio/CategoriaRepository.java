package cl.nexotech.api.repositorio;

import cl.nexotech.api.modelo.Categoria;
import cl.nexotech.api.dto.StockCategoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    @Query("""
            select new cl.nexotech.api.dto.StockCategoria(c.nombre, count(p.id), coalesce(sum(p.stock), 0L))
            from Categoria c left join Producto p on p.categoria.id = c.id
            group by c.id, c.nombre
            order by c.nombre
            """)
    List<StockCategoria> stockPorCategoria();

    Optional<Categoria> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);
}
