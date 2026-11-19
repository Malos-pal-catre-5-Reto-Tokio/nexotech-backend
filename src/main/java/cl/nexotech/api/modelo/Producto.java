package cl.nexotech.api.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "productos")
@Getter
@Setter
@NoArgsConstructor
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String codigo;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 1000)
    private String descripcion;

    /** Precio normal en pesos chilenos (sin decimales). */
    @Column(nullable = false)
    private Integer precio;

    @Column(nullable = false)
    private Integer stock;

    /** Con este stock o menos el producto se considera en nivel crítico. */
    @Column(name = "stock_critico", nullable = false)
    private Integer stockCritico;

    @Column(length = 255)
    private String imagen;

    /** Porcentaje de descuento de la oferta; 0 significa que no está en oferta. */
    @Column(name = "descuento_pct", nullable = false)
    private Integer descuentoPct;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    /** Precio final con el descuento de oferta aplicado, redondeado al peso. */
    public int precioFinal() {
        return (int) Math.round(precio * (1 - descuentoPct / 100.0));
    }

    public boolean enNivelCritico() {
        return stock <= stockCritico;
    }
}
