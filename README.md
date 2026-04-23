# Sistema de Rastreamento GPS via MQTT

Projeto da disciplina de **Sistemas de Integração e Automação Industrial**.

Implementa um sistema distribuído que coleta a localização GPS de múltiplos celulares, publica os dados em um broker MQTT (HiveMQ) e disponibiliza a visualização em um servidor. O sistema suporta celulares Android (via Termux) e iPhones (via PWA com MQTT over WebSockets), permitindo análise do erro do GPS entre dispositivos heterogêneos.

---

## Arquitetura

```
[Android + Termux] ──TCP:1883────┐
                                 │
[iPhone + PWA] ──WSS:8884────────┼──► [Broker HiveMQ] ──TCP:1883──► [server.py]
                                 │
[Outros clientes MQTT] ──────────┘
```

- **Publishers:** celulares coletam latitude, longitude, precisão e altitude e publicam periodicamente em um tópico MQTT.
- **Broker:** HiveMQ público (`broker.hivemq.com`) roteia as mensagens entre publishers e subscribers.
- **Subscriber:** script Python que se inscreve nos tópicos e imprime as localizações no terminal.

**Tópico padrão:** `automacao/ufscar/gps/<device_id>`

**Payload (JSON):**
```json
{
  "device_id": "iphone_01",
  "timestamp": 1729512345.678,
  "latitude": -22.007845,
  "longitude": -47.890123,
  "accuracy": 10.5,
  "altitude": 825.3
}
```

---

## Estrutura do Projeto

```
.
├── README.md              # este arquivo
├── server.py              # subscriber que imprime no terminal
├── termux_publisher.py       # publisher para Android via Termux
└── index.html             # publisher PWA para iPhone (ou qualquer navegador)
```

---

## Pré-requisitos

- **Python 3.8+** no computador que vai rodar o servidor.
- **Celular Android** com o app **Termux** (via F-Droid) e **Termux:API** — para a versão Termux.
- **Celular com navegador moderno** (Safari no iPhone, Chrome no Android) — para a versão PWA.
- **Conexão com a internet** em todos os dispositivos (o broker é público, na nuvem).

---

## 1. Servidor (server.py)

O servidor se inscreve no tópico `automacao/ufscar/gps/+` e imprime toda mensagem recebida, independente de qual celular publicou.

### Instalação

```bash
pip install paho-mqtt
```

### Execução

```bash
python server.py
```

### Saída esperada

```
[14:32:05] Conectando em broker.hivemq.com:1883...
[14:32:06] Conectado ao broker broker.hivemq.com:1883
[14:32:06] Inscrito em: automacao/ufscar/gps/+

[14:32:10] iphone_01    | lat=-22.007845 | lon=-47.890123 | acc=  10.5 m | alt=825.3 m
[14:32:15] iphone_01    | lat=-22.007847 | lon=-47.890121 | acc=   9.8 m | alt=825.1 m
[14:32:15] android_01   | lat=-22.007850 | lon=-47.890118 | acc=  12.3 m | alt=824.9 m
```

Pressione `Ctrl+C` para encerrar.

---

## 2. Publisher Android (gps_publisher.py via Termux)

### 2.1. Preparar o Termux

