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

## 🔧 Configuración del Entorno

### 1. Variables de Entorno

Crea un archivo `.env` en la raíz del proyecto con las siguientes variables:

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

# Genera una clave secreta segura (recomendado: 256 bits)
APP_SECURITY_JWT_SECRET=clave_muy_segura_de_al_menos_32_caracteres

# Docker
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management

# O instalación nativa según tu sistema operativo
RABBITMQ_URL=amqp://guest:guest@localhost:5672/

# O para producción:
RABBITMQ_URL=amqp://usuario:contraseña@tu-servidor:5672/

# O para local:
RABBITMQ_URL=amqp://guest:guest@localhost:5672/

# O para producción:
RABBITMQ_URL=amqp://usuario:contraseña@tu-servidor:5672/