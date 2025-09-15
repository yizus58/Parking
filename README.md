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
- **Docker** - Containerización de la base de datos

### Servicios Externos
- **Cloudflare R2 (Compatible S3)** - Almacenamiento de archivos
- **RabbitMQ** - Sistema de colas para procesamiento asíncrono

### Utilidades
- **Lombok** - Reducción de código boilerplate
- **MapStruct** - Mapeo entre DTOs y entidades
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

## 📂 Estructura del Proyecto

Una visión general de la estructura de directorios principal del proyecto:

```
.
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── nelumbo/
│   │   │           └── park/
│   │   │               ├── controller/       # Controladores REST
│   │   │               ├── service/          # Lógica de negocio
│   │   │               ├── repository/       # Acceso a datos
│   │   │               ├── model/            # Entidades de base de datos
│   │   │               ├── dto/              # Objetos de transferencia de datos
│   │   │               ├── config/           # Configuraciones de la aplicación
│   │   │               └── ...
│   │   └── resources/      # Archivos de configuración, estáticos, etc.
│   └── test/               # Pruebas unitarias e de integración
├── .env.example          # Ejemplo de archivo de variables de entorno
├── pom.xml                 # Archivo de configuración de Maven
├── docker-compose.yml      # Configuración de Docker Compose
└── README.md               # Este archivo
```

## 💡 Ejemplos de Uso

Aquí hay algunos ejemplos de cómo interactuar con la API una vez que la aplicación esté en funcionamiento.

### Autenticación de Usuario

**Endpoint:** `POST http://localhost:8080/auth/login`
**Body:**
```json
{
  "username": "usuario@example.com",
  "password": "password123"
}
```
**Respuesta (ejemplo):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer"
}
```

### Registrar un Vehículo (requiere autenticación)

**Endpoint:** `POST http://localhost:8080/vehicles`
**Headers:** `Authorization: Bearer <your_jwt_token>`
**Body:**
```json
{
  "licensePlate": "ABC-123",
  "type": "CAR",
  "userId": 1
}
```

### Obtener Lista de Parqueaderos

**Endpoint:** `GET http://localhost:8080/parkings`
**Headers:** `Authorization: Bearer <your_jwt_token>`

### Documentación de la API

Puedes acceder a la documentación interactiva de la API a través de Swagger UI en:
`http://localhost:8080/swagger-ui.html`

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
