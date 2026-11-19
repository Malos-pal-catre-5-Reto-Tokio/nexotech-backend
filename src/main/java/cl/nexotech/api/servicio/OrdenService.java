package cl.nexotech.api.servicio;

import cl.nexotech.api.dto.OrdenRequest;
import cl.nexotech.api.dto.OrdenResponse;
import cl.nexotech.api.dto.OrdenResumenResponse;
import cl.nexotech.api.excepcion.ApiException;
import cl.nexotech.api.modelo.EstadoOrden;
import cl.nexotech.api.modelo.Orden;
import cl.nexotech.api.modelo.OrdenItem;
import cl.nexotech.api.modelo.Producto;
import cl.nexotech.api.modelo.Rol;
import cl.nexotech.api.modelo.Usuario;
import cl.nexotech.api.repositorio.OrdenRepository;
import cl.nexotech.api.repositorio.ProductoRepository;
import cl.nexotech.api.repositorio.UsuarioRepository;
import cl.nexotech.api.seguridad.UsuarioAutenticado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class OrdenService {

    /** Cupones vigentes y su porcentaje de descuento. */
    private static final Map<String, Integer> CUPONES = Map.of("NEXO10", 10);

    private static final String PAGO_RECHAZADO = "rechazado";

    private final OrdenRepository ordenes;
    private final ProductoRepository productos;
    private final UsuarioRepository usuarios;

    public OrdenService(OrdenRepository ordenes, ProductoRepository productos, UsuarioRepository usuarios) {
        this.ordenes = ordenes;
        this.productos = productos;
        this.usuarios = usuarios;
    }

    /**
     * Crea la orden del usuario que inició sesión. Los precios y el stock se leen de la base de datos
     * (nunca del navegador) y las filas de los productos quedan bloqueadas hasta terminar la transacción.
     * El stock solo se descuenta si el pago es aprobado.
     */
    @Transactional
    public OrdenResponse crear(OrdenRequest datos, UsuarioAutenticado actual) {
        Usuario dueno = usuarios.findById(actual.id())
                .orElseThrow(() -> ApiException.noAutorizado("Debes iniciar sesión."));

        // Se juntan las líneas repetidas y se recorren por id: así siempre se bloquea en el mismo orden (sin deadlocks).
        Map<Long, Integer> pedido = new TreeMap<>();
        for (OrdenRequest.Item item : datos.items()) {
            pedido.merge(item.id(), item.cantidad(), Integer::sum);
        }

        Orden orden = new Orden();
        orden.setUsuario(dueno);
        orden.setNombre(datos.cliente().nombre());
        orden.setApellidos(datos.cliente().apellidos());
        orden.setCorreo(datos.cliente().correo());
        orden.setCalle(datos.entrega().calle());
        orden.setDepartamento(vacioANulo(datos.entrega().departamento()));
        orden.setRegion(datos.entrega().region());
        orden.setComuna(datos.entrega().comuna());
        orden.setIndicaciones(vacioANulo(datos.entrega().indicaciones()));

        long subtotal = 0;
        for (Map.Entry<Long, Integer> linea : pedido.entrySet()) {
            Producto producto = productos.findParaActualizarStock(linea.getKey())
                    .orElseThrow(() -> ApiException.conflicto("Un producto de la compra ya no existe."));
            if (producto.getStock() < linea.getValue()) {
                throw ApiException.conflicto("Stock insuficiente para " + producto.getNombre() + ".");
            }
            OrdenItem item = new OrdenItem();
            item.setProducto(producto);
            item.setNombre(producto.getNombre());
            item.setPrecio(producto.precioFinal());
            item.setCantidad(linea.getValue());
            orden.agregarItem(item);
            subtotal += (long) item.getPrecio() * item.getCantidad();
        }

        long descuento = Math.round(subtotal * CUPONES.getOrDefault(datos.cupon(), 0) / 100.0);
        long total = subtotal - descuento;
        if (total > Integer.MAX_VALUE) {
            throw ApiException.solicitudInvalida("El monto de la compra excede el máximo permitido.");
        }
        orden.setDescuento((int) descuento);
        orden.setTotal((int) total);

        boolean aprobado = !PAGO_RECHAZADO.equals(datos.pago());
        orden.setEstado(aprobado ? EstadoOrden.PAGADA : EstadoOrden.FALLIDA);
        if (aprobado) {
            descontarStock(orden);
        }
        return OrdenResponse.de(ordenes.save(orden));
    }

    /** Reintenta el pago de una orden fallida. Si ahora se aprueba, descuenta el stock y la deja pagada. */
    @Transactional
    public OrdenResponse reintentarPago(Long id, String pago, UsuarioAutenticado actual) {
        Orden orden = ordenes.findParaActualizar(id).orElseThrow(OrdenService::ordenNoEncontrada);
        verificarAcceso(orden, actual);
        if (orden.getEstado() != EstadoOrden.FALLIDA) {
            throw ApiException.conflicto("Esta orden no tiene un pago pendiente.");
        }
        for (OrdenItem item : orden.getItems()) {
            if (item.getProducto() == null) {
                throw ApiException.conflicto("Un producto de la compra ya no existe.");
            }
        }
        if (!PAGO_RECHAZADO.equals(pago)) {
            descontarStock(orden);
            orden.setEstado(EstadoOrden.PAGADA);
        }
        return OrdenResponse.de(orden);
    }

    @Transactional(readOnly = true)
    public OrdenResponse obtener(Long id, UsuarioAutenticado actual) {
        Orden orden = ordenes.findConItemsById(id).orElseThrow(OrdenService::ordenNoEncontrada);
        verificarAcceso(orden, actual);
        return OrdenResponse.de(orden);
    }

    /** Listado completo para el personal; con usuarioId filtra el historial de ese usuario. */
    @Transactional(readOnly = true)
    public List<OrdenResumenResponse> listar(Long usuarioId) {
        List<Orden> lista = usuarioId == null
                ? ordenes.findAllByOrderByIdDesc()
                : ordenes.findByUsuarioIdOrderByIdDesc(usuarioId);
        return lista.stream().map(OrdenResumenResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public List<OrdenResumenResponse> listarPropias(UsuarioAutenticado actual) {
        return ordenes.findByUsuarioIdOrderByIdDesc(actual.id()).stream().map(OrdenResumenResponse::de).toList();
    }

    /** Descuenta el stock de cada línea (en orden de id, con la fila bloqueada) o falla si no alcanza. */
    private void descontarStock(Orden orden) {
        List<OrdenItem> lineas = orden.getItems().stream()
                .sorted((a, b) -> a.getProducto().getId().compareTo(b.getProducto().getId()))
                .toList();
        for (OrdenItem item : lineas) {
            Producto producto = productos.findParaActualizarStock(item.getProducto().getId())
                    .orElseThrow(() -> ApiException.conflicto("Un producto de la compra ya no existe."));
            if (producto.getStock() < item.getCantidad()) {
                throw ApiException.conflicto("Stock insuficiente para " + producto.getNombre() + ".");
            }
            producto.setStock(producto.getStock() - item.getCantidad());
        }
    }

    /** Una orden la ve su dueño o el personal; para cualquier otro usuario "no existe". */
    private void verificarAcceso(Orden orden, UsuarioAutenticado actual) {
        boolean esPersonal = actual.rol() == Rol.ADMIN || actual.rol() == Rol.VENDEDOR;
        boolean esDueno = orden.getUsuario() != null && orden.getUsuario().getId().equals(actual.id());
        if (!esPersonal && !esDueno) {
            throw ordenNoEncontrada();
        }
    }

    private static ApiException ordenNoEncontrada() {
        return ApiException.noEncontrado("Orden no encontrada.");
    }

    private static String vacioANulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto;
    }
}
