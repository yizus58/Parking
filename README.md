# 🚗 Sistema de Gestión de Parqueaderos - Api Park

Una aplicación completa de gestión de parqueaderos desarrollada con Spring Boot que permite administrar usuarios, vehículos, espacios de parqueo y generar reportes con integración a servicios de nube.

## 🚀 Características Principales

- **Gestión de Usuarios**: Registro, autenticación y autorización con JWT
- **Gestión de Parqueaderos**: CRUD completo de espacios de parqueo
- **Gestión de Vehículos**: Administración de vehículos y sus propietarios
- **Sistema de Rankings**: Rankings de parqueaderos y socios más utilizados
- **Indicadores y Reportes**: Generación de indicadores de negocio
- **Procesamiento de Archivos**: Generación de reportes Excel automáticos
- **Integración AWS S3**: Almacenamiento seguro de archivos en la nube
- **Sistema de Colas**: Procesamiento asíncrono de tareas con RabbitMQ
- **Notificaciones por Email**: Envío automático de reportes por correo

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
- **Amazon S3** - Almacenamiento de archivos
- **RabbitMQ** - Sistema de colas para procesamiento asíncrono

### Utilidades
- **Lombok** - Reducción de código boilerplate
- **MapStruct** - Mapeo entre DTOs y entidades
- **Apache POI** - Generación de archivos Excel
- **Jackson** - Serialización/deserialización JSON
- **Maven** - Gestión de dependencias

## 📋 Prerrequisitos

- Java 21 o superior
- Maven 3.6+
- Docker y Docker Compose
- Cuenta de AWS con acceso a S3
- Servidor RabbitMQ

## 🔧 Configuración del Entorno

### 1. Variables de Entorno

Crea un archivo `.env` en la raíz del proyecto con las siguientes variables:
