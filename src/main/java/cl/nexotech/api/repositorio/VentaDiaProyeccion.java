package cl.nexotech.api.repositorio;

/** Fila del reporte de ventas por día (resultado de una consulta nativa). */
public interface VentaDiaProyeccion {

    String getDia();

    Long getOrdenes();

    Long getTotal();
}
