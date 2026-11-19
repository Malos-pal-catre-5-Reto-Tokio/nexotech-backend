package cl.nexotech.api.modelo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/** Roles del sistema. En la base de datos y en el JSON viajan en minúscula ("admin", "vendedor", "cliente"). */
public enum Rol {
    ADMIN("admin"),
    VENDEDOR("vendedor"),
    CLIENTE("cliente");

    private final String valor;

    Rol(String valor) {
        this.valor = valor;
    }

    @JsonValue
    public String getValor() {
        return valor;
    }

    @JsonCreator
    public static Rol desde(String texto) {
        return Arrays.stream(values())
                .filter(r -> r.valor.equalsIgnoreCase(texto))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El rol no es válido."));
    }
}
