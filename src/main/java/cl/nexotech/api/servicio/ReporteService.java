package cl.nexotech.api.servicio;

import cl.nexotech.api.dto.ReporteResponse;
import cl.nexotech.api.dto.VentaDia;
import cl.nexotech.api.modelo.EstadoOrden;
import cl.nexotech.api.repositorio.CategoriaRepository;
import cl.nexotech.api.repositorio.OrdenItemRepository;
import cl.nexotech.api.repositorio.OrdenRepository;
import cl.nexotech.api.repositorio.ProductoRepository;
import cl.nexotech.api.repositorio.UsuarioRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class ReporteService {

    private final OrdenRepository ordenes;
    private final OrdenItemRepository items;
    private final ProductoRepository productos;
    private final CategoriaRepository categorias;
    private final UsuarioRepository usuarios;

    public ReporteService(OrdenRepository ordenes, OrdenItemRepository items, ProductoRepository productos,
                          CategoriaRepository categorias, UsuarioRepository usuarios) {
        this.ordenes = ordenes;
        this.items = items;
        this.productos = productos;
        this.categorias = categorias;
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public ReporteResponse generar() {
        var resumen = new ReporteResponse.Resumen(
                ordenes.countByEstado(EstadoOrden.PAGADA),
                ordenes.countByEstado(EstadoOrden.FALLIDA),
                ordenes.sumaTotalPorEstado(EstadoOrden.PAGADA),
                productos.count(),
                productos.totalUnidadesEnInventario(),
                productos.contarEnNivelCritico(),
                usuarios.count(),
                usuarios.countByCreadoEnAfter(Instant.now().minus(30, ChronoUnit.DAYS)));

        return new ReporteResponse(
                resumen,
                items.masVendidos(EstadoOrden.PAGADA, PageRequest.of(0, 5)),
                categorias.stockPorCategoria(),
                ordenes.resumenPorEstado(),
                ordenes.ventasPorDia().stream()
                        .map(v -> new VentaDia(v.getDia(), v.getOrdenes(), v.getTotal()))
                        .toList());
    }
}
