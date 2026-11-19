# NexoTech — Backend (Spring Boot + PostgreSQL en Neon)

API REST de la tienda NexoTech. Proyecto de la Evaluación Parcial 3 de Desarrollo Fullstack II.

## Tecnologías

| Área | Tecnología |
|---|---|
| Lenguaje y framework | Java 21, Spring Boot 3.3 |
| Persistencia | Spring Data JPA (Hibernate), PostgreSQL alojado en [Neon](https://neon.tech) |
| Seguridad | Spring Security, JWT (jjwt), BCrypt |
| Documentación | Swagger UI (springdoc-openapi) |
| Utilidades | Lombok, Bean Validation |

## Requisitos

- JDK 21 y Maven 3.9 (o el wrapper del IDE).
- Una base de datos en Neon.

## Base de datos (Neon)

1. En Neon crea un proyecto y copia los datos de conexión (**Connection details**).
2. Abre **SQL Editor**, pega todo `database/nexotech-neon.sql` y ejecútalo. Crea las tablas y carga productos,
   categorías y tres usuarios de demostración.
3. Define estas variables de entorno antes de ejecutar la API:

| Variable | Ejemplo |
|---|---|
| `DB_URL` | `jdbc:postgresql://ep-nombre-123456.sa-east-1.aws.neon.tech/neondb?sslmode=require` |
| `DB_USER` | usuario de Neon |
| `DB_PASSWORD` | contraseña de Neon |
| `JWT_SECRET` | clave para firmar los tokens, mínimo 32 caracteres (`openssl rand -base64 48`) |

En VS Code puedes definirlas en `.vscode/launch.json` (campo `env`) o en la terminal:

```powershell
$env:DB_URL = "jdbc:postgresql://..."
$env:DB_USER = "..."
$env:DB_PASSWORD = "..."
$env:JWT_SECRET = "una-clave-larga-de-al-menos-32-caracteres"
mvn spring-boot:run
```

La API queda en `http://localhost:8080`. Al arrancar, Hibernate valida que las entidades coincidan con las tablas
(`ddl-auto=validate`); si algo no calza, el error indica la tabla o columna.

## Usuarios de demostración

| Correo | Contraseña | Rol |
|---|---|---|
| admin@duoc.cl | admin123 | admin |
| vendedor@duoc.cl | vend123 | vendedor |
| cliente@gmail.com | cliente123 | cliente |

## Modelo de datos

| Tabla | Entidad | Descripción |
|---|---|---|
| `categorias` | `Categoria` | Categorías del catálogo |
| `productos` | `Producto` | Productos con precio, stock, stock crítico y descuento de oferta |
| `usuarios` | `Usuario` | Cuentas con rol `admin`, `vendedor` o `cliente` y contraseña en BCrypt |
| `ordenes` | `Orden` | Compra con datos de entrega, descuento, total y estado de pago |
| `orden_items` | `OrdenItem` | Líneas de la orden; copian nombre y precio para que la boleta no cambie |
| `mensajes_contacto` | `MensajeContacto` | Mensajes del formulario de contacto |

Las entidades están en `cl.nexotech.api.modelo` y los repositorios en `cl.nexotech.api.repositorio`.

## Documentación Swagger

Con la API en ejecución:

- Interfaz interactiva: http://localhost:8080/swagger-ui.html
- Especificación OpenAPI (JSON): http://localhost:8080/v3/api-docs

## Autenticación y roles

- **Token de acceso (JWT):** dura 15 minutos y se envía en cada petición: `Authorization: Bearer <token>`.
- **Token de renovación:** dura 7 días y viaja en una cookie `HttpOnly` (`refresh_token`, ruta `/api/auth`). JavaScript no puede leerla.
  `POST /api/auth/refresh` entrega un nuevo token de acceso, lo que permite restaurar la sesión al recargar la página.
- **Roles:** `admin`, `vendedor` y `cliente`. Se validan en el servidor con `@PreAuthorize`; ocultar botones en el frontend es solo cosmético.
- **Probar en Swagger:** haz login en `POST /api/auth/login`, copia el `accessToken`, pulsa **Authorize** y pégalo.
- Sin token o con token vencido la API responde `401`; con un rol insuficiente responde `403`.
- Los tokens se firman con `JWT_SECRET` (HS256). Si cambias el rol de un usuario, el cambio se refleja al renovar el token de acceso (hasta 15 minutos).

## Endpoints

### Autenticación

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| POST | `/api/auth/login` | Público | Iniciar sesión |
| POST | `/api/auth/registro` | Público | Crear cuenta de cliente |
| POST | `/api/auth/refresh` | Cookie de renovación | Nuevo token de acceso |
| POST | `/api/auth/logout` | Público | Elimina la cookie de renovación |
| GET | `/api/auth/me` | Autenticado | Mi perfil |
| PUT | `/api/auth/perfil` | Autenticado | Editar mi perfil y contraseña |

### Catálogo

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/categorias` | Listar categorías |
| GET | `/api/categorias/{id}` | Obtener una categoría |
| POST | `/api/categorias` | Crear categoría |
| PUT | `/api/categorias/{id}` | Editar categoría |
| DELETE | `/api/categorias/{id}` | Eliminar categoría (409 si tiene productos) |
| GET | `/api/productos` | Listar productos. Filtros: `categoria`, `oferta`, `critico`, `q` |
| GET | `/api/productos/{id}` | Obtener un producto |
| POST | `/api/productos` | Crear producto |
| PUT | `/api/productos/{id}` | Editar producto |
| DELETE | `/api/productos/{id}` | Eliminar producto |

### Usuarios (solo `admin`)

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/usuarios` | Listar usuarios |
| GET | `/api/usuarios/{id}` | Obtener un usuario |
| POST | `/api/usuarios` | Crear usuario (el admin elige el rol) |
| PUT | `/api/usuarios/{id}` | Editar usuario; la contraseña solo cambia si se envía una nueva |
| DELETE | `/api/usuarios/{id}` | Eliminar usuario (no se puede eliminar la propia cuenta) |

### Órdenes (comprar exige iniciar sesión)

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| POST | `/api/ordenes` | Autenticado | Crear orden. Precios, oferta y cupón `NEXO10` se calculan en el servidor |
| GET | `/api/ordenes` | admin, vendedor | Listar todas las órdenes (`?usuario=id` filtra por usuario) |
| GET | `/api/ordenes/mias` | Autenticado | Mi historial de compras |
| GET | `/api/ordenes/{id}` | Dueño, admin, vendedor | Ver una orden (boleta); para otros usuarios responde 404 |
| PUT | `/api/ordenes/{id}/pago` | Dueño, admin, vendedor | Reintentar el pago de una orden fallida |

Reglas de negocio de las órdenes:

- Los precios se leen de la base de datos; el navegador solo envía ids y cantidades.
- Con pago `aprobado` el stock se descuenta y la orden queda `pagada`; con `rechazado` queda `fallida` y el stock no cambia.
- Las filas de producto se bloquean durante la compra (`PESSIMISTIC_WRITE`), así dos compras simultáneas no venden la misma unidad.
- Si no hay stock suficiente la compra completa se revierte y responde `409`.

### Reportes y contacto

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| GET | `/api/reportes` | admin, vendedor | Resumen, más vendidos, stock por categoría, órdenes por estado y ventas de 30 días |
| POST | `/api/contacto` | Público | Enviar un mensaje de contacto |
| GET | `/api/contacto` | admin, vendedor | Listar los mensajes recibidos |

### Permisos y errores

Consultar el catálogo (GET) es público; crear, editar y eliminar productos y categorías es solo para `admin`.

Los errores siempre responden `{"error": "mensaje"}` con el código HTTP correspondiente (400, 401, 403, 404, 409, 500).