1. Instale o **Termux** pelo [F-Droid](https://f-droid.org/packages/com.termux/). **Não use a versão da Play Store** — está desatualizada e quebrada.
2. Instale também o **Termux:API** pelo mesmo F-Droid.
3. Abra o Termux e instale os pacotes necessários:
   ```bash
   pkg update && pkg upgrade
   pkg install python termux-api
   pip install paho-mqtt
   ```
4. Conceda permissão de localização ao Termux:API nas configurações do Android (`Configurações > Apps > Termux:API > Permissões > Localização > Permitir sempre`).
5. Teste o acesso ao GPS:
   ```bash
   termux-location -p gps
   ```
   Deve retornar um JSON com latitude, longitude, etc.

### 2.2. Copiar o script para o Termux

Maneira mais simples — colar o conteúdo diretamente:
```bash
nano gps_publisher.py
# (cole o conteúdo, Ctrl+O para salvar, Ctrl+X para sair)
```

Ou, se tiver o arquivo no armazenamento do celular:
```bash
termux-setup-storage
cp ~/storage/downloads/gps_publisher.py ~/
```

### 2.3. Definir o ID do dispositivo

Abra o script e ajuste a variável `DEVICE_ID` para algo único, ex: `android_01`, `android_02`, etc. Cada celular precisa de um ID diferente para que o servidor consiga distingui-los.

### 2.4. Executar

```bash
python termux_publisher.py
```

Saída esperada no celular:
```
Publicado: {'device_id': 'android_01', 'timestamp': 1729512345.6, 'latitude': -22.00784, ...}
Publicado: {'device_id': 'android_01', 'timestamp': 1729512350.6, 'latitude': -22.00785, ...}
```

Para parar, `Ctrl+C`. Para manter rodando com a tela bloqueada, use `termux-wake-lock` antes de iniciar.

---

## 3. Publisher iPhone (index.html — PWA)

O iPhone não suporta Termux. A solução é uma página web que usa a Geolocation API do navegador e publica via **MQTT over WebSockets**.

### 3.1. Por que precisa de HTTPS

A Geolocation API só funciona em páginas servidas via **HTTPS** (exceto `localhost`). Abrir o `index.html` com duplo clique, ou servir via HTTP comum, não funciona no iPhone. É necessário hospedar o arquivo em um servidor com TLS.

### 3.2. Opção Escolhida — GitHub Pages 

1. Crie um repositório público no GitHub e faça upload do `index.html`.
2. Vá em **Settings > Pages**.
3. Em **Source**, selecione a branch `main` e a pasta `/ (root)`. Salve.
4. Após 1–2 minutos, o GitHub fornece uma URL HTTPS.
5. Abra essa URL (https://ivanmartinotto.github.io/Mobile-GPS-Tracker/) no Safari do iPhone (ou de um android mesmo).

<<<<<<< HEAD
### 3.3. Usar no iPhone

1. Abra a URL HTTPS (https://ivanmartinotto.github.io/Mobile-GPS-Tracker/) no Safari.
=======
### 3.4. Usar no iPhone

1. Abra a URL HTTPS no Safari.
>>>>>>> 515d55738696a52634e46becf80e35ee6ab5f1c6
2. Preencha o **ID do dispositivo** com um valor único (ex: `iphone_01`).
3. Confirme o **tópico base** (padrão `automacao/ufscar/gps` — deve casar com o tópico que o `server.py` está escutando).
4. Defina o **intervalo** de publicação em segundos.
5. Toque em **Iniciar**. O Safari vai pedir permissão de localização — aceite.
6. A tela mostra o status da conexão e a última coordenada publicada.

**Dicas:**
- Para manter publicando com a tela ativa, vá em `Ajustes > Tela e Brilho > Bloqueio Automático > Nunca`. O Safari pausa o JavaScript quando a tela bloqueia.
- Para deixar com cara de app, use o menu de compartilhar do Safari e escolha **Adicionar à Tela de Início**. Abre em tela cheia, sem barras do navegador.

---

## 4. Testando com Múltiplos Dispositivos

A principal finalidade do projeto é medir o erro de GPS entre celulares. Para testes controlados:

1. Inicie o `server.py` no computador.
2. Em cada celular, inicie o publisher com um `device_id` diferente (`android_01`, `iphone_01`, `iphone_02`, etc.).
3. Coloque os celulares **empilhados no mesmo ponto** por pelo menos 10 minutos.
4. As linhas impressas no terminal mostrarão leituras simultâneas de todos os dispositivos.
5. Redirecione a saída para um arquivo para análise posterior:
   ```bash
   python server.py | tee log_teste_$(date +%Y%m%d_%H%M).txt
   ```

Experimentos sugeridos:
- Ponto estático em ambiente aberto (rua, praça).
- Ponto estático em ambiente fechado (dentro do prédio).
- Movimento controlado (ex: caminhada em linha reta de 100 m).

---

## 5. Debug

### Nada chega no servidor

1. Verifique se o `server.py` mostrou `Conectado ao broker` e `Inscrito em: automacao/ufscar/gps/+`.
2. Abra o [MQTT WebSocket Client do HiveMQ](https://www.hivemq.com/demos/websocket-client/) em um navegador:
   - Host: `broker.hivemq.com`
   - Port: `8884`
   - Marque **SSL**
   - Conecte e inscreva-se no tópico `automacao/ufscar/gps/#`.
3. Se as mensagens aparecem no cliente web mas não no `server.py`, o problema está no servidor (tópico incorreto, firewall bloqueando a porta 1883, etc.).
4. Se não aparecem em lugar nenhum, o problema está no publisher (GPS sem permissão, tópico incorreto, sem internet).

### iPhone não pega localização

- Confira se a URL é HTTPS (não HTTP).
- Nas configurações do iPhone: `Ajustes > Safari > Localização > Permitir`.
- Tente outdoor — GPS demora para estabilizar dentro de prédios.

### Android (Termux) retorna localização vazia

- Confirme a permissão do Termux:API nas configurações.
- Execute `termux-location -p gps` isoladamente para testar. Se travar, tente `termux-location -p network` (usa Wi-Fi/celular em vez de GPS).
- Mantenha o celular em local com visão do céu nos primeiros minutos para o GPS sincronizar com os satélites.

---

## 6. Limitações conhecidas (broker público)

- **`broker.hivemq.com` é público e sem autenticação.** Qualquer pessoa no mundo pode se inscrever no seu tópico. Para evitar colisão ou vazamento, use um tópico específico e não trafegue dados sensíveis.
- **Sem garantia de disponibilidade.** É um broker de demonstração, pode ficar fora do ar sem aviso.

Para um cenário mais próximo de aplicações reais, migrar para o **HiveMQ Cloud** (gratuito, com TLS e autenticação) é o próximo passo. Apenas as seguintes linhas precisariam mudar:

- No `server.py` e no `termux_publisher.py`: trocar host, porta (`8883`), habilitar TLS e adicionar `username_pw_set`.
- No `index.html`: trocar a URL para `wss://seu-cluster.hivemq.cloud:8884/mqtt` e passar `username` e `password` no `mqtt.connect`.

---

## 7. Próximos Passos Previstos

- Dashboard web com mapa (Leaflet + Flask-SocketIO).
- Análise estatística do erro (desvio padrão, CEP — Circular Error Probable).
- Migração para HiveMQ Cloud com TLS e autenticação.
- Camada OPC UA no servidor expondo os dados dos dispositivos em um modelo de informação estruturado, demonstrando o padrão gateway MQTT ↔ OPC UA usado em ambientes industriais.

---

## Referências

- [Documentação MQTT.js](https://github.com/mqttjs/MQTT.js)
- [Documentação paho-mqtt](https://eclipse.dev/paho/files/paho.mqtt.python/html/client.html)
- [Termux Wiki](https://wiki.termux.com/)
- [HiveMQ Public Broker](https://www.hivemq.com/mqtt/public-mqtt-broker/)
- [Especificação MQTT 5.0 (OASIS)](https://docs.oasis-open.org/mqtt/mqtt/v5.0/mqtt-v5.0.html)
