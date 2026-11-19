package cl.nexotech.api.repositorio;

import cl.nexotech.api.dto.OrdenesEstado;
import cl.nexotech.api.modelo.EstadoOrden;
import cl.nexotech.api.modelo.Orden;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrdenRepository extends JpaRepository<Orden, Long> {

    /** Orden con sus líneas cargadas, lista para armar la boleta. */
    @EntityGraph(attributePaths = {"items", "usuario"})
    Optional<Orden> findConItemsById(Long id);

    /** Bloquea la orden hasta terminar la transacción: evita pagar dos veces el mismo reintento. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Orden o where o.id = :id")
    Optional<Orden> findParaActualizar(@Param("id") Long id);

    @EntityGraph(attributePaths = "usuario")
    List<Orden> findAllByOrderByIdDesc();

    @EntityGraph(attributePaths = "usuario")
    List<Orden> findByUsuarioIdOrderByIdDesc(Long usuarioId);

    long countByEstado(EstadoOrden estado);

    @Query("select coalesce(sum(o.total), 0L) from Orden o where o.estado = :estado")
    long sumaTotalPorEstado(@Param("estado") EstadoOrden estado);

    @Query("""
            select new cl.nexotech.api.dto.OrdenesEstado(o.estado, count(o), coalesce(sum(o.total), 0L))
            from Orden o group by o.estado order by o.estado
            """)
    List<OrdenesEstado> resumenPorEstado();

    /** Ventas pagadas de los últimos 30 días agrupadas por día (hora de Chile). */
    @Query(value = """
            SELECT to_char(creado_en AT TIME ZONE 'America/Santiago', 'YYYY-MM-DD') AS dia,
                   COUNT(*) AS ordenes,
                   SUM(total) AS total
            FROM ordenes
            WHERE estado = 'pagada' AND creado_en >= now() - interval '30 days'
            GROUP BY 1
            ORDER BY 1
            """, nativeQuery = true)
    List<VentaDiaProyeccion> ventasPorDia();
}
