package cl.nexotech.api.seguridad;

import cl.nexotech.api.modelo.Rol;

/** Usuario que hizo la petición, reconstruido desde el token de acceso. Se obtiene con @AuthenticationPrincipal. */
public record UsuarioAutenticado(Long id, String correo, Rol rol) {
}
