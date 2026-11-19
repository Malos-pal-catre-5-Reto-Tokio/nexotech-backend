package cl.nexotech.api.servicio;

import cl.nexotech.api.dto.ContactoRequest;
import cl.nexotech.api.dto.ContactoResponse;
import cl.nexotech.api.modelo.MensajeContacto;
import cl.nexotech.api.repositorio.MensajeContactoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ContactoService {

    private final MensajeContactoRepository mensajes;

    public ContactoService(MensajeContactoRepository mensajes) {
        this.mensajes = mensajes;
    }

    @Transactional
    public void recibir(ContactoRequest datos) {
        MensajeContacto mensaje = new MensajeContacto();
        mensaje.setNombre(datos.nombre());
        mensaje.setCorreo(datos.correo());
        mensaje.setComentario(datos.comentario());
        mensajes.save(mensaje);
    }

    @Transactional(readOnly = true)
    public List<ContactoResponse> listar() {
        return mensajes.findAllByOrderByIdDesc().stream().map(ContactoResponse::de).toList();
    }
}
