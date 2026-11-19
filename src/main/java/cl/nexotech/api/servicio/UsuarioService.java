package cl.nexotech.api.servicio;

import cl.nexotech.api.dto.UsuarioRequest;
import cl.nexotech.api.dto.UsuarioResponse;
import cl.nexotech.api.excepcion.ApiException;
import cl.nexotech.api.modelo.Rol;
import cl.nexotech.api.modelo.Usuario;
import cl.nexotech.api.repositorio.UsuarioRepository;
import cl.nexotech.api.seguridad.UsuarioAutenticado;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Gestión de usuarios por un administrador. */
@Service
public class UsuarioService {

    private static final int PASSWORD_MIN = 4;
    private static final int PASSWORD_MAX = 10;

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;

    public UsuarioService(UsuarioRepository usuarios, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarios.findAll(Sort.by("id")).stream().map(UsuarioResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtener(Long id) {
        return UsuarioResponse.de(buscar(id));
    }

    @Transactional
    public UsuarioResponse crear(UsuarioRequest datos) {
        validarPassword(datos.password(), true);
        if (usuarios.existsByCorreo(datos.correo()) || usuarios.existsByRun(datos.run())) {
            throw ApiException.conflicto("Ya existe un usuario con ese correo o RUN.");
        }
        Usuario usuario = new Usuario();
        copiar(datos, usuario);
        usuario.setPasswordHash(encoder.encode(datos.password()));
        return UsuarioResponse.de(usuarios.save(usuario));
    }

    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioRequest datos, UsuarioAutenticado actual) {
        Usuario usuario = buscar(id);
        if (id.equals(actual.id()) && datos.rol() != Rol.ADMIN) {
            throw ApiException.conflicto("No puedes quitarte el rol de administrador.");
        }
        if (usuarios.existsByCorreoAndIdNot(datos.correo(), id) || usuarios.existsByRunAndIdNot(datos.run(), id)) {
            throw ApiException.conflicto("Ya existe un usuario con ese correo o RUN.");
        }
        validarPassword(datos.password(), false);
        copiar(datos, usuario);
        if (datos.password() != null && !datos.password().isEmpty()) {
            usuario.setPasswordHash(encoder.encode(datos.password()));
        }
        return UsuarioResponse.de(usuario);
    }

    @Transactional
    public void eliminar(Long id, UsuarioAutenticado actual) {
        if (id.equals(actual.id())) {
            throw ApiException.conflicto("No puedes eliminar tu propia cuenta.");
        }
        usuarios.delete(buscar(id)); // sus órdenes se conservan: la clave foránea queda en NULL
    }

    private Usuario buscar(Long id) {
        return usuarios.findById(id).orElseThrow(() -> ApiException.noEncontrado("Usuario no encontrado."));
    }

    private void copiar(UsuarioRequest datos, Usuario usuario) {
        usuario.setRun(datos.run());
        usuario.setNombre(datos.nombre());
        usuario.setApellidos(datos.apellidos());
        usuario.setCorreo(datos.correo());
        usuario.setRol(datos.rol());
        usuario.setRegion(datos.region());
        usuario.setComuna(datos.comuna());
        usuario.setDireccion(datos.direccion());
    }

    /** Al crear la contraseña es obligatoria; al editar solo se valida si se envía una nueva. */
    private void validarPassword(String password, boolean obligatoria) {
        boolean vacia = password == null || password.isEmpty();
        if (vacia && !obligatoria) return;
        if (vacia || password.length() < PASSWORD_MIN || password.length() > PASSWORD_MAX) {
            throw ApiException.solicitudInvalida(
                    "La contraseña debe tener entre " + PASSWORD_MIN + " y " + PASSWORD_MAX + " caracteres.");
        }
    }
}
