import asyncio
from asyncua import Server

async def main():
    server = Server()
    await server.init()

    # endereço que o servidor vai escutar
    server.set_endpoint("opc.tcp://0.0.0.0:4840/gps")

    # namespace do projeto
    url = "http://projeto.gps.opcua"
    idx = await server.register_namespace(url)

    # criando os nós de GPS
    objects = server.nodes.objects
    gps = await objects.add_object(idx, "GPS")

    latitude = await gps.add_variable(idx, "Latitude", 0.0)
    longitude = await gps.add_variable(idx, "Longitude", 0.0)
    timestamp = await gps.add_variable(idx, "Timestamp", "")

    # Permitindo que clientes escrevam nos nós
    await latitude.set_writable()
    await longitude.set_writable()
    await timestamp.set_writable()

    print("Servidor OPC UA rodando em opc.tcp://localhost:4840/gps")

    async with server:
        while True:
            await asyncio.sleep(1)

if __name__ == "__main__":
    asyncio.run(main())

