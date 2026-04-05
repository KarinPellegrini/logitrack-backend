# API Reference — LogiTrack Backend

Base URL local: `http://localhost:8080`
Base URL producción: `https://logitrack-backend.railway.app`

Todos los endpoints devuelven JSON. Los errores siguen el formato estándar de Spring Boot (`timestamp`, `status`, `error`, `message`).

---

## Autenticación

### `POST /api/usuarios/login`

Valida credenciales y devuelve los datos del usuario autenticado.

**Request body:**
```json
{
  "usuario": "melina",
  "contrasena": "1234"
}
```

**Response `200 OK`:**
```json
{
  "id": 1,
  "usuario": "melina",
  "rol": "OPERADOR"
}
```

**Response `401 Unauthorized`:** credenciales incorrectas.

---

## Envíos

### `POST /api/envios`

Registra un nuevo envío. Llama al microservicio IA para calcular prioridad y distancia.

**Request body:**
```json
{
  "nombre": "Laura",
  "apellido": "Gómez",
  "dni": "30123456",
  "cpOrigen": "1663",
  "cpDestino": "7000",
  "peso": 12.5,
  "tipoEnvio": "Estandar",
  "usuarioOperador": "melina"
}
```

**Validaciones:**
- `cpOrigen` y `cpDestino`: rango `1000–9499`
- `peso`: mayor a 0
- `nombre`, `apellido`, `dni`, `tipoEnvio`: obligatorios

**Response `201 Created`:** `EnvioResponseDTO` completo (ver estructura abajo).

---

### `GET /api/envios`

Devuelve todos los envíos registrados.

**Response `200 OK`:** `List<EnvioResponseDTO>`

---

### `GET /api/envios/{trackingId}`

Devuelve el detalle de un envío por su Tracking ID.

**Response `200 OK`:** `EnvioResponseDTO`
**Response `404 Not Found`:** envío no encontrado.

---

### `PUT /api/envios/{trackingId}/estado`

Cambia el estado de un envío. Registra el cambio en el historial con usuario y timestamp.

**Request body:**
```json
{
  "nuevoEstado": "EN_TRANSITO",
  "usuario": "ciro"
}
```

**Estados válidos y transiciones:**
```
REGISTRADO → EN_TRANSITO → EN_SUCURSAL → ENTREGADO
```

No se permiten saltos de estado ni retrocesos.

**Response `200 OK`:** `EnvioResponseDTO` actualizado.
**Response `400 Bad Request`:** transición inválida.

---

### `GET /api/envios/buscar?nombre={termino}`

Busca envíos por nombre, apellido o Tracking ID (búsqueda parcial, case-insensitive).

**Response `200 OK`:** `List<EnvioResponseDTO>`

---

### `GET /api/envios/por-fecha?desde={fecha}&hasta={fecha}`

Filtra envíos por rango de fechas de creación.

**Formato de fecha:** `yyyy-MM-dd` (ej: `2025-04-01`)

**Response `200 OK`:** `List<EnvioResponseDTO>`

---

### `GET /api/envios/{trackingId}/historial`

Devuelve el historial cronológico de cambios de estado de un envío.

**Response `200 OK`:**
```json
[
  {
    "id": 1,
    "estado": "REGISTRADO",
    "usuario": "melina",
    "timestamp": "2025-04-01T10:30:00"
  },
  {
    "id": 2,
    "estado": "EN_TRANSITO",
    "usuario": "ciro",
    "timestamp": "2025-04-02T14:15:00"
  }
]
```

---

### `POST /api/envios/{trackingId}/anonimizar`

Realiza el borrado lógico de datos personales del envío (Ley 25.326 — Derecho al Olvido).

Los campos `nombre`, `apellido` y `dni` se reemplazan por `[ANONIMIZADO]`. El envío permanece en el sistema para trazabilidad logística.

**Request body:**
```json
{
  "motivo": "Solicitud del titular — Derecho al Olvido",
  "solicitante": "melina"
}
```

**Response `200 OK`:** `EnvioResponseDTO` con datos anonimizados.

---

### `GET /api/envios/solicitudes-borrado`

Devuelve todos los envíos que han sido anonimizados. Solo accesible para Supervisores.

**Response `200 OK`:** `List<EnvioResponseDTO>`

---

## Dashboard y Auditoría

### `GET /api/dashboard/resumen`

Devuelve métricas agregadas del sistema.

**Response `200 OK`:**
```json
{
  "totalEnvios": 42,
  "porEstado": {
    "REGISTRADO": 10,
    "EN_TRANSITO": 15,
    "EN_SUCURSAL": 8,
    "ENTREGADO": 9
  },
  "actividadReciente": [
    {
      "usuario": "melina",
      "accion": "CAMBIO_ESTADO",
      "detalle": "TRK-001 → EN_TRANSITO",
      "timestamp": "2025-04-05T09:00:00"
    }
  ],
  "rankingUsuarios": [
    { "usuario": "melina", "totalAcciones": 28 },
    { "usuario": "ciro", "totalAcciones": 14 }
  ]
}
```

---

### `GET /api/historial/buscar?usuario={u}&accion={a}`

Busca logs de auditoría por usuario y/o acción. Ambos parámetros son opcionales.

**Response `200 OK`:** lista de registros de historial con `usuario`, `accion`, `detalle` y `timestamp`.

---

## Gestión de Usuarios

### `GET /api/usuarios`

Devuelve todos los usuarios registrados. Solo Supervisor.

### `POST /api/usuarios`

Crea un nuevo usuario.

**Request body:**
```json
{
  "usuario": "karin",
  "contrasena": "pass123",
  "rol": "OPERADOR"
}
```

### `PUT /api/usuarios/{id}`

Modifica datos o rol de un usuario existente. Solo Supervisor.

---

## Estructura de `EnvioResponseDTO`

```json
{
  "trackingId": "TRK-001",
  "nombre": "Laura",
  "apellido": "Gómez",
  "dni": "30123456",
  "cpOrigen": "1663",
  "cpDestino": "7000",
  "peso": 12.5,
  "tipoEnvio": "Estandar",
  "estado": "EN_TRANSITO",
  "prioridad": "MEDIA",
  "motivoPrioridad": "Envío estándar de peso moderado con distancia intermedia.",
  "distanciaKm": 385.2,
  "probabilidadRetraso": 42,
  "fechaCreacion": "2025-04-01T10:30:00",
  "fechaCambioEstado": "2025-04-02T14:15:00",
  "usuarioCambioEstado": "ciro",
  "anonimizado": false
}
```

| Campo | Tipo | Descripción |
|---|---|---|
| `trackingId` | `String` | Identificador único generado automáticamente |
| `prioridad` | `BAJA / MEDIA / ALTA` | Calculado por el modelo IA al registrar |
| `motivoPrioridad` | `String` | Explicación en lenguaje natural de la prioridad |
| `distanciaKm` | `Double` | Distancia entre CP origen y destino (Haversine) |
| `probabilidadRetraso` | `Integer` | Porcentaje 5–95 (0 si estado es ENTREGADO) |
| `anonimizado` | `Boolean` | `true` si se aplicó borrado lógico |
