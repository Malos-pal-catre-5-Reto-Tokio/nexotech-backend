package cl.nexotech.api.integracion;

import cl.nexotech.api.modelo.Categoria;
import cl.nexotech.api.modelo.Producto;
import cl.nexotech.api.modelo.Rol;
import cl.nexotech.api.modelo.Usuario;
import cl.nexotech.api.repositorio.CategoriaRepository;
import cl.nexotech.api.repositorio.ProductoRepository;
import cl.nexotech.api.repositorio.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integración: arrancan toda la aplicación (filtro JWT, Spring Security, controladores, servicios y JPA)
 * con una base H2 en memoria y llaman a la API real con MockMvc. Cada prueba se revierte al terminar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiIntegracionTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UsuarioRepository usuarios;
    @Autowired ProductoRepository productos;
    @Autowired CategoriaRepository categorias;
    @Autowired PasswordEncoder encoder;

    Categoria categoria;
    Producto mouse;    // $10.000, stock 5
    Producto teclado;  // $40.000 con 20% de oferta, stock 3

    @BeforeEach
    void datos() {
        categoria = new Categoria();
        categoria.setSlug("accesorios");
        categoria.setNombre("Accesorios");
        categoria = categorias.saveAndFlush(categoria);

        mouse = productos.saveAndFlush(producto("AC-001", "Mouse", 10000, 0, 5));
        teclado = productos.saveAndFlush(producto("AC-002", "Teclado", 40000, 20, 3));

        usuario("11111111K", "Admin", "admin@duoc.cl", "admin123", Rol.ADMIN);
        usuario("22222222K", "Vera", "vendedor@duoc.cl", "vend123", Rol.VENDEDOR);
        usuario("33333333K", "Pedro", "cliente@gmail.com", "cliente123", Rol.CLIENTE);
        usuario("44444444K", "Otra", "otra@gmail.com", "otra123", Rol.CLIENTE);
    }

    // ---------- ayudas ----------

    private Producto producto(String codigo, String nombre, int precio, int descuento, int stock) {
        Producto p = new Producto();
        p.setCodigo(codigo);
        p.setNombre(nombre);
        p.setPrecio(precio);
        p.setDescuentoPct(descuento);
        p.setStock(stock);
        p.setStockCritico(1);
        p.setCategoria(categoria);
        return p;
    }

    private void usuario(String run, String nombre, String correo, String clave, Rol rol) {
        Usuario u = new Usuario();
        u.setRun(run);
        u.setNombre(nombre);
        u.setApellidos("Prueba");
        u.setCorreo(correo);
        u.setPasswordHash(encoder.encode(clave));
        u.setRol(rol);
        usuarios.saveAndFlush(u);
    }

    private MvcResult iniciarSesion(String correo, String clave) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"" + correo + "\",\"password\":\"" + clave + "\"}")).andReturn();
    }

    private String token(String correo, String clave) throws Exception {
        JsonNode cuerpo = json.readTree(iniciarSesion(correo, clave).getResponse().getContentAsString());
        return cuerpo.get("accessToken").asText();
    }

    private String admin() throws Exception { return token("admin@duoc.cl", "admin123"); }
    private String vendedor() throws Exception { return token("vendedor@duoc.cl", "vend123"); }
    private String cliente() throws Exception { return token("cliente@gmail.com", "cliente123"); }

    private static MockHttpServletRequestBuilder con(MockHttpServletRequestBuilder peticion, String token, String cuerpo) {
        peticion.header("Authorization", "Bearer " + token);
        if (cuerpo != null) peticion.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
        return peticion;
    }

    private String productoNuevo(String codigo) {
        return "{\"codigo\":\"" + codigo + "\",\"nombre\":\"Webcam\",\"precio\":19990,\"stock\":4,\"categoriaId\":" + categoria.getId() + "}";
    }

    private String orden(long productoId, int cantidad, String pago) {
        return """
                {"cliente":{"nombre":"Pedro","apellidos":"Prueba","correo":"cliente@gmail.com"},
                 "entrega":{"calle":"Los Crisantemos 1","departamento":"","region":"Región de Tarapacá","comuna":"Iquique","indicaciones":""},
                 "items":[{"id":%d,"cantidad":%d}],"pago":"%s"}
                """.formatted(productoId, cantidad, pago);
    }

    private long crearOrden(String token, long productoId, int cantidad, String pago) throws Exception {
        MvcResult r = mvc.perform(con(post("/api/ordenes"), token, orden(productoId, cantidad, pago)))
                .andExpect(status().isCreated()).andReturn();
        return json.readTree(r.getResponse().getContentAsString()).get("id").asLong();
    }

    private int stockDe(Producto p) throws Exception {
        MvcResult r = mvc.perform(get("/api/productos/" + p.getId())).andExpect(status().isOk()).andReturn();
        return json.readTree(r.getResponse().getContentAsString()).get("stock").asInt();
    }

    // ---------- autenticación (JWT) ----------

    @Test
    void loginCorrectoEntregaTokenUsuarioYCookieDeRenovacionHttpOnly() throws Exception {
        MvcResult r = iniciarSesion("admin@duoc.cl", "admin123");

        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        JsonNode cuerpo = json.readTree(r.getResponse().getContentAsString());
        assertThat(cuerpo.get("accessToken").asText()).isNotBlank();
        assertThat(cuerpo.get("usuario").get("rol").asText()).isEqualTo("admin");
        assertThat(cuerpo.get("usuario").has("passwordHash")).isFalse();
        Cookie cookie = r.getResponse().getCookie("refresh_token");
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/api/auth");
    }

    @Test
    void loginConContrasenaIncorrectaResponde401ConMensaje() throws Exception {
        MvcResult r = iniciarSesion("admin@duoc.cl", "mala");

        assertThat(r.getResponse().getStatus()).isEqualTo(401);
        assertThat(json.readTree(r.getResponse().getContentAsString()).get("error").asText())
                .isEqualTo("Correo o contraseña incorrectos.");
    }

    @Test
    void registroCreaUnClienteAunqueSeIntenteEnviarOtroRol() throws Exception {
        String cuerpo = """
                {"run":"20123456K","nombre":"Nuevo","apellidos":"Usuario","correo":"nuevo@gmail.com","password":"clave123",
                 "region":"Región de Tarapacá","comuna":"Iquique","direccion":"Calle 1","rol":"admin"}
                """;

        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuario.rol").value("cliente"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isConflict());
    }

    @Test
    void elTokenDeAccesoPermiteVerElPropioPerfil() throws Exception {
        mvc.perform(con(get("/api/auth/me"), cliente(), null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("cliente@gmail.com"));
    }

    @Test
    void conLaCookieDeRenovacionSeObtieneUnTokenNuevo() throws Exception {
        Cookie renovacion = iniciarSesion("cliente@gmail.com", "cliente123").getResponse().getCookie("refresh_token");

        mvc.perform(post("/api/auth/refresh").cookie(renovacion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.usuario.correo").value("cliente@gmail.com"));
    }

    @Test
    void sinCookieOconUnTokenDeAccesoComoRenovacionResponde401() throws Exception {
        mvc.perform(post("/api/auth/refresh")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh").cookie(new Cookie("refresh_token", cliente())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void elTokenDeRenovacionNoSirveParaLlamarALaApi() throws Exception {
        String renovacion = iniciarSesion("admin@duoc.cl", "admin123").getResponse().getCookie("refresh_token").getValue();

        mvc.perform(con(get("/api/usuarios"), renovacion, null)).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutBorraLaCookieDeRenovacion() throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/logout")).andExpect(status().isOk()).andReturn();

        assertThat(r.getResponse().getCookie("refresh_token").getMaxAge()).isZero();
    }

    // ---------- 401 y 403 ----------

    @Test
    void sinTokenOconTokenInvalidoLasRutasProtegidasResponden401() throws Exception {
        mvc.perform(get("/api/usuarios")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").isNotEmpty());
        mvc.perform(con(get("/api/usuarios"), "token.falso.manipulado", null)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/ordenes").contentType(MediaType.APPLICATION_JSON).content(orden(mouse.getId(), 1, "aprobado")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cadaRolSoloAccedeAloQueLeCorresponde() throws Exception {
        String admin = admin(), vendedor = vendedor(), cliente = cliente();

        // usuarios: solo admin
        mvc.perform(con(get("/api/usuarios"), admin, null)).andExpect(status().isOk());
        mvc.perform(con(get("/api/usuarios"), vendedor, null)).andExpect(status().isForbidden());
        mvc.perform(con(get("/api/usuarios"), cliente, null)).andExpect(status().isForbidden());
        // lista completa de órdenes: personal
        mvc.perform(con(get("/api/ordenes"), vendedor, null)).andExpect(status().isOk());
        mvc.perform(con(get("/api/ordenes"), cliente, null)).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").isNotEmpty());
        // mensajes de contacto: personal
        mvc.perform(con(get("/api/contacto"), cliente, null)).andExpect(status().isForbidden());
    }

    // ---------- CRUD de productos por rol ----------

    @Test
    void elCatalogoEsPublico() throws Exception {
        mvc.perform(get("/api/productos")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(get("/api/productos/" + mouse.getId())).andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Mouse"));
        mvc.perform(get("/api/productos/999999")).andExpect(status().isNotFound());
        mvc.perform(get("/api/categorias")).andExpect(status().isOk());
    }

    @Test
    void soloElAdminCreaEditaYEliminaProductos() throws Exception {
        String nuevo = productoNuevo("AC-009");

        mvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON).content(nuevo))
                .andExpect(status().isUnauthorized());
        mvc.perform(con(post("/api/productos"), cliente(), nuevo)).andExpect(status().isForbidden());
        mvc.perform(con(post("/api/productos"), vendedor(), nuevo)).andExpect(status().isForbidden());

        String admin = admin();
        MvcResult creado = mvc.perform(con(post("/api/productos"), admin, nuevo))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.codigo").value("AC-009")).andReturn();
        long id = json.readTree(creado.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(con(put("/api/productos/" + id), admin, productoNuevo("AC-009").replace("Webcam", "Webcam HD")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Webcam HD"));
        mvc.perform(con(delete("/api/productos/" + id), vendedor(), null)).andExpect(status().isForbidden());
        mvc.perform(con(delete("/api/productos/" + id), admin, null)).andExpect(status().isNoContent());
        mvc.perform(get("/api/productos/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void unProductoConDatosInvalidosOCodigoRepetidoSeRechaza() throws Exception {
        String admin = admin();

        mvc.perform(con(post("/api/productos"), admin, "{\"nombre\":\"\",\"precio\":-1}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").isNotEmpty());
        mvc.perform(con(post("/api/productos"), admin, productoNuevo("AC-001"))) // el código ya existe
                .andExpect(status().isConflict());
    }

    // ---------- flujo de compra y stock ----------

    @Test
    void unaCompraAprobadaDescuentaElStockYQuedaPagada() throws Exception {
        String cliente = cliente();

        MvcResult r = mvc.perform(con(post("/api/ordenes"), cliente, orden(teclado.getId(), 2, "aprobado")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("pagada"))
                .andExpect(jsonPath("$.total").value(64000)) // 2 x $32.000 (precio con 20% de oferta, leído de la base)
                .andReturn();

        assertThat(json.readTree(r.getResponse().getContentAsString()).get("codigo").asText()).startsWith("ORD-");
        assertThat(stockDe(teclado)).isEqualTo(1);
    }

    @Test
    void unPagoRechazadoDejaLaOrdenFallidaSinTocarElStockYSePuedeReintentar() throws Exception {
        String cliente = cliente();
        long id = crearOrden(cliente, mouse.getId(), 2, "rechazado");
        assertThat(stockDe(mouse)).isEqualTo(5);

        mvc.perform(con(get("/api/ordenes/" + id), cliente, null))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("fallida"));

        mvc.perform(con(put("/api/ordenes/" + id + "/pago"), cliente, "{\"pago\":\"aprobado\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("pagada"));
        assertThat(stockDe(mouse)).isEqualTo(3);

        mvc.perform(con(put("/api/ordenes/" + id + "/pago"), cliente, "{\"pago\":\"aprobado\"}"))
                .andExpect(status().isConflict()); // ya estaba pagada
        assertThat(stockDe(mouse)).isEqualTo(3);
    }

    @Test
    void sinStockSuficienteResponde409YNoCambiaNada() throws Exception {
        mvc.perform(con(post("/api/ordenes"), cliente(), orden(teclado.getId(), 10, "aprobado")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Stock insuficiente para Teclado."));

        assertThat(stockDe(teclado)).isEqualTo(3);
    }

    @Test
    void unaCompraSinProductosOConPagoInvalidoResponde400() throws Exception {
        String cliente = cliente();
        String vacia = orden(mouse.getId(), 1, "aprobado").replaceAll("\"items\":\\[[^]]*]", "\"items\":[]");

        mvc.perform(con(post("/api/ordenes"), cliente, vacia)).andExpect(status().isBadRequest());
        mvc.perform(con(post("/api/ordenes"), cliente, orden(mouse.getId(), 1, "tal-vez"))).andExpect(status().isBadRequest());
    }

    @Test
    void cadaClienteSoloVeSusPropiasOrdenes() throws Exception {
        String cliente = cliente();
        long id = crearOrden(cliente, mouse.getId(), 1, "aprobado");
        String otra = token("otra@gmail.com", "otra123");

        mvc.perform(con(get("/api/ordenes/mias"), cliente, null)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(con(get("/api/ordenes/mias"), otra, null)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(con(get("/api/ordenes/" + id), otra, null)).andExpect(status().isNotFound());
        mvc.perform(con(get("/api/ordenes/" + id), vendedor(), null)).andExpect(status().isOk());
        mvc.perform(con(get("/api/ordenes"), admin(), null)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
    }

    // ---------- contacto ----------

    @Test
    void elFormularioDeContactoEsPublicoPeroLosMensajesSoloLosVeElPersonal() throws Exception {
        String mensaje = "{\"nombre\":\"Ana\",\"correo\":\"ana@gmail.com\",\"comentario\":\"Hola, quiero saber más.\"}";

        mvc.perform(post("/api/contacto").contentType(MediaType.APPLICATION_JSON).content(mensaje))
                .andExpect(status().is2xxSuccessful());
        mvc.perform(get("/api/contacto")).andExpect(status().isUnauthorized());
        mvc.perform(con(get("/api/contacto"), vendedor(), null)).andExpect(status().isOk());
    }
}
