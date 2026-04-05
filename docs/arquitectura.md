# Arquitectura del Sistema — LogiTrack

---

## Visión general

LogiTrack está compuesto por tres servicios independientes que se comunican via HTTP:

```
┌─────────────────────────────────────────────────────────────┐
│                        CLIENTE                              │
│           React 19 + Vite  —  Vercel                        │
└─────────────────────┬───────────────────────────────────────┘
                      │ HTTP / JSON
                      ▼
┌─────────────────────────────────────────────────────────────┐
│                   BACKEND API                               │
│           Java 21 + Spring Boot 3.4  —  Railway             │
│                                                             │
│  controller → service → repository → model                  │
└──────────┬──────────────────────────┬───────────────────────┘
           │ HTTP / JSON              │ JPA / SQL
           ▼                          ▼
┌──────────────────────┐   ┌──────────────────────────────────┐
│   MICROSERVICIO IA   │   │          BASE DE DATOS           │
│ Python 3.11 + Flask  │   │   PostgreSQL  —  Supabase        │
│       Railway        │   └──────────────────────────────────┘
│                      │
│  POST /predict       │
│  POST /retrain       │
│  GET  /health        │
└──────────────────────┘
```

---

## Responsabilidades por servicio

### Frontend — React / Vercel

- Autenticación y routing de vistas por rol (Operador / Supervisor)
- Gestión de envíos: lista, búsqueda con debounce, filtro por fechas, alta, detalle
- Visualización de análisis IA: semáforo de prioridad, motivo, distancia, barra de probabilidad de retraso
- Historial de estados por envío (timeline)
- Dashboard de auditoría y buscador de logs
- Modal ARCO para ejercer derechos de la Ley 25.326
- Panel de solicitudes de borrado (Supervisor)
- Gestión de usuarios: alta, modificación, cambio de rol (Supervisor)

### Backend API — Spring Boot / Railway

- CRUD de envíos con validación de CP (rango 1000–9499)
- Ciclo de vida de estados con transiciones validadas: `REGISTRADO → EN_TRANSITO → EN_SUCURSAL → ENTREGADO`
- Registro de historial de cambios con usuario y timestamp (`HistorialEstado`)
- Integración HTTP con el microservicio IA al crear cada envío
- Cálculo de probabilidad de retraso por reglas de negocio
- Borrado lógico de datos personales (Ley 25.326)
- Dashboard de auditoría con métricas agregadas
- ABM de usuarios con roles (OPERADOR / SUPERVISOR)

### Microservicio IA — Flask / Railway

- Predicción de prioridad (`BAJA / MEDIA / ALTA`) mediante RandomForestClassifier
- Cálculo de distancia entre CPs usando Haversine + Nominatim (OSM)
- Entrenamiento con dataset sintético de 1200 registros
- Re-entrenamiento en caliente sin downtime (`POST /retrain`)

### Base de datos — PostgreSQL / Supabase

- Tablas principales: `envio`, `usuario`, `historial_estado`
- Índices en `trackingId`, `nombre`, `apellido` para optimizar búsquedas
- Schema gestionado por Hibernate con `ddl-auto=update`

---

## Flujo de creación de un envío

```
Frontend
  │
  ├─ POST /api/envios ──────────────────► Backend
  │                                          │
  │                                          ├─ Valida campos (CP, peso, tipo)
  │                                          │
  │                                          ├─ POST /predict ──► Microservicio IA
  │                                          │                        │
  │                                          │◄── { prioridad,        │
  │                                          │     distanciaKm }  ◄───┘
  │                                          │
  │                                          ├─ Calcula probabilidadRetraso
  │                                          ├─ Persiste Envio + HistorialEstado
  │                                          │
  │◄── EnvioResponseDTO ◄───────────────────┘
  │
  └─ Muestra semáforo, distancia, barra de retraso
```

---

## Capas del Backend (Java)

```
com.logitrack.logitrack_api/
├── controller/    ← Recibe HTTP, delega al service, devuelve DTO
├── service/       ← Lógica de negocio, integración con IA
├── repository/    ← Acceso a BD con Spring Data JPA
├── dto/           ← EnvioRequestDTO, EnvioResponseDTO
├── model/         ← Envio, Usuario, HistorialEstado, EstadoEnvio
└── config/        ← DatosSemillas, CorsConfig
```

Regla: los controllers nunca exponen entidades JPA directamente. Todo sale como DTO.

---

## Variables de entorno por servicio

### Backend (Railway)

| Variable | Descripción |
|---|---|
| `DATABASE_URL` | URL de conexión a Supabase |
| `DATABASE_USER` | Usuario de la base de datos |
| `DATABASE_PASSWORD` | Contraseña de la base de datos |
| `IA_SERVICE_URL` | URL del microservicio IA |
| `PORT` | Puerto dinámico asignado por Railway |

### Microservicio IA (Railway)

| Variable | Descripción |
|---|---|
| `PORT` | Puerto del servicio Flask |

### Frontend (Vercel)

| Variable | Descripción |
|---|---|
| `VITE_API_URL` | URL base del backend |

---

## Decisiones de arquitectura relevantes

Ver `docs/semana2/02-adrs.md` en el repositorio del frontend para el detalle completo de las ADRs.

Resumen de decisiones clave:
- **Microservicio IA separado**: permite re-entrenar el modelo sin afectar el backend Java
- **DTOs en todos los endpoints**: nunca se exponen entidades JPA directamente
- **Fallback de IA**: ante timeout o error, el backend responde con valores por defecto (`prioridad: BAJA`, `distanciaKm: 300.0`) para no bloquear el registro del envío
- **Borrado lógico**: los envíos anonimizados permanecen en la base con datos reemplazados por `[ANONIMIZADO]`, preservando la trazabilidad logística
