# 🚗 Sistema de Gestión de Parqueaderos - Api Park

Una aplicación completa de gestión de parqueaderos desarrollada con Spring Boot que permite administrar usuarios, vehículos, espacios de parqueo y generar reportes con integración a servicios de nube.

## 🚀 Características Principales

- **Gestión de Usuarios**: Registro, autenticación y autorización con JWT
- **Gestión de Parqueaderos**: CRUD completo de espacios de parqueo
- **Gestión de Vehículos**: Administración de vehículos y sus propietarios
- **Sistema de Rankings**: Rankings de parqueaderos y socios más utilizados
- **Indicadores y Reportes**: Generación de indicadores de negocio
- **Procesamiento de Archivos**: Generación de reportes Excel automáticos
- **Integración AWS S3/R2**: Almacenamiento seguro de archivos en la nube
- **Sistema de Colas**: Procesamiento asíncrono de tareas con RabbitMQ
- **Notificaciones por Email**: Envío automático de reportes por correo
- **Documentación API**: Integración con Swagger/OpenAPI 3

## 🛠️ Tecnologías Utilizadas

### Backend
- **Java 21** - Lenguaje de programación principal
- **Spring Boot 3.5.5** - Framework principal
- **Spring Data JPA** - Persistencia de datos
- **Spring Security** - Autenticación y autorización
- **Spring AMQP** - Integración con RabbitMQ
- **JWT (JSON Web Tokens)** - Autenticación stateless

### Base de Datos
- **PostgreSQL** - Base de datos principal
- **Flyway** - Migraciones versionadas del esquema (`src/main/resources/db/migration`)
- **Docker** - Containerización de la base de datos

### Servicios Externos
- **Cloudflare R2 (Compatible S3)** - Almacenamiento de archivos
- **RabbitMQ** - Sistema de colas para procesamiento asíncrono

### Utilidades
- **Lombok** - Reducción de código boilerplate
- **Apache POI** - Generación de archivos Excel
- **Jackson** - Serialización/deserialización JSON
- **Maven** - Gestión de dependencias
- **Swagger/OpenAPI 3** - Documentación de API

## 📋 Prerrequisitos

- Java 21 o superior
- Maven 3.6+
- Docker y Docker Compose
- Cuenta de AWS con acceso a S3 (o Cloudflare R2)
- Servidor RabbitMQ

## ⚙️ Instalación y Configuración

Sigue estos pasos para configurar y ejecutar el proyecto localmente.

### 1. Clonar el Repositorio

```bash
git clone https://github.com/tu-usuario/api-park.git # Reemplaza con la URL de tu repositorio
cd api-park
```

### 2. Configurar Variables de Entorno

Crea un archivo `.env` en la raíz del proyecto con las siguientes variables. Asegúrate de reemplazar los valores de ejemplo con tus credenciales y configuraciones reales.

```ini
# ================================
# VARIABLES OBLIGATORIAS - CONFIGURAR
# ================================

# R2/S3 - REQUERIDO
R2_BUCKET_PATH=https://pub-xxxxxxxxxx.r2.dev
R2_BUCKET_NAME=mi-bucket
R2_BUCKET_ACCESS_KEY=xxxxxxxxxxxxxxxx
R2_BUCKET_SECRET_KEY=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
R2_BUCKET_REGION=auto
R2_ACCOUNT_ID=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

# JWT - REQUERIDO
APP_SECURITY_JWT_SECRET=mi_clave_jwt_super_secreta_de_al_menos_32_caracteres
APP_SECURITY_JWT_EXPIRATION_HOURS=6

# RabbitMQ - REQUERIDO
RABBITMQ_URL=amqp://guest:guest@localhost:5672/
QUEUE_NAME=email_queue
TYPE_MESSAGE=email_notification
APP_SUBJECT=Notificación del Sistema de Parqueadero

# ================================
# VARIABLES OPCIONALES (tienen valores por defecto)
# ================================

# Base de datos (opcional si usas Docker con la config por defecto)
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5332/park
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=root

# Configuración de aplicación (opcional)
SPRING_APPLICATION_NAME=spring-boot
LOGGING_LEVEL_ROOT=INFO
```

### 3. Iniciar Servicios con Docker Compose (Base de Datos y RabbitMQ)

Asegúrate de tener Docker y Docker Compose instalados. Desde la raíz del proyecto, ejecuta:

```bash
docker-compose up -d
```
Esto levantará un contenedor de PostgreSQL y otro de RabbitMQ.

### 4. Compilar y Ejecutar la Aplicación

Una vez que los servicios de Docker estén en funcionamiento, puedes compilar y ejecutar la aplicación Spring Boot:

```bash
mvn clean install
mvn spring-boot:run
```

La aplicación estará disponible en `http://localhost:8080` (o el puerto configurado).

## 📂 Estructura del Proyecto (arquitectura hexagonal)

