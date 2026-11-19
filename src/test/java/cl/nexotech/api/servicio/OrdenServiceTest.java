package cl.nexotech.api.servicio;

import cl.nexotech.api.dto.OrdenRequest;
import cl.nexotech.api.dto.OrdenResponse;
import cl.nexotech.api.excepcion.ApiException;
import cl.nexotech.api.modelo.Categoria;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** Pruebas unitarias de las reglas de negocio de las órdenes (stock, precios, cupón, permisos). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrdenServiceTest {

    @Mock
    OrdenRepository ordenes;
    @Mock
    ProductoRepository productos;
    @Mock
    UsuarioRepository usuarios;

    private OrdenService servicio;
    private Usuario pedro;
    private Producto mouse;   // $10.000, sin oferta, stock 5
    private Producto teclado; // $40.000 con 20% de oferta (precio final $32.000), stock 3
    private final UsuarioAutenticado actual = new UsuarioAutenticado(3L, "cliente@gmail.com", Rol.CLIENTE);

    @BeforeEach
    void preparar() {
        servicio = new OrdenService(ordenes, productos, usuarios);

        pedro = new Usuario();
        pedro.setId(3L);
        pedro.setRol(Rol.CLIENTE);
        when(usuarios.findById(3L)).thenReturn(Optional.of(pedro));

        Categoria categoria = new Categoria();
        categoria.setId(1L);
        mouse = producto(1L, "Mouse", 10000, 0, 5, categoria);
        teclado = producto(2L, "Teclado", 40000, 20, 3, categoria);
        when(productos.findParaActualizarStock(1L)).thenReturn(Optional.of(mouse));
        when(productos.findParaActualizarStock(2L)).thenReturn(Optional.of(teclado));
        when(ordenes.save(any(Orden.class))).thenAnswer(i -> i.getArgument(0));
    }

    private static Producto producto(Long id, String nombre, int precio, int descuento, int stock, Categoria categoria) {
        Producto p = new Producto();
        p.setId(id);
        p.setNombre(nombre);
        p.setPrecio(precio);
        p.setDescuentoPct(descuento);
        p.setStock(stock);
        p.setStockCritico(1);
        p.setCategoria(categoria);
        return p;
    }

    private static OrdenRequest pedido(String cupon, String pago, OrdenRequest.Item... items) {
        return new OrdenRequest(
                new OrdenRequest.Cliente("Pedro", "Cliente", "cliente@gmail.com"),
                new OrdenRequest.Entrega("Los Crisantemos 1", "", "Región de Tarapacá", "Iquique", ""),
                List.of(items), cupon, pago);
    }

    private static OrdenRequest.Item item(long id, int cantidad) {
        return new OrdenRequest.Item(id, cantidad);
    }

    @Test
    void conPagoAprobadoCalculaElTotalEnElServidorYDescuentaElStock() {
        OrdenResponse orden = servicio.crear(pedido(null, "aprobado", item(1, 2), item(2, 1)), actual);

        assertThat(orden.estado()).isEqualTo(EstadoOrden.PAGADA);
        assertThat(orden.total()).isEqualTo(2 * 10000 + 32000); // el precio con oferta sale de la base de datos
        assertThat(orden.descuento()).isZero();
        assertThat(mouse.getStock()).isEqualTo(3);
        assertThat(teclado.getStock()).isEqualTo(2);
    }

    @Test
    void elCuponNexo10DescuentaElDiezPorCiento() {
        OrdenResponse orden = servicio.crear(pedido("nexo10", "aprobado", item(2, 2)), actual);

        assertThat(orden.descuento()).isEqualTo(6400);
        assertThat(orden.total()).isEqualTo(57600);
    }

    @Test
    void unCuponDesconocidoSeIgnora() {
        OrdenResponse orden = servicio.crear(pedido("INVENTADO", "aprobado", item(1, 1)), actual);

        assertThat(orden.descuento()).isZero();
        assertThat(orden.total()).isEqualTo(10000);
    }

    @Test
    void conPagoRechazadoLaOrdenQuedaFallidaYElStockNoCambia() {
        OrdenResponse orden = servicio.crear(pedido(null, "rechazado", item(1, 2)), actual);

        assertThat(orden.estado()).isEqualTo(EstadoOrden.FALLIDA);
        assertThat(mouse.getStock()).isEqualTo(5);
    }

    @Test
    void lasLineasRepetidasDeUnMismoProductoSeSuman() {
        servicio.crear(pedido(null, "aprobado", item(1, 1), item(1, 2)), actual);

        assertThat(mouse.getStock()).isEqualTo(2);
    }

    @Test
    void sinStockSuficienteResponde409() {
        assertThatThrownBy(() -> servicio.crear(pedido(null, "aprobado", item(2, 4)), actual))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getEstado()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getMessage()).isEqualTo("Stock insuficiente para Teclado.");
                });
        assertThat(teclado.getStock()).isEqualTo(3);
    }

    @Test
    void unProductoInexistenteResponde409() {
        when(productos.findParaActualizarStock(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.crear(pedido(null, "aprobado", item(99, 1)), actual))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getEstado()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void sinUsuarioValidoNoSePuedeComprar() {
        UsuarioAutenticado fantasma = new UsuarioAutenticado(77L, "x@gmail.com", Rol.CLIENTE);
        when(usuarios.findById(77L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.crear(pedido(null, "aprobado", item(1, 1)), fantasma))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getEstado()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    // ---- reintento de pago y acceso a las órdenes ----

    private Orden ordenFallida(Usuario dueno) {
        Orden orden = new Orden();
        orden.setId(8L);
        orden.setUsuario(dueno);
        orden.setEstado(EstadoOrden.FALLIDA);
        orden.setTotal(20000);
        OrdenItem linea = new OrdenItem();
        linea.setProducto(mouse);
        linea.setNombre("Mouse");
        linea.setPrecio(10000);
        linea.setCantidad(2);
        orden.agregarItem(linea);
        when(ordenes.findParaActualizar(8L)).thenReturn(Optional.of(orden));
        when(ordenes.findConItemsById(8L)).thenReturn(Optional.of(orden));
        return orden;
    }

    @Test
    void reintentarUnPagoFallidoLoApruebaYDescuentaElStock() {
        ordenFallida(pedro);

        OrdenResponse orden = servicio.reintentarPago(8L, "aprobado", actual);

        assertThat(orden.estado()).isEqualTo(EstadoOrden.PAGADA);
        assertThat(mouse.getStock()).isEqualTo(3);
    }

    @Test
    void reintentarConPagoRechazadoDejaLaOrdenFallida() {
        ordenFallida(pedro);

        OrdenResponse orden = servicio.reintentarPago(8L, "rechazado", actual);

        assertThat(orden.estado()).isEqualTo(EstadoOrden.FALLIDA);
        assertThat(mouse.getStock()).isEqualTo(5);
    }

    @Test
    void unaOrdenYaPagadaNoSePuedePagarDeNuevo() {
        Orden orden = ordenFallida(pedro);
        orden.setEstado(EstadoOrden.PAGADA);

        assertThatThrownBy(() -> servicio.reintentarPago(8L, "aprobado", actual))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getEstado()).isEqualTo(HttpStatus.CONFLICT));
        assertThat(mouse.getStock()).isEqualTo(5);
    }

    @Test
    void otroClienteNoPuedeVerNiPagarUnaOrdenAjenaYRecibe404() {
        ordenFallida(pedro);
        UsuarioAutenticado intruso = new UsuarioAutenticado(4L, "otro@gmail.com", Rol.CLIENTE);

        assertThatThrownBy(() -> servicio.obtener(8L, intruso))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getEstado()).isEqualTo(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> servicio.reintentarPago(8L, "aprobado", intruso))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getEstado()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void elDuenoYElPersonalSiPuedenVerLaOrden() {
        ordenFallida(pedro);

        assertThat(servicio.obtener(8L, actual).id()).isEqualTo(8L);
        assertThat(servicio.obtener(8L, new UsuarioAutenticado(1L, "admin@duoc.cl", Rol.ADMIN)).id()).isEqualTo(8L);
        assertThat(servicio.obtener(8L, new UsuarioAutenticado(2L, "vendedor@duoc.cl", Rol.VENDEDOR)).id()).isEqualTo(8L);
    }
}
