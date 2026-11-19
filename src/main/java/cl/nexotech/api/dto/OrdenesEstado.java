package cl.nexotech.api.dto;

import cl.nexotech.api.modelo.EstadoOrden;

public record OrdenesEstado(EstadoOrden estado, Long cantidad, Long total) {
}
