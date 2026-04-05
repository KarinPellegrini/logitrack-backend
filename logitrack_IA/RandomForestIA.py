from flask import Flask, request, jsonify
import pandas as pd
from sklearn.ensemble import RandomForestClassifier
import requests
import time
import numpy as np
import os

# --- Importamos el generador de dataset sintético (US-30) ---
from generar_dataset import generar_dataset

app = Flask(__name__)

# ============================================================
# PARTE 1: ENTRENAMIENTO INICIAL CON DATASET SINTÉTICO (US-28)
# ============================================================

DATASET_PATH = os.path.join(os.path.dirname(__file__), 'datasetIA.csv')

def cargar_o_generar_dataset() -> pd.DataFrame:
    """
    Carga el dataset desde CSV si existe, o lo genera en tiempo de ejecución.
    Esto permite re-entrenar sin afectar el entorno de producción.
    """
    if os.path.exists(DATASET_PATH):
        print(f"📂 Dataset cargado desde: {DATASET_PATH}")
        return pd.read_csv(DATASET_PATH)
    print("⚙️  Dataset no encontrado — generando dataset sintético...")
    df = generar_dataset()
    df.to_csv(DATASET_PATH, index=False)
    print(f"✅ Dataset generado y guardado en: {DATASET_PATH}")
    return df

def entrenar_modelo(df: pd.DataFrame) -> RandomForestClassifier:
    """Entrena y retorna un RandomForestClassifier con el dataset recibido."""
    modelo = RandomForestClassifier(n_estimators=100, random_state=42)
    modelo.fit(df[['distancia', 'peso', 'tipo_envio']], df['prioridad'])
    print(f"🌲 Modelo entrenado con {len(df)} registros.")
    return modelo

# Entrenamiento en el arranque del servidor
df_inicial = cargar_o_generar_dataset()
clf = entrenar_modelo(df_inicial)


# ============================================================
# PARTE 2: FUNCIONES DE GEOLOCALIZACIÓN
# ============================================================

def obtener_coordenadas(cp):
    """Consulta la latitud y longitud de un CP en Argentina."""
    url = "https://nominatim.openstreetmap.org/search"
    params = {'postalcode': cp, 'country': 'Argentina', 'format': 'json', 'limit': 1}
    headers = {'User-Agent': 'LogiTrack_App_UNGS'}
    try:
        response = requests.get(url, params=params, headers=headers)
        data = response.json()
        if data:
            lat = float(data[0]['lat'])
            lon = float(data[0]['lon'])
            print(f"📍 CP: {cp} -> Lat: {lat}, Lon: {lon}")
            return lat, lon
    except Exception as e:
        print(f"Error consultando coordenadas para {cp}: {e}")
    return None

def obtener_distancia(cp_orig, cp_dest):
    """Calcula la distancia en KM entre dos códigos postales."""
    coords1 = obtener_coordenadas(cp_orig)
    time.sleep(1)
    coords2 = obtener_coordenadas(cp_dest)

    if coords1 is None or coords2 is None:
        print(f"Advertencia: No se pudieron geolocalizar los CPs {cp_orig} o {cp_dest}. Usando distancia base.")
        return 300.0

    lat1, lon1 = coords1
    lat2, lon2 = coords2
    R = 6371.0
    dlat = np.radians(lat2 - lat1)
    dlon = np.radians(lon2 - lon1)
    a = (np.sin(dlat / 2) ** 2
         + np.cos(np.radians(lat1)) * np.cos(np.radians(lat2)) * np.sin(dlon / 2) ** 2)
    c = 2 * np.arctan2(np.sqrt(a), np.sqrt(1 - a))
    return R * c


# ============================================================
# PARTE 3: ENDPOINTS
# ============================================================

@app.route('/health', methods=['GET'])
def health():
    return jsonify({'status': 'ok'})


@app.route('/predict', methods=['POST'])
def predict():
    """Predice la prioridad de un envío dado CP origen, destino, peso y tipo."""
    global clf
    datos_recibidos = request.get_json()

    cp_orig  = datos_recibidos.get('cp_origen')
    cp_dest  = datos_recibidos.get('cp_destino')
    peso     = datos_recibidos.get('peso')
    tipo_str = datos_recibidos.get('tipo_envio', 'Estandar')

    mapeo    = {"Estandar": 0, "Fragil": 1, "Medica": 2, "Peligrosa": 3}
    tipo_num = mapeo.get(tipo_str, 0)

    dist = obtener_distancia(cp_orig, cp_dest)
    pred = clf.predict([[dist, peso, tipo_num]])

    categorias = {0: "BAJA", 1: "MEDIA", 2: "ALTA"}
    return jsonify({
        'prioridad':   categorias[int(pred[0])],
        'distanciaKm': round(float(dist), 2)
    })


@app.route('/retrain', methods=['POST'])
def retrain():
    """
    Re-entrena el modelo con el dataset existente (o con nuevos datos adicionales).
    El modelo anterior sigue activo hasta que el nuevo esté listo → sin downtime.
    Criterio US-28: puede re-entrenarse sin afectar el entorno de producción.

    Body JSON opcional:
    {
      "nuevos_registros": [
        {"distancia": 120, "peso": 8.5, "tipo_envio": 1},
        ...
      ]
    }
    """
    global clf

    body = request.get_json(silent=True) or {}
    nuevos_registros = body.get('nuevos_registros')

    df_base = cargar_o_generar_dataset()

    if nuevos_registros:
        df_nuevos = pd.DataFrame(nuevos_registros)
        # Calcular etiqueta con las mismas reglas de negocio
        from generar_dataset import calcular_prioridad
        if 'prioridad' not in df_nuevos.columns:
            df_nuevos['prioridad'] = df_nuevos.apply(
                lambda r: calcular_prioridad(r['distancia'], r['peso'], r['tipo_envio']),
                axis=1
            )
        df_combinado = pd.concat([df_base, df_nuevos], ignore_index=True)
        df_combinado.to_csv(DATASET_PATH, index=False)
        print(f"➕ {len(df_nuevos)} nuevos registros incorporados al dataset.")
    else:
        df_combinado = df_base

    # Entrenar nuevo modelo sin interrumpir el actual
    nuevo_clf = entrenar_modelo(df_combinado)

    # Swap atómico: /predict ya usa el nuevo modelo
    clf = nuevo_clf

    return jsonify({
        'status':           'ok',
        'registros_usados': len(df_combinado),
        'mensaje':          'Modelo re-entrenado exitosamente sin interrumpir el servicio.'
    })


if __name__ == '__main__':
    app.run(port=5001, debug=True)
