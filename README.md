# LogiTrack – Backend & AI Routing System

LogiTrack es un sistema de gestión de envíos logísticos desarrollado como trabajo práctico para la materia **Laboratorio de Construcción de Software (UNGS)**.

El sistema permite registrar envíos, consultar su estado y calcular automáticamente la **prioridad logística** utilizando un modelo de **Machine Learning (Random Forest)**.

---

# Arquitectura del sistema

El proyecto está compuesto por dos servicios principales:

### Backend API

* **Java 21**
* **Spring Boot**
* **Spring Data JPA**
* **Maven**
* **Swagger**

Responsabilidades:

* CRUD de envíos
* gestión de estados logísticos
* exposición de endpoints REST
* integración con el servicio de Machine Learning

---

### Servicio de Inteligencia Artificial

* **Python**
* **Flask**
* **scikit-learn**
* **RandomForestClassifier**

Responsabilidades:

* cálculo de prioridad logística
* estimación basada en:
  * distancia
  * peso
  * tipo_envio
  * prioridad
---

# Estructura del repositorio

```
logitrack-backend
│
├ docs                # Documentación técnica
│
├ src
│ ├ main
│ │ ├ java
│ │ │ └ com.logitrack_api
│ │ │ └ com.logitrack_IA
│ │ │ │ └  datasetIA.csv       # dataset de entrenamiento IA
│ │ │ │ └  RandomForestIA.py   # servicio ML en Python
│ │ └ resources
│
│ └ test
│
│
├ README.md
├ CONTRIBUTING.md
└ pom.xml
```

---

# Estados de envío

Los envíos siguen el siguiente ciclo:

```
REGISTRADO → EN_TRANSITO → EN_SUCURSAL → ENTREGADO
```

El backend valida que **no se puedan realizar saltos inválidos de estado**.

Ejemplo inválido:

```
REGISTRADO → ENTREGADO
```

---

# Endpoints principales

## Crear envío

POST

```
/api/envios
```

Body:

```json
{
 "dni": "40123456",
 "nombre": "Juan",
 "apellido": "Perez",
 "direccion": "Av Siempre Viva 742",
 "codigoPostal": "1704",
 "peso": 2.5
}
```

---

## Obtener todos los envíos

GET

```
/api/envios
```

---

## Obtener envío por trackingId

GET

```
/api/envios/{trackingId}
```

---

## Cambiar estado del envío

PUT

```
/api/envios/{trackingId}/estado?estado=EN_TRANSITO
```

---

## Buscar envíos por nombre

GET

```
/api/envios/buscar?nombre=juan
```

---

# API Documentation

Swagger UI disponible en:

```
http://localhost:8080/swagger-ui/index.html
```

---

# Servicio de Machine Learning

El servicio IA utiliza un modelo **RandomForestClassifier** entrenado con datos logísticos.

Variables utilizadas:

* distancia entre códigos postales
* peso del paquete
* tipo de envío

Tipos de envío:

```
Estandar
Fragil
Medica
Peligrosa
```

El modelo predice la prioridad:

```
BAJA
MEDIA
ALTA
```

Endpoint del modelo:

```
POST /predict
```

Ejemplo request:

```json
{
 "cp_origen": "1704",
 "cp_destino": "2000",
 "peso": 5,
 "tipo_envio": "Fragil"
}
```

Respuesta:

```json
{
 "prioridad": "MEDIA"
}
```

---

# Ejecutar el proyecto

## 1 – Ejecutar el modelo de IA

Instalar dependencias:

```
pip install flask pandas scikit-learn requests numpy
```

Ejecutar:

```
python RandomForestIA.py
```

El servicio quedará disponible en:

```
http://localhost:5001
```

---

## 2 – Ejecutar el backend

```
mvn spring-boot:run
```

El backend correrá en:

```
http://localhost:8080
```

---

# Datos semilla

Al iniciar el backend se generan automáticamente **envíos de prueba** para facilitar el testing del sistema.

---

# Pipeline CI

El proyecto incluye un pipeline de **GitHub Actions** que ejecuta:

* build del proyecto
* ejecución de tests
* verificación del código

---


# Autores

Proyecto académico desarrollado por Ciro Martín López, Karin Pellegrini y Melina Scabini, estudiantes de **Licenciatura en Sistemas – UNGS**.
