"""
generar_dataset.py — US-30 LogiTrack
=====================================
Genera un dataset sintético de 1200 registros para entrenar el modelo
de clasificación de prioridad de envíos (RandomForest).

Campos del dataset
------------------
distancia   : float  — distancia en km entre CP origen y destino (10–1500)
peso        : float  — peso del paquete en kg (0.1–100)
tipo_envio  : int    — 0=Estandar, 1=Fragil, 2=Medica, 3=Peligrosa
prioridad   : int    — 0=BAJA, 1=MEDIA, 2=ALTA  (etiqueta, derivada por reglas)

Reglas de negocio aplicadas
----------------------------
ALTA  (2): tipo == Peligrosa (3)
           OR tipo == Medica (2) y (peso > 5 OR distancia > 100)
           OR (tipo IN [Estandar,Fragil]) y peso > 15 y distancia > 200

MEDIA (1): tipo == Medica (2) y peso <= 5 y distancia <= 100
           OR (tipo IN [Estandar,Fragil]) y (peso >= 5 OR distancia >= 50)
           y no cumple condición ALTA

BAJA  (0): (tipo IN [Estandar,Fragil]) y peso < 5 y distancia < 50

Distribución objetivo
---------------------
~30% BAJA, ~35% MEDIA, ~35% ALTA  →  balance representativo para el modelo
"""

import numpy as np
import pandas as pd
import os

SEED = 42
N    = 1200


def calcular_prioridad(distancia: float, peso: float, tipo: int) -> int:
    """Aplica las reglas de negocio y retorna 0=BAJA, 1=MEDIA, 2=ALTA."""
    if tipo == 3:                               # Peligrosa → siempre ALTA
        return 2
    if tipo == 2:                               # Médica
        return 2 if (peso > 5 or distancia > 100) else 1
    # Estándar o Frágil
    if peso > 15 and distancia > 200:
        return 2
    if peso >= 5 or distancia >= 50:
        return 1
    return 0


def generar_dataset(n: int = N, seed: int = SEED) -> pd.DataFrame:
    """
    Genera n registros sintéticos con distribución balanceada usando
    tres bloques de reglas para cubrir los tres niveles de prioridad.
    """
    rng = np.random.default_rng(seed)

    # --- Bloque BAJA (~30%): Estándar/Frágil, peso < 5, distancia < 50 ---
    n_baja = int(n * 0.30)
    dist_b = rng.uniform(10, 49,  n_baja).round(2)
    peso_b = rng.uniform(0.1, 4.9, n_baja).round(2)
    tipo_b = rng.integers(0, 2,   n_baja)          # 0 o 1

    # --- Bloque MEDIA (~35%): Estándar/Frágil, rangos medios ---
    n_media = int(n * 0.35)
    dist_m = rng.uniform(50, 200,  n_media).round(2)
    peso_m = rng.uniform(5,  15,   n_media).round(2)
    tipo_m = rng.integers(0, 2,    n_media)

    # --- Bloque ALTA (~35%): cargas pesadas/largas + Médica/Peligrosa ---
    n_alta = n - n_baja - n_media
    # Mitad: Estándar/Frágil con peso>15 y distancia>200
    n_alt_std = n_alta // 2
    dist_a1 = rng.uniform(201, 1500, n_alt_std).round(2)
    peso_a1 = rng.uniform(16,  100,  n_alt_std).round(2)
    tipo_a1 = rng.integers(0, 2, n_alt_std)
    # Mitad: Médica (2) y Peligrosa (3)
    n_alt_esp = n_alta - n_alt_std
    dist_a2 = rng.uniform(10,  1500, n_alt_esp).round(2)
    peso_a2 = rng.uniform(0.1, 100,  n_alt_esp).round(2)
    tipo_a2 = rng.integers(2, 4, n_alt_esp)    # 2 o 3

    # Combinar todos los bloques
    distancias = np.concatenate([dist_b, dist_m, dist_a1, dist_a2])
    pesos      = np.concatenate([peso_b, peso_m, peso_a1, peso_a2])
    tipos      = np.concatenate([tipo_b, tipo_m, tipo_a1, tipo_a2])

    prioridades = np.array([
        calcular_prioridad(d, p, t)
        for d, p, t in zip(distancias, pesos, tipos)
    ])

    df = pd.DataFrame({
        'distancia':  distancias,
        'peso':       pesos,
        'tipo_envio': tipos,
        'prioridad':  prioridades,
    })

    # Mezclar filas para que el modelo no aprenda el orden de los bloques
    df = df.sample(frac=1, random_state=seed).reset_index(drop=True)
    return df


if __name__ == '__main__':
    df = generar_dataset()

    print(f"Total registros : {len(df)}")
    print(f"Distribución prioridad:\n{df['prioridad'].value_counts().sort_index()}")
    print(f"\nPrimeras filas:\n{df.head()}")

    out_path = os.path.join(os.path.dirname(__file__), 'datasetIA.csv')
    df.to_csv(out_path, index=False)
    print(f"\nDataset guardado en: {out_path}")
