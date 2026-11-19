package cl.nexotech.api.seguridad;

import cl.nexotech.api.modelo.Rol;
import cl.nexotech.api.modelo.Usuario;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas unitarias de la generación y verificación de tokens JWT (sin Spring ni base de datos). */
class JwtServiceTest {

    private static final String SECRETO = "una-clave-de-prueba-con-mas-de-32-caracteres";

    private final JwtService jwt = new JwtService(SECRETO, 15, 7);

    private static Usuario usuario(Rol rol) {
        Usuario u = new Usuario();
        u.setId(7L);
        u.setCorreo("ana@duoc.cl");
        u.setRol(rol);
        return u;
    }

    @Test
    void elTokenDeAccesoLlevaUsuarioCorreoYRol() {
        String token = jwt.generarAcceso(usuario(Rol.VENDEDOR));

        JwtService.DatosToken datos = jwt.leer(token);

        assertThat(datos.usuarioId()).isEqualTo(7L);
        assertThat(datos.correo()).isEqualTo("ana@duoc.cl");
        assertThat(datos.rol()).isEqualTo(Rol.VENDEDOR);
        assertThat(datos.esAcceso()).isTrue();
        assertThat(datos.esRenovacion()).isFalse();
    }

    @Test
    void elTokenDeRenovacionSeDistingueDelDeAcceso() {
        JwtService.DatosToken datos = jwt.leer(jwt.generarRenovacion(usuario(Rol.CLIENTE)));

        assertThat(datos.esRenovacion()).isTrue();
        assertThat(datos.esAcceso()).isFalse();
    }

    @Test
    void lasDuracionesSalenDeLaConfiguracion() {
        assertThat(jwt.segundosAcceso()).isEqualTo(15 * 60);
        assertThat(jwt.duracionRenovacion().toDays()).isEqualTo(7);
    }

    @Test
    void unTokenAlteradoSeRechaza() {
        String token = jwt.generarAcceso(usuario(Rol.CLIENTE));
        String alterado = token.substring(0, token.length() - 2) + (token.endsWith("AA") ? "BB" : "AA");

        assertThatThrownBy(() -> jwt.leer(alterado)).isInstanceOf(JwtException.class);
    }

    @Test
    void unTokenFirmadoConOtraClaveSeRechaza() {
        JwtService ajeno = new JwtService("otra-clave-distinta-de-mas-de-32-caracteres!!", 15, 7);
        String token = ajeno.generarAcceso(usuario(Rol.ADMIN));

        assertThatThrownBy(() -> jwt.leer(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void unTokenVencidoSeRechaza() {
        JwtService vencido = new JwtService(SECRETO, -1, 7); // expira un minuto antes de emitirse
        String token = vencido.generarAcceso(usuario(Rol.CLIENTE));

        assertThatThrownBy(() -> jwt.leer(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void textoQueNoEsUnTokenSeRechaza() {
        assertThatThrownBy(() -> jwt.leer("esto-no-es-un-jwt")).isInstanceOf(JwtException.class);
    }

    @Test
    void unaClaveCortaImpideArrancar() {
        assertThatThrownBy(() -> new JwtService("corta", 15, 7))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 caracteres");
    }
}
