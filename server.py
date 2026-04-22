import json
from datetime import datetime
import paho.mqtt.client as mqtt

BROKER = "broker.hivemq.com"
PORT = 1883
TOPIC = "ufscar/automacao/gps/+"   # '+' captura qualquer device_id


def on_connect(client, userdata, flags, rc, properties=None):
    if rc == 0:
        print(f"[{ts()}] Conectado ao broker {BROKER}:{PORT}")
        client.subscribe(TOPIC, qos=1)
        print(f"[{ts()}] Inscrito em: {TOPIC}\n")
    else:
        print(f"[{ts()}] Falha na conexão, código {rc}")


def on_message(client, userdata, msg):
    try:
        data = json.loads(msg.payload.decode())
    except json.JSONDecodeError:
        print(f"[{ts()}] Payload inválido em {msg.topic}: {msg.payload}")
        return

    device = data.get("device_id", "?")
    lat = data.get("latitude")
    lon = data.get("longitude")
    acc = data.get("accuracy")
    alt = data.get("altitude")

    print(
        f"[{ts()}] {device:<12} | "
        f"lat={lat:>10.6f} | lon={lon:>11.6f} | "
        f"acc={acc:>6.1f} m | alt={alt if alt is None else f'{alt:.1f}'} m"
    )


def ts():
    return datetime.now().strftime("%H:%M:%S")


def main():
    client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2)
    client.on_connect = on_connect
    client.on_message = on_message

    print(f"[{ts()}] Conectando em {BROKER}:{PORT}...")
    client.connect(BROKER, PORT, keepalive=60)

    try:
        client.loop_forever()
    except KeyboardInterrupt:
        print(f"\n[{ts()}] Encerrando...")
        client.disconnect()


if __name__ == "__main__":
    main()