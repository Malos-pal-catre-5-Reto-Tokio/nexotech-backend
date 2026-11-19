package cl.nexotech.api.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Datos que aparecen en la documentación Swagger (http://localhost:8080/swagger-ui.html).
 * El botón "Authorize" permite pegar el accessToken devuelto por /api/auth/login para probar los endpoints protegidos.
 */
@Configuration
@OpenAPIDefinition(info = @Info(
        title = "API NexoTech",
        version = "1.0.0",
        description = "API REST de la tienda de tecnología NexoTech: catálogo, usuarios, órdenes y reportes."))
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Token de acceso JWT obtenido en /api/auth/login")
public class OpenApiConfig {
}
