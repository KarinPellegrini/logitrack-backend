# Contributing

Gracias por querer contribuir a **LogiTrack** 🚚📦
Este proyecto sigue ciertas reglas para mantener el código limpio, organizado y fácil de mantener.

---

# Convención de commits

Se utiliza **Conventional Commits**.

Formato:

```
tipo(scope): descripcion
```

Ejemplos:

```
feat(envios): CRUD de envíos
feat(api): documentación swagger
feat(ia): integración con modelo RandomForest
fix(envios): validación de estado
docs: actualización README
refactor(service): mejora lógica de cálculo de prioridad
```

Tipos de commit más usados:

| Tipo     | Uso                                        |
| -------- | ------------------------------------------ |
| feat     | Nueva funcionalidad                        |
| fix      | Corrección de bug                          |
| docs     | Cambios en documentación                   |
| refactor | Mejora de código sin cambiar funcionalidad |
| test     | Agregar o modificar tests                  |
| chore    | Cambios de configuración o mantenimiento   |

---

# Estrategia de ramas

Se utiliza una estrategia similar a **GitFlow simplificado**.

```
main
│
develop
│
feature/*
```

### Ramas principales

**main**

* Rama de producción
* Solo se actualiza mediante Pull Requests aprobados

**develop**

* Rama de integración
* Aquí se mergean todas las features antes de producción

---

### Ramas de desarrollo

Las nuevas funcionalidades se crean desde `develop`.

Formato:

```
feature/nombre-feature
```

Ejemplos:

```
feature/crud-envios
feature/swagger-docs
feature/ia-prioridad-envios
```

Para correcciones:

```
fix/nombre-bug
```

Ejemplo:

```
fix/validacion-peso-envio
```

---

# Flujo de trabajo

1️⃣ Clonar repositorio

```
git clone https://github.com/usuario/logitrack.git
```

2️⃣ Crear rama desde develop

```
git checkout develop
git pull origin develop
git checkout -b feature/nueva-feature
```

3️⃣ Realizar cambios y commits

```
git add .
git commit -m "feat(envios): agregar endpoint de creación"
```

4️⃣ Subir la rama

```
git push origin feature/nueva-feature
```

5️⃣ Crear **Pull Request hacia `develop`**

---

# Reglas para Pull Requests

Todo PR debe:

✔ Explicar qué problema soluciona
✔ Describir qué cambios introduce
✔ Tener commits claros
✔ No romper funcionalidades existentes

---

# Estilo de código

Backend Java sigue convenciones:

* **Java 17**
* **Spring Boot**
* Arquitectura por capas:

```
controller
service
repository
dto
model
```

Buenas prácticas:

✔ Uso de **DTOs**
✔ Validaciones con **Bean Validation**
✔ Documentación con **Swagger**
✔ Separación clara de responsabilidades

---

# Testing

* **tests unitarios**
* Validación de endpoints del CRUD
* Integración con la IA

---

# Datos de prueba

El proyecto incluye **datos semillas** para facilitar pruebas del CRUD.

Los datos se cargan automáticamente al iniciar la aplicación.

---

# IA de priorización de envíos

El sistema incluye un microservicio en **Python** que calcula la prioridad de envío usando **RandomForest**.

Se comunica con el backend mediante HTTP:

```
POST /predict
```

Entrada:

```
{
  "cp_origen": "1617",
  "cp_destino": "1000",
  "peso": 10,
  "tipo_envio": "Fragil"
}
```

Respuesta:

```
{
  "prioridad": "MEDIA"
}
```

---

# Buenas prácticas

✔ Mantener commits pequeños
✔ Documentar endpoints nuevos
✔ No subir credenciales ni configuraciones sensibles
✔ Mantener consistencia con el estilo existente

---

# Contacto

Proyecto desarrollado para **TP – Laboratorio de Construcción de Software (UNGS)**.
