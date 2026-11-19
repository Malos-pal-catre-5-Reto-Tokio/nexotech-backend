package cl.nexotech.api.repositorio;

import cl.nexotech.api.modelo.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreo(String correo);

    long countByCreadoEnAfter(Instant desde);

    boolean existsByCorreo(String correo);

    boolean existsByRun(String run);

    boolean existsByCorreoAndIdNot(String correo, Long id);

    boolean existsByRunAndIdNot(String run, Long id);
}
