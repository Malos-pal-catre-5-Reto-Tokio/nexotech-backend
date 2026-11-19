package cl.nexotech.api.controlador;

import cl.nexotech.api.dto.ProductoRequest;
import cl.nexotech.api.dto.ProductoResponse;
import cl.nexotech.api.excepcion.ErrorRespuesta;
import cl.nexotech.api.servicio.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
@Tag(name = "Productos", description = "Catálogo de productos, ofertas y stock")
public class ProductoController {

    private final ProductoService servicio;

    public ProductoController(ProductoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Listar productos",
            description = "Catálogo completo. Todos los filtros son opcionales y se pueden combinar.")
    public List<ProductoResponse> listar(
            @Parameter(description = "Slug de la categoría, por ejemplo notebooks")
            @RequestParam(required = false) String categoria,
            @Parameter(description = "Solo productos en oferta")
            @RequestParam(defaultValue = "false") boolean oferta,
            @Parameter(description = "Solo productos con stock crítico")
            @RequestParam(defaultValue = "false") boolean critico,
            @Parameter(description = "Texto a buscar en el nombre o el código")
            @RequestParam(required = false) String q) {
        return servicio.listar(categoria, oferta, critico, q);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un producto por id")
    @ApiResponse(responseCode = "404", description = "El producto no existe",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public ProductoResponse obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear un producto")
    @ApiResponse(responseCode = "400", description = "Datos inválidos o categoría inexistente",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "409", description = "El código ya existe",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public ProductoResponse crear(@Valid @RequestBody ProductoRequest datos) {
        return servicio.crear(datos);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{id}")
    @Operation(summary = "Editar un producto")
    @ApiResponse(responseCode = "400", description = "Datos inválidos o categoría inexistente",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "404", description = "El producto no existe",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "409", description = "El código ya existe",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public ProductoResponse actualizar(@PathVariable Long id, @Valid @RequestBody ProductoRequest datos) {
        return servicio.actualizar(id, datos);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar un producto",
            description = "Las boletas de órdenes pasadas conservan el nombre y precio del producto.")
    @ApiResponse(responseCode = "404", description = "El producto no existe",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public void eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
    }
}
