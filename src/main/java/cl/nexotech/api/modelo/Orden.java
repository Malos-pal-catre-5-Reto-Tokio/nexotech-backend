package cl.nexotech.api.modelo;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ordenes")
@Getter
@Setter
@NoArgsConstructor
public class Orden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Dueño de la orden. Como comprar exige iniciar sesión siempre existe; solo queda nulo si luego se elimina la cuenta. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false, length = 50)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String apellidos;

    @Column(nullable = false, length = 100)
    private String correo;

    @Column(nullable = false, length = 150)
    private String calle;

    @Column(length = 30)
    private String departamento;

    @Column(nullable = false, length = 80)
    private String region;

    @Column(nullable = false, length = 60)
    private String comuna;

    @Column(length = 300)
    private String indicaciones;

    /** Monto descontado por cupón. */
    @Column(nullable = false)
    private Integer descuento = 0;

    @Column(nullable = false)
    private Integer total;

    @Column(nullable = false, length = 10)
    private EstadoOrden estado = EstadoOrden.PENDIENTE;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<OrdenItem> items = new ArrayList<>();

    public void agregarItem(OrdenItem item) {
        item.setOrden(this);
        items.add(item);
    }

    /** Código visible de la boleta, por ejemplo ORD-000012. */
    public String codigo() {
        return String.format("ORD-%06d", id);
    }

    @PrePersist
    void asignarFechaCreacion() {
        if (creadoEn == null) creadoEn = Instant.now();
    }
}
