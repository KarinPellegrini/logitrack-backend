# Microservicio IA — LogiTrack

Servicio independiente desarrollado en Python/Flask que expone una API REST para predicción de prioridad de envíos y cálculo de distancia entre códigos postales.

Deployado en Railway como servicio separado del backend Java.

---

## Stack

| Tecnología | Uso |
|---|---|
| Python 3.11 | Lenguaje |
| Flask | API REST |
| scikit-learn | RandomForestClassifier |
| pandas / numpy | Manipulación del dataset |
| Nominatim (OSM) | Geolocalización de CPs (con timeout 5s) |

---

## Endpoints

### `GET /health`

Healthcheck. Verifica que el servicio y el modelo estén operativos.

**Response `200 OK`:**
```json
{ "status": "ok", "modelo": "entrenado" }
```

---

### `POST /predict`

Predice la prioridad de un envío y calcula la distancia entre los CPs.

**Request body:**
```json
{
  "cp_origen": "1663",
  "cp_destino": "7000",
  "peso": 15.0,
  "tipo_envio": "Medica"
}
```

**Tipos de envío válidos:** `Estandar`, `Fragil`, `Medica`, `Peligrosa`

**Response `200 OK`:**
```json
{
  "prioridad": "ALTA",
  "distanciaKm": 385.2
}
```

**Lógica interna:**
1. Obtiene coordenadas de `cp_origen` y `cp_destino` via Nominatim (OSM)
2. Calcula distancia con fórmula Haversine
3. Codifica `tipo_envio` a entero: Estandar=0, Fragil=1, Medica=2, Peligrosa=3
4. Llama a `clf.predict([[distanciaKm, peso, tipo_num]])`
5. Devuelve prioridad en mayúsculas

**Fallback:** si Nominatim no responde en 5s, se usa distancia estimada por diferencia de CP.

---

### `POST /retrain`

Re-entrena el modelo con el dataset actual sin downtime.

**Mecanismo:** entrena un nuevo `RandomForestClassifier` en memoria y luego realiza el swap atómico `clf = nuevo_clf`. Las predicciones en curso no se interrumpen.

**Response `200 OK`:**
```json
{ "status": "modelo reentrenado correctamente" }
```

---

## Modelo

### Algoritmo

`RandomForestClassifier` de scikit-learn con parámetros por defecto.

**Features de entrada:**
| Feature | Tipo | Descripción |
|---|---|---|
| `distanciaKm` | Float | Distancia entre CP origen y destino |
| `peso` | Float | Peso del envío en kg |
| `tipo_envio` | Int (0–3) | Estandar=0, Fragil=1, Medica=2, Peligrosa=3 |

**Clases de salida:** `BAJA`, `MEDIA`, `ALTA`

### Dataset de entrenamiento

1200 registros sintéticos generados por `generar_dataset.py` con las siguientes reglas de negocio:

| Condición | Prioridad |
|---|---|
| `tipo == Peligrosa` | ALTA |
| `tipo == Médica` y (`peso > 5` ó `dist > 100`) | ALTA |
| `tipo == Médica` y `peso ≤ 5` y `dist ≤ 100` | MEDIA |
| `tipo Estándar/Frágil`, `peso > 15` y `dist > 200` | ALTA |
| `tipo Estándar/Frágil`, `peso ≥ 5` ó `dist ≥ 50` | MEDIA |
| `tipo Estándar/Frágil`, `peso < 5` y `dist < 50` | BAJA |

**Distribución aproximada:** ~30% BAJA / ~50% MEDIA / ~20% ALTA

El dataset cubre explícitamente todos los cuadrantes de combinación de features para evitar sesgos de predicción.

---

## Ejecutar localmente

```bash
cd logitrack_IA
pip install -r requirements.txt
python RandomForestIA.py
# Disponible en http://localhost:5001
```

Para regenerar el dataset:
```bash
python generar_dataset.py
# Genera datasetIA.csv con 1200 registros
```

---

## Variables de entorno

| Variable | Descripción | Default |
|---|---|---|
| `PORT` | Puerto del servicio | `5001` |

---

## Integración con el backend Java

El backend Java llama a `/predict` al registrar cada nuevo envío mediante `EnvioService.consultarIA()`.

- URL configurada via `IA_SERVICE_URL` en `application.properties`
- Timeouts: conexión 10s, request 30s
- Fallback ante fallo: prioridad `BAJA`, distancia `300.0 km`
- La URL se normaliza con `.trim()` para evitar errores por saltos de línea en Railway

---

## Docker

```dockerfile
FROM python:3.11-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY RandomForestIA.py .
COPY generar_dataset.py .
COPY datasetIA.csv .
EXPOSE 5001
CMD ["python", "RandomForestIA.py"]
```
