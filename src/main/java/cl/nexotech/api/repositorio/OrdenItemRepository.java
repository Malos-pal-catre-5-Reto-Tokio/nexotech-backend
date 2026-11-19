package cl.nexotech.api.repositorio;

import cl.nexotech.api.dto.ProductoVendido;
import cl.nexotech.api.modelo.EstadoOrden;
import cl.nexotech.api.modelo.OrdenItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrdenItemRepository extends JpaRepository<OrdenItem, Long> {

    /** Productos más vendidos entre las órdenes con el estado indicado; el Pageable limita la cantidad de filas. */
    @Query("""
            select new cl.nexotech.api.dto.ProductoVendido(oi.nombre, sum(oi.cantidad), sum(oi.precio * oi.cantidad))
            from OrdenItem oi join oi.orden o
            where o.estado = :estado
            group by oi.nombre
            order by sum(oi.cantidad) desc
            """)
    List<ProductoVendido> masVendidos(@Param("estado") EstadoOrden estado, Pageable limite);
}
