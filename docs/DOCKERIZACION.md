# Guía para dockerizar VAR-Enabled

Este documento resume todo lo que alguien necesita saber para contenerizar el backend sin tener que leer todo el historial de commits. Todo el código mencionado aquí ya está escrito, probado y funcionando; lo único que falta es el Dockerfile por servicio y el `docker-compose.yml`.

## 1. Estado del repo antes de empezar

- Rama base: `dev`.
- **Pendiente de fusionar:** la rama `feature/historical-archiver` (agrega el séptimo microservicio, `historical_archiver`, con persistencia en PostgreSQL). Antes de dockerizar, fusionar esa rama a `dev` o partir directamente desde ella.
- Convención del repo: una rama por tarea, commits cortos y humanos (sin prefijos tipo `feat:`), sin atribución de IA en los commits. La dockerización debería ir en su propia rama, ej. `feature/docker-compose`.
- Todo el código Java vive bajo `backend/`. Es un monorepo Maven multimódulo: un `pom.xml` padre en `backend/pom.xml` y un módulo por microservicio (más `common-domain`, que no es un microservicio sino la librería compartida).

## 2. Los 7 microservicios

| Carpeta | Qué hace | Consume | Publica | Puerto propio |
|---|---|---|---|---|
| `live_feed_producer` | Simula el partido: manda goles y anulaciones al azar | — | Kafka `match_events` | — |
| `match_state_service` | El "cerebro": reconcilia el marcador de fútbol | Kafka `match_events` (`group-id: match-state-service`) | RabbitMQ `live_updates` (marcador) y `live_alerts` (alertas) | — |
| `dashboard_backend` | Sirve el marcador en vivo por WebSocket | RabbitMQ `live_updates` (routing `score.*`) | WebSocket en `/marcador` | **8081** (exponer al host) |
| `notification_backend` | Imprime las alertas con trazabilidad | RabbitMQ `live_alerts` | consola | — |
| `betting_suspension_service` | Decide si suspender/reanudar apuestas | Kafka `match_events` (`group-id: betting-suspension-service`, grupo independiente) | RabbitMQ `betting_commands` | — |
| `betting_worker` | Ejecuta la suspensión/reanudación (idempotente) | RabbitMQ `betting_commands` | consola | — (pensado para correr con **varias réplicas**) |
| `historical_archiver` | Persiste el historial y audita las revisiones VAR | Kafka `match_events` (`group-id: historical-archiver`) | PostgreSQL | — |

Cada módulo trae su propio `Dockerfile` independiente (no comparten imagen), pero todos comparten el mismo `pom.xml` padre y dependen de `common-domain`.

## 3. Infraestructura que necesita el `docker-compose.yml`

### Kafka + Zookeeper
- Topic: **`match_events`** (clave = `match_id`, para que Kafka preserve el orden por partido).
- No hace falta crearlo a mano: Spring Kafka lo crea si no existe, pero para producción es mejor declararlo explícito con al menos 1 partición.
- Tres microservicios lo consumen, cada uno con su propio `group-id` (ver tabla arriba) — así cada uno recibe **todos** los eventos de forma independiente.

### RabbitMQ
Los exchanges y colas los declaran los propios microservicios por código (`@Bean` de Spring AMQP) la primera vez que arrancan — no hay que crearlos a mano, solo que el contenedor de RabbitMQ esté disponible. Para referencia, esto es lo que se crea:

| Nombre | Tipo | Quién publica | Quién consume |
|---|---|---|---|
| `live_updates` | Topic Exchange | `match_state_service` (routing key `score.match_<id>`) | `dashboard_backend` (bind con `score.*`) |
| `live_alerts` | Fanout Exchange | `match_state_service` | `notification_backend` |
| `betting_commands` | Cola directa (Work Queue) | `betting_suspension_service` | `betting_worker` (con réplicas, se reparten los mensajes) |

Imagen recomendada: `rabbitmq:3-management` (el plugin de management ayuda a depurar visualmente en `localhost:15672`, usuario/clave por defecto `guest`/`guest`).

### PostgreSQL
- Usado solo por `historical_archiver`.
- Base de datos / usuario / clave configurados hoy en el código: `var_enabled` / `var_enabled` / `var_enabled` (ver `backend/historical_archiver/src/main/resources/application.yml`). Se pueden cambiar, pero deben coincidir entre el contenedor de Postgres (`POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`) y ese `application.yml`.
- Necesita **volumen con nombre** para persistir datos entre reinicios.
- Las tablas (`match_event_log`, `video_review_audit`, `final_score`) las crea Hibernate solo (`ddl-auto: update`), no hace falta un script de inicialización.

## 4. Qué hay que cambiar para que corran en contenedores

