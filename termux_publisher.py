import json
import time
import subprocess
import socket
import paho.mqtt.client as mqtt

BROKER = "broker.hivemq.com"   # broker público para testes
PORT = 1883
DEVICE_ID = socket.gethostname()  # ou defina manualmente: "celular_01"
TOPIC = f"automacao/ufscar/gps/{DEVICE_ID}"
INTERVAL = 5  # segundos

client = mqtt.Client(client_id=DEVICE_ID)
client.connect(BROKER, PORT, 60)
client.loop_start()

def get_location():
    result = subprocess.run(
        ["termux-location", "-p", "gps"],
        capture_output=True, text=True, timeout=30
    )
    return json.loads(result.stdout) if result.stdout else None

while True:
    try:
        loc = get_location()
        if loc:
            payload = {
                "device_id": DEVICE_ID,
                "timestamp": time.time(),
                "latitude": loc.get("latitude"),
                "longitude": loc.get("longitude"),
                "accuracy": loc.get("accuracy"),
                "altitude": loc.get("altitude"),
            }
            client.publish(TOPIC, json.dumps(payload), qos=1)
            print(f"Publicado: {payload}")
    except Exception as e:
        print(f"Erro: {e}")
    time.sleep(INTERVAL)