package cl.nexotech.api.dto;

import cl.nexotech.api.modelo.Rol;
import cl.nexotech.api.modelo.Usuario;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Usuario del sistema (nunca incluye la contraseña)")
public record UsuarioResponse(
        Long id,
        String run,
        String nombre,
        String apellidos,
        String correo,
        Rol rol,
        String region,
        String comuna,
        String direccion,
        Instant creadoEn) {

    public static UsuarioResponse de(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getRun(), u.getNombre(), u.getApellidos(), u.getCorreo(),
                u.getRol(), u.getRegion(), u.getComuna(), u.getDireccion(), u.getCreadoEn());
    }
}
