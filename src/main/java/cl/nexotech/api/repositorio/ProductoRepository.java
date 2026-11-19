package cl.nexotech.api.repositorio;

import cl.nexotech.api.modelo.Producto;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    /**
     * Catálogo con filtros opcionales. Para no filtrar se envía slug vacío ("") y patrón "%" en la búsqueda;
     * los booleanos activan solo ofertas o solo stock crítico.
     */
    @Query("""
            select p from Producto p join fetch p.categoria c
            where (:slug = '' or c.slug = :slug)
              and (:oferta = false or p.descuentoPct > 0)
              and (:critico = false or p.stock <= p.stockCritico)
              and (lower(p.nombre) like :patron or lower(p.codigo) like :patron)
            order by p.id
            """)
    List<Producto> buscar(@Param("slug") String slug,
                          @Param("oferta") boolean oferta,
                          @Param("critico") boolean critico,
                          @Param("patron") String patron);

    @Query("select p from Producto p join fetch p.categoria where p.id = :id")
    Optional<Producto> findConCategoria(@Param("id") Long id);

    /** Bloquea la fila hasta terminar la transacción: evita vender dos veces la misma unidad. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Producto p where p.id = :id")
    Optional<Producto> findParaActualizarStock(@Param("id") Long id);

    @Query("select coalesce(sum(p.stock), 0L) from Producto p")
    long totalUnidadesEnInventario();

    @Query("select count(p) from Producto p where p.stock <= p.stockCritico")
    long contarEnNivelCritico();

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, Long id);

    boolean existsByCategoriaId(Long categoriaId);
}
