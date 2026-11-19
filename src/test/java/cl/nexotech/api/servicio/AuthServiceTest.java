package cl.nexotech.api.servicio;

import cl.nexotech.api.dto.LoginRequest;
import cl.nexotech.api.dto.PerfilRequest;
import cl.nexotech.api.dto.RegistroRequest;
import cl.nexotech.api.excepcion.ApiException;
import cl.nexotech.api.modelo.Rol;
import cl.nexotech.api.modelo.Usuario;
import cl.nexotech.api.repositorio.UsuarioRepository;
import cl.nexotech.api.seguridad.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pruebas unitarias de AuthService: el repositorio es un mock; el cifrado y los tokens son los reales. */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UsuarioRepository usuarios;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4); // costo bajo: pruebas rápidas
    private final JwtService jwt = new JwtService("una-clave-de-prueba-con-mas-de-32-caracteres", 15, 7);
    private AuthService servicio;
    private Usuario ana;

    @BeforeEach
    void preparar() {
        servicio = new AuthService(usuarios, encoder, jwt);
        ana = new Usuario();
        ana.setId(5L);
        ana.setRun("19011022K");
        ana.setNombre("Ana");
        ana.setApellidos("Pérez");
        ana.setCorreo("ana@duoc.cl");
        ana.setPasswordHash(encoder.encode("clave1"));
        ana.setRol(Rol.VENDEDOR);
    }

    private RegistroRequest registro(String correo, String run) {
        return new RegistroRequest(run, "Pedro", "Soto", correo, "clave123", "Región de Tarapacá", "Iquique", "Calle 1");
    }

    @Test
    void loginCorrectoEntregaTokenDeAccesoYDeRenovacion() {
        when(usuarios.findByCorreo("ana@duoc.cl")).thenReturn(Optional.of(ana));

        AuthService.Resultado resultado = servicio.login(new LoginRequest("ANA@duoc.cl ", "clave1"));

        assertThat(resultado.sesion().usuario().correo()).isEqualTo("ana@duoc.cl");
        assertThat(resultado.sesion().usuario().rol()).isEqualTo(Rol.VENDEDOR);
        assertThat(jwt.leer(resultado.sesion().accessToken()).esAcceso()).isTrue();
        assertThat(jwt.leer(resultado.tokenRenovacion()).esRenovacion()).isTrue();
        assertThat(resultado.sesion().expiraEnSegundos()).isEqualTo(900);
    }

    @Test
    void loginConContrasenaIncorrectaEsError401() {
        when(usuarios.findByCorreo("ana@duoc.cl")).thenReturn(Optional.of(ana));

        assertThatThrownBy(() -> servicio.login(new LoginRequest("ana@duoc.cl", "mala")))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getEstado()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(e.getMessage()).isEqualTo("Correo o contraseña incorrectos.");
                });
    }

    @Test
    void loginConCorreoInexistenteDaElMismoErrorQueUnaClaveMala() {
        when(usuarios.findByCorreo("nadie@duoc.cl")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.login(new LoginRequest("nadie@duoc.cl", "clave1")))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getEstado()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(e.getMessage()).isEqualTo("Correo o contraseña incorrectos.");
                });
    }

    @Test
    void registrarCreaSiempreUnClienteConLaContrasenaCifrada() {
        when(usuarios.existsByCorreo("pedro@gmail.com")).thenReturn(false);
        when(usuarios.existsByRun("20123456K")).thenReturn(false);
        when(usuarios.save(any(Usuario.class))).thenAnswer(i -> {
            Usuario u = i.getArgument(0);
            u.setId(9L);
            return u;
        });

        AuthService.Resultado resultado = servicio.registrar(registro("pedro@gmail.com", "20123456K"));

        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).save(guardado.capture());
        assertThat(guardado.getValue().getRol()).isEqualTo(Rol.CLIENTE);
        assertThat(guardado.getValue().getPasswordHash()).isNotEqualTo("clave123");
        assertThat(encoder.matches("clave123", guardado.getValue().getPasswordHash())).isTrue();
        assertThat(resultado.sesion().usuario().rol()).isEqualTo(Rol.CLIENTE);
    }

    @Test
    void registrarConCorreoOrunExistenteEsConflicto409() {
        when(usuarios.existsByCorreo("ana@duoc.cl")).thenReturn(true);

        assertThatThrownBy(() -> servicio.registrar(registro("ana@duoc.cl", "20123456K")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getEstado()).isEqualTo(HttpStatus.CONFLICT));
        verify(usuarios, never()).save(any());
    }

    @Test
    void renovarConUnTokenDeRenovacionValidoEntregaUnaSesionNueva() {
        when(usuarios.findById(5L)).thenReturn(Optional.of(ana));

        AuthService.Resultado resultado = servicio.renovar(jwt.generarRenovacion(ana));

        assertThat(jwt.leer(resultado.sesion().accessToken()).usuarioId()).isEqualTo(5L);
        assertThat(jwt.leer(resultado.tokenRenovacion()).esRenovacion()).isTrue();
    }

    @Test
    void renovarReflejaElRolActualDelUsuario() {
        when(usuarios.findById(5L)).thenReturn(Optional.of(ana));
        String renovacion = jwt.generarRenovacion(ana);
        ana.setRol(Rol.ADMIN); // el administrador le cambió el rol después de iniciar sesión

        AuthService.Resultado resultado = servicio.renovar(renovacion);

        assertThat(jwt.leer(resultado.sesion().accessToken()).rol()).isEqualTo(Rol.ADMIN);
    }

    @Test
    void renovarConUnTokenDeAccesoSeRechaza() {
        assertThatThrownBy(() -> servicio.renovar(jwt.generarAcceso(ana)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getEstado()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void renovarSinCookieOConTokenBasuraEsError401() {
        assertThatThrownBy(() -> servicio.renovar(null)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> servicio.renovar("  ")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> servicio.renovar("basura"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getEstado()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void renovarSiElUsuarioYaNoExisteEsError401() {
        when(usuarios.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.renovar(jwt.generarRenovacion(ana)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getEstado()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void cambiarLaContrasenaExigeLaActualCorrecta() {
        when(usuarios.findById(5L)).thenReturn(Optional.of(ana));
        PerfilRequest datos = new PerfilRequest("Ana", "Pérez", "Región X", "Comuna Y", "Calle 2", "nueva1", "equivocada");

        assertThatThrownBy(() -> servicio.actualizarPerfil(5L, datos))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getEstado()).isEqualTo(HttpStatus.BAD_REQUEST); // 400 y no 401: así el frontend no cierra la sesión
                    assertThat(e.getMessage()).isEqualTo("La contraseña actual no es correcta.");
                });
    }

    @Test
    void cambiarLaContrasenaConLaActualCorrectaLaGuardaCifrada() {
        when(usuarios.findById(5L)).thenReturn(Optional.of(ana));
        PerfilRequest datos = new PerfilRequest("Ana", "Pérez", "Región X", "Comuna Y", "Calle 2", "nueva1", "clave1");

        servicio.actualizarPerfil(5L, datos);

        assertThat(encoder.matches("nueva1", ana.getPasswordHash())).isTrue();
        assertThat(ana.getDireccion()).isEqualTo("Calle 2");
    }

    @Test
    void editarElPerfilSinContrasenaNoLaToca() {
        when(usuarios.findById(5L)).thenReturn(Optional.of(ana));
        String hashAntes = ana.getPasswordHash();

        servicio.actualizarPerfil(5L, new PerfilRequest("Ana María", "Pérez", "Región X", "Comuna Y", "Calle 2", null, null));

        assertThat(ana.getPasswordHash()).isEqualTo(hashAntes);
        assertThat(ana.getNombre()).isEqualTo("Ana María");
    }

    @Test
    void laContrasenaNuevaFueraDeRangoSeRechaza() {
        when(usuarios.findById(5L)).thenReturn(Optional.of(ana));
        PerfilRequest datos = new PerfilRequest("Ana", "Pérez", "R", "C", "D", "123", "clave1");

        assertThatThrownBy(() -> servicio.actualizarPerfil(5L, datos))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getEstado()).isEqualTo(HttpStatus.BAD_REQUEST));
    }
}
