package cl.nexotech.api.controlador;

import cl.nexotech.api.dto.CategoriaRequest;
import cl.nexotech.api.dto.CategoriaResponse;
import cl.nexotech.api.excepcion.ErrorRespuesta;
import cl.nexotech.api.servicio.CategoriaService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categorias")
@Tag(name = "Categorías", description = "Categorías del catálogo")
public class CategoriaController {

    private final CategoriaService servicio;

    public CategoriaController(CategoriaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Listar categorías", description = "Devuelve todas las categorías ordenadas por nombre.")
    public List<CategoriaResponse> listar() {
        return servicio.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una categoría por id")
    @ApiResponse(responseCode = "404", description = "La categoría no existe",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public CategoriaResponse obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear una categoría")
    @ApiResponse(responseCode = "400", description = "Datos inválidos",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "409", description = "El slug ya existe",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public CategoriaResponse crear(@Valid @RequestBody CategoriaRequest datos) {
        return servicio.crear(datos);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{id}")
    @Operation(summary = "Editar una categoría")
    @ApiResponse(responseCode = "404", description = "La categoría no existe",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "409", description = "El slug ya existe",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public CategoriaResponse actualizar(@PathVariable Long id, @Valid @RequestBody CategoriaRequest datos) {
        return servicio.actualizar(id, datos);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar una categoría", description = "No se puede eliminar si tiene productos asociados.")
    @ApiResponse(responseCode = "404", description = "La categoría no existe",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "409", description = "La categoría tiene productos",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public void eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
    }
}
