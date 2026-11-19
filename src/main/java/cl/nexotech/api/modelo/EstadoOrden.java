package cl.nexotech.api.modelo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/** Estado de pago de una orden. En la base de datos y en el JSON viaja en minúscula. */
public enum EstadoOrden {
    PAGADA("pagada"),
    FALLIDA("fallida"),
    PENDIENTE("pendiente");

    private final String valor;

    EstadoOrden(String valor) {
        this.valor = valor;
    }

    @JsonValue
    public String getValor() {
        return valor;
    }

    @JsonCreator
    public static EstadoOrden desde(String texto) {
        return Arrays.stream(values())
                .filter(e -> e.valor.equalsIgnoreCase(texto))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El estado de la orden no es válido."));
    }
}
