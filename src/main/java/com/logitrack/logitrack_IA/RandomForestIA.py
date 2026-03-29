from flask import Flask, request, jsonify
import pandas as pd
from sklearn.ensemble import RandomForestClassifier
import requests
import time
import numpy as np

#install python 
#pip install flask pandas scikit-learn requests numpy
#py "C:\Users\bue-melinas\Downloads\2026 S1 - LCS\TP LCS\TP1 LogiTrack\logitrack-backend\src\main\java\com\logitrack\logitrack_IA\RandomForestIA.py"

app = Flask(__name__)

# --- PARTE 1: ENTRENAMIENTO (Se ejecuta una vez al prender el server) ---
data = {
    'distancia': [10, 500, 1200, 5, 300, 800, 50, 1000],
    'peso': [1.5, 50.0, 600.0, 0.5, 10.0, 200.0, 20.0, 5.0],
    'tipo_envio': [0, 1, 3, 0, 2, 3, 1, 2], 
    'prioridad': [0, 1, 2, 0, 1, 2, 1, 2]
}
df = pd.DataFrame(data)
clf = RandomForestClassifier(n_estimators=100, random_state=42)
clf.fit(df[['distancia', 'peso', 'tipo_envio']], df['prioridad'])

def obtener_coordenadas(cp):
    """Consulta la latitud y longitud de un CP en Argentina"""
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
            return float(data[0]['lat']), float(data[0]['lon'])
        
    except Exception as e:
        print(f"Error consultando coordenadas para {cp}: {e}")
    return None

def obtener_distancia(cp_orig, cp_dest):
    """Calcula la distancia en KM entre dos códigos postales"""
    coords1 = obtener_coordenadas(cp_orig)
    # Esperamos 1 segundo para no saturar la API gratuita de Nominatim
    time.sleep(1) 
    coords2 = obtener_coordenadas(cp_dest)
    
    # Fallback: si la API falla o no encuentra el CP, devolvemos una distancia base
    if coords1 is None or coords2 is None:
        print(f"Advertencia: No se pudieron geolocalizar los CPs {cp_orig} o {cp_dest}. Usando distancia base.")
        return 300.0 

    # Fórmula de Haversine para calcular distancia sobre la esfera terrestre
    lat1, lon1 = coords1
    lat2, lon2 = coords2
    R = 6371.0 # Radio de la Tierra en KM
    
    dlat = np.radians(lat2 - lat1)
    dlon = np.radians(lon2 - lon1)
    a = np.sin(dlat / 2)**2 + np.cos(np.radians(lat1)) * np.cos(np.radians(lat2)) * np.sin(dlon / 2)**2
    c = 2 * np.arctan2(np.sqrt(a), np.sqrt(1 - a))
    
    return R * c

# --- PARTE 3: EL ENDPOINT QUE LLAMA JAVA ---
@app.route('/predict', methods=['POST'])
def predict():
    datos_recibidos = request.get_json()
    
    cp_orig = datos_recibidos.get('cp_origen')
    cp_dest = datos_recibidos.get('cp_destino')
    peso = datos_recibidos.get('peso')
    tipo_str = datos_recibidos.get('tipo_envio', 'Estándar')
    
    # Mapeo según tu imagen b58244.png
    mapeo = {"Estandar": 0, "Fragil": 1, "Medica": 2, "Peligrosa": 3}
    tipo_num = mapeo.get(tipo_str, 0)
    
    dist = obtener_distancia(cp_orig, cp_dest)
    pred = clf.predict([[dist, peso, tipo_num]])
    
    categorias = {0: "BAJA", 1: "MEDIA", 2: "ALTA"}
    return jsonify({'prioridad': categorias[pred[0]]})

if __name__ == '__main__':
    # Importante: Puerto 5001 para que coincida con tu código Java
    app.run(port=5001, debug=True)