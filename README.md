# Exchange Platform

Spring Boot multi-module trading exchange backend.

## Module layout

```
exchange-parent/
├── common/                  shared DTOs, events, enums, exceptions
├── order-service/           REST API — place/cancel/query orders   :8081
├── matching-engine/         Kafka consumer — in-memory order book  (no HTTP)
├── account-service/         balances, freeze/unfreeze, settlement  :8082
├── market-data-service/     WebSocket push + K-line REST           :8083
├── docker-compose.yml       Postgres + Redis + Kafka + Kafka-UI
└── scripts/init-dbs.sql     one-shot DB creation
```

## Data flow

```
Client → order-service ──[order-events]──▶ matching-engine
                                                  │
                                         [trade-events]
                                          ┌────────┴─────────┐
                                    account-service    market-data-service
                                    (settle funds)     (push WS + K-lines)
```

## Quick start

### 1. Start infrastructure

```bash
docker compose up -d
# wait ~20s for Kafka to be ready
```

### 2. Create Kafka topics

```bash
docker exec exchange-kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --create --topic order-events  --partitions 4 --replication-factor 1

docker exec exchange-kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --create --topic trade-events  --partitions 4 --replication-factor 1
```

### 3. Build all modules

```bash
mvn clean install -DskipTests
```

### 4. Run services (separate terminals)

```bash
# Terminal 1
java -jar order-service/target/order-service-1.0.0-SNAPSHOT.jar

# Terminal 2
java -jar matching-engine/target/matching-engine-1.0.0-SNAPSHOT.jar

# Terminal 3
java -jar account-service/target/account-service-1.0.0-SNAPSHOT.jar

# Terminal 4
java -jar market-data-service/target/market-data-service-1.0.0-SNAPSHOT.jar
```

### 5. Monitor Kafka

Open http://localhost:9091 for Kafka-UI.

## WebSocket subscriptions (STOMP)

Connect endpoint: `ws://localhost:8083/ws`

| Topic | Payload |
|-------|---------|
| `/topic/ticker.BTC-USDT` | `TickerMessage` — last price + qty |
| `/topic/depth.BTC-USDT`  | `OrderBookSnapshot` — L2 depth (20 levels) |

## REST endpoints

### order-service :8081
| Method | Path | Description |
|--------|------|-------------|
| POST   | `/api/v1/orders` | Place a new order |
| DELETE | `/api/v1/orders/{id}` | Cancel an order |
| GET    | `/api/v1/orders?status=OPEN` | List orders (paginated) |

### market-data-service :8083
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/market-data/candles?instrument=BTC-USDT&interval=1m&from=...&to=...` | Historical candles |
| GET | `/api/v1/market-data/candles/latest?instrument=BTC-USDT&interval=1m&limit=200` | Latest N candles |

## Next tasks (T-03 onwards)

- T-03: Spring Security + JWT auth
- T-04: Redis order-book cache integration
- T-05~08: Full account + order REST with validation
- T-09~13: Matching engine unit tests
