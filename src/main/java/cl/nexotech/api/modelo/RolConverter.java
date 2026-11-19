package cl.nexotech.api.modelo;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Guarda el rol en la columna VARCHAR usando su valor en minúscula. */
@Converter(autoApply = true)
public class RolConverter implements AttributeConverter<Rol, String> {

    @Override
    public String convertToDatabaseColumn(Rol rol) {
        return rol == null ? null : rol.getValor();
    }

    @Override
    public Rol convertToEntityAttribute(String valor) {
        return valor == null ? null : Rol.desde(valor);
    }
}
