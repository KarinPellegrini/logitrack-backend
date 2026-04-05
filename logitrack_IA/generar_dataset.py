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
    Genera n registros cubriendo explícitamente todos los cuadrantes de
    (distancia × peso × tipo) para que el modelo aprenda correctamente
    los límites de cada clase.

    BAJA  → Estándar/Frágil, peso<5, dist<50
    MEDIA → Estándar/Frágil, cualquier dist con peso<5 y dist≥50
              o peso 5-15 con cualquier dist
              o peso>15 con dist≤200  (clave: NO es ALTA si dist≤200)
              o dist>200 con peso≤15  (clave: NO es ALTA si peso≤15)
    ALTA  → Estándar/Frágil: peso>15 AND dist>200
              Médica: peso>5 OR dist>100
              Peligrosa: siempre
    """
    rng = np.random.default_rng(seed)
    cada = n // 10  # bloques de ~120 registros

    bloques = []

    # ── BAJA ──────────────────────────────────────────────────────────────
    # Estándar/Frágil, dist<50, peso<5
    bloques.append((rng.uniform(10,  49,  cada*3).round(2),
                    rng.uniform(0.1, 4.9, cada*3).round(2),
                    rng.integers(0, 2,    cada*3)))

    # ── MEDIA ─────────────────────────────────────────────────────────────
    # Caso 1: dist 50-200, peso 5-15 (caso central)
    bloques.append((rng.uniform(50,  200, cada*2).round(2),
                    rng.uniform(5,   15,  cada*2).round(2),
                    rng.integers(0, 2,    cada*2)))

    # Caso 2: dist>200 con peso≤15  ← cuadrante crítico que faltaba
    bloques.append((rng.uniform(201, 1500, cada*2).round(2),
                    rng.uniform(0.1, 15,   cada*2).round(2),
                    rng.integers(0, 2,     cada*2)))

    # Caso 3: dist<50 con peso 5-15
    bloques.append((rng.uniform(10,  49, cada).round(2),
                    rng.uniform(5,   15, cada).round(2),
                    rng.integers(0, 2,   cada)))

    # ── ALTA ──────────────────────────────────────────────────────────────
    # Estándar/Frágil: peso>15 AND dist>200
    bloques.append((rng.uniform(201, 1500, cada).round(2),
                    rng.uniform(16,  100,  cada).round(2),
                    rng.integers(0, 2,     cada)))

    # Médica y Peligrosa (dist y peso variados)
    bloques.append((rng.uniform(10,  1500, cada).round(2),
                    rng.uniform(0.1, 100,  cada).round(2),
                    rng.integers(2, 4,     cada)))   # tipo 2 o 3

    # Combinar
    distancias = np.concatenate([b[0] for b in bloques])
    pesos      = np.concatenate([b[1] for b in bloques])
    tipos      = np.concatenate([b[2] for b in bloques])

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