```
src/main/java/com/nelumbo/park/
├── domain/                     # Modelo y reglas de negocio, sin Spring
│   ├── model/                  # User, Parking, Vehicle, stats, report
│   ├── service/                # ParkingFeeCalculator, rankings, reportes
│   └── exception/              # Errores de negocio
├── application/
│   ├── port/in/                # Casos de uso (LoginUseCase, RegisterVehicleEntryUseCase, ...)
│   ├── port/out/               # Puertos hacia infraestructura (repositorios, hasher, JWT, correo, ...)
│   └── service/                # Implementación de los casos de uso
└── infrastructure/
    ├── adapter/in/web/         # Controladores REST, DTOs, GlobalExceptionHandler
    ├── adapter/in/scheduler/   # Cron del reporte diario
    ├── adapter/out/            # JPA, seguridad, RabbitMQ, R2/S3, Excel/PDF
    └── config/                 # Seguridad (JWT), Jackson, inicialización
src/main/resources/db/migration # V1 esquema base, V2 placa única dentro, V3 índices
```

## 🗄️ Base de datos y migraciones

El esquema lo crea Flyway; Hibernate solo lo valida (`ddl-auto=validate`).

- **Base nueva:** se aplican V1, V2 y V3.
- **Base existente creada antes con `ddl-auto=update`:** en el primer arranque Flyway la marca como versión 1
  (`baseline-on-migrate`) y aplica solo V2 y V3.
- **V2** crea el índice único parcial `ux_vehicles_plate_inside` (una placa solo puede estar `IN` una vez, en
  cualquier parqueadero). **Antes de desplegar** sobre una base existente, verifica que no haya duplicados; si
  los hay, la migración falla sin cambiar nada:

```sql
SELECT upper(trim(plate_number)) AS plate, count(*)
FROM "vehicles" WHERE status = 'IN'
GROUP BY upper(trim(plate_number)) HAVING count(*) > 1;
```

`SPRING_DATASOURCE_URL` acepta `jdbc:postgresql://host:puerto/base` o `postgresql://usuario:clave@host:puerto/base`.
Si la base no existe se crea al arrancar (desactívalo con `app.database.create-if-missing=false`).

## 🔐 Permisos

| Operación | ADMIN | SOCIO |
|---|---|---|
| Crear/listar usuarios, crear/editar/borrar parqueaderos, rankings de socios y parqueaderos | ✅ | ❌ |
| Ver parqueaderos | todos | solo los suyos |
| Listar vehículos | todos | los que registró |
| Entrada / salida / ver un vehículo | ❌ | solo en sus parqueaderos |
| Borrar un vehículo | ❌ | solo de sus parqueaderos |
| Logs del webhook de SonarQube | ✅ | ❌ |

El login responde siempre `401 {"error":"Credenciales incorrectas"}` ante email desconocido o contraseña
incorrecta, y bloquea la IP 5 minutos tras 3 intentos fallidos. Un token vencido o inválido responde 401.

## 🧪 Pruebas

```bash
./mvnw test
```

- Pruebas unitarias de dominio y servicios, slices web (MockMvc con la seguridad real) y de persistencia sobre H2.
- Pruebas contra **PostgreSQL real** (migraciones, índice único parcial, concurrencia, API HTTP completa):
  se ejecutan con Testcontainers si hay Docker, o contra una base desechable indicada con
  `TEST_POSTGRES_URL=jdbc:postgresql://localhost:5432/park_test` (`TEST_POSTGRES_USERNAME`,
  `TEST_POSTGRES_PASSWORD`). Las pruebas borran todas las filas de esa base. Sin Docker ni
  `TEST_POSTGRES_URL` se reportan como *skipped*.

## 💡 Ejemplos de Uso

### Autenticación

`POST /auth/login`
```json
{ "email": "socio@example.com", "password": "secret123" }
```
Respuesta:
```json
{ "id": "…", "username": "socio", "email": "socio@example.com", "role": "SOCIO", "tokenType": "Bearer", "accessToken": "eyJ…" }
```

### Registrar la entrada de un vehículo (SOCIO dueño del parqueadero)

`POST /vehicles/` con `Authorization: Bearer <token>`
```json
{ "plateNumber": "ABC123", "modelVehicle": "Mazda 3", "idParking": "<id del parqueadero>" }
```

### Registrar la salida

`PUT /vehicles/` con `Authorization: Bearer <token>`
```json
{ "plateNumber": "ABC123", "modelVehicle": "Mazda 3", "idParking": "<id del parqueadero>" }
```
Se cobra cada hora iniciada (`totalCost`, p. ej. `"$ 4.000"`).

### Documentación de la API

Swagger UI: `http://localhost:8080/swagger-ui.html`

## 🤝 Cómo Contribuir

¡Nos encantaría recibir tus contribuciones! Si deseas mejorar este proyecto, sigue estos pasos:

1.  Haz un "fork" de este repositorio.
2.  Crea una nueva rama para tu característica (`git checkout -b feature/nueva-caracteristica`).
3.  Realiza tus cambios y asegúrate de que las pruebas pasen.
4.  Haz "commit" de tus cambios (`git commit -am 'feat: Añade nueva característica X'`).
5.  Sube tu rama (`git push origin feature/nueva-caracteristica`).
6.  Abre un "Pull Request" detallando tus cambios.

Por favor, asegúrate de seguir las convenciones de código existentes y de escribir pruebas para tus nuevas funcionalidades.

## 📄 Licencia

Este proyecto está bajo la Licencia MIT. Consulta el archivo `LICENSE` para más detalles.

## 📧 Contacto

Si tienes alguna pregunta o sugerencia, no dudes en contactar a
`Jesus Cantor / jedacan58@gmail.com`.
