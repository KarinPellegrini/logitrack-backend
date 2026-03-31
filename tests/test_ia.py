import unittest
import json
import sys
import os

sys.path.append(
    os.path.abspath(
        os.path.join(os.path.dirname(__file__), "../src/main/java/com/logitrack/logitrack_IA")
    )
)

from RandomForestIA import app

class TestIA(unittest.TestCase):

    def setUp(self):
        self.client = app.test_client()

    def test_predict_endpoint_responde(self):
        """Test: el endpoint /predict responde correctamente"""

        data = {
            "cp_origen": "1617",
            "cp_destino": "1000",
            "peso": 10,
            "tipo_envio": "Fragil"
        }

        response = self.client.post(
            "/predict",
            data=json.dumps(data),
            content_type="application/json"
        )

        self.assertEqual(response.status_code, 200)

    def test_respuesta_contiene_prioridad(self):
        data = {
            "cp_origen": "1617",
            "cp_destino": "1000",
            "peso": 10,
            "tipo_envio": "Fragil"
        }

        response = self.client.post("/predict", json=data)

        resultado = response.get_json()

        self.assertIn("prioridad", resultado)
    def test_peso_alto_prioridad(self):
        data = {
            "cp_origen": "1617",
            "cp_destino": "1000",
            "peso": 600,
            "tipo_envio": "Peligrosa"
        }

        response = self.client.post("/predict", json=data)

        resultado = response.get_json()

        self.assertEqual(response.status_code, 200)
        self.assertIn(resultado["prioridad"], ["MEDIA", "ALTA"])


    def test_prioridad_valida(self):
            """Test: la IA devuelve BAJA, MEDIA o ALTA"""

            data = {
                "cp_origen": "1617",
                "cp_destino": "1000",
                "peso": 20,
                "tipo_envio": "Estandar"
            }

            response = self.client.post(
                "/predict",
                data=json.dumps(data),
                content_type="application/json"
            )

            resultado = json.loads(response.data)

            self.assertIn(resultado["prioridad"], ["BAJA", "MEDIA", "ALTA"])

    def test_cp_invalido(self):
            """Test: fallback si el CP no existe"""

            data = {
                "cp_origen": "0000",
                "cp_destino": "9999",
                "peso": 5,
                "tipo_envio": "Fragil"
            }

            response = self.client.post(
                "/predict",
                data=json.dumps(data),
                content_type="application/json"
            )

            self.assertEqual(response.status_code, 200)


    def test_modelo_entrenado(self):
        from RandomForestIA import clf
        self.assertIsNotNone(clf)

if __name__ == "__main__":
    unittest.main()