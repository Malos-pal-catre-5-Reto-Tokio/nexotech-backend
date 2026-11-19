package cl.nexotech.api.modelo;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Guarda el estado de la orden en la columna VARCHAR usando su valor en minúscula. */
@Converter(autoApply = true)
public class EstadoOrdenConverter implements AttributeConverter<EstadoOrden, String> {

    @Override
    public String convertToDatabaseColumn(EstadoOrden estado) {
        return estado == null ? null : estado.getValor();
    }

    @Override
    public EstadoOrden convertToEntityAttribute(String valor) {
        return valor == null ? null : EstadoOrden.desde(valor);
    }
}