Hoy cada `application.yml` apunta a `localhost` porque se probó todo con Kafka embebido, mocks de RabbitMQ y H2 — nunca se corrió contra los brokers reales. Para Docker, cada servicio necesita que estos valores se puedan sobreescribir (por variables de entorno o perfiles), apuntando al nombre del servicio en la red de Docker Compose en vez de `localhost`:

- `spring.kafka.bootstrap-servers` → host:puerto del servicio Kafka (ej. `kafka:9092`)
- `spring.rabbitmq.host` → nombre del servicio RabbitMQ (ej. `rabbitmq`)
- `spring.datasource.url` (solo `historical_archiver`) → nombre del servicio Postgres (ej. `jdbc:postgresql://postgres-db:5432/var_enabled`)

La forma más simple: Spring Boot permite sobreescribir cualquier propiedad de `application.yml` con una variable de entorno equivalente (ej. `SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092`, `SPRING_RABBITMQ_HOST=rabbitmq`), así que no hace falta tocar el código — solo definir esas variables en el `docker-compose.yml` de cada servicio.

## 5. Patrón de Dockerfile (pedido por el enunciado)

Cada microservicio necesita un Dockerfile **multietapa**:

```dockerfile
# Etapa 1: build
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY common-domain/pom.xml common-domain/
COPY <nombre_del_servicio>/pom.xml <nombre_del_servicio>/
RUN mvn -pl <nombre_del_servicio> -am dependency:go-offline -B
COPY common-domain/src common-domain/src
COPY <nombre_del_servicio>/src <nombre_del_servicio>/src
RUN mvn -pl <nombre_del_servicio> -am package -DskipTests

# Etapa 2: runtime
FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
COPY --from=build /app/<nombre_del_servicio>/target/*.jar app.jar
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

**Importante:** como es un monorepo Maven, el contexto de build de cada Dockerfile debe ser la carpeta `backend/` completa (no la carpeta de cada microservicio), y el build tiene que usar `-pl <módulo> -am` ("also make") para que Maven compile primero `common-domain` dentro del mismo contenedor — si se olvida el `-am`, el build va a fallar porque no va a encontrar el artefacto `common-domain` en el repositorio Maven local de esa imagen.

## 6. `docker-compose.yml`: lo que pide el enunciado

- Red(es) virtual(es) aisladas para los servicios.
- `deploy.resources.limits` (cpus y memory) en **cada** contenedor Spring Boot y en el de Postgres. El enunciado no fija números exactos; son libres siempre que estén presentes y sean razonables (ej. `0.5` CPU / `512M` cada uno para los microservicios chicos).
- `betting_worker` debe desplegarse con **varias réplicas** (`deploy.replicas: 3` o similar) para demostrar el balanceo de carga de la Work Queue — como es idempotente, no hay problema en que dos réplicas reciban comandos del mismo partido.
- Exponer al host solo lo necesario para verificar que funciona: `dashboard_backend` (puerto 8081, para ver el WebSocket) y probablemente el management UI de RabbitMQ (15672) mientras se depura. Kafka y Postgres pueden quedar solo en la red interna.

## 7. Cómo verificar que quedó bien

El escenario de prueba de punta a punta es el del enunciado: un gol que se anula por el VAR. Con todo el `docker-compose up`:

1. `live_feed_producer` va a mandar goles cada ~10 segundos y, al azar, anulaciones.
2. Conectarse al WebSocket `ws://localhost:8081/marcador` debería mostrar el marcador cambiando y volviendo a 0 cuando se anula un gol.
3. Los logs de `notification_backend` deberían mostrar las alertas con `match_id`, `event_id` y hora.
4. Los logs de `betting_worker` (en cualquiera de sus réplicas) deberían mostrar "apuestas SUSPENDIDO" al gol y "apuestas ABIERTO" al anularse.
5. Conectado a la base Postgres, las tablas `match_event_log`, `video_review_audit` y `final_score` deberían ir llenándose.

## 8. Limitaciones a tener en cuenta

- **Solo fútbol está conectado de punta a punta.** El dominio de baloncesto y voleibol existe en `common-domain` (con sus pruebas), pero ningún microservicio los consume todavía — `live_feed_producer` solo genera eventos de fútbol. Esto es una decisión de alcance documentada desde el principio (fútbol completo es lo obligatorio; baloncesto/voleibol quedaron en el dominio como demostración de la herencia).
- El estado de "apuestas abiertas/suspendidas" de `betting_worker` vive en memoria, por servicio — no hay un store compartido entre réplicas. Es una simplificación consciente para mantener el alcance del proyecto razonable.
- No hay script de inicialización de datos ni `pt_*`/seed data: todo arranca vacío y se va llenando con los eventos simulados.
