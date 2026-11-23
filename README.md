# VAR-Enabled ⚽

**Sistema de gestión de partidos de fútbol y marcadores en tiempo real con tecnología VAR.**

VAR-Enabled es un proyecto para seguir partidos de fútbol en tiempo real. Desde EventMaker se crean los encuentros y se registran las jugadas; en el dashboard se pueden ver el marcador, los eventos y las estadísticas. El sistema también permite revisar goles y faltas mediante VAR.

## Integrantes

- **Michael David Lagos Rosero**
- **Ronald Felipe Benavides Bastidas**
- **Nicolás Bastidas Calvache**
- **Julio Bolaños Benavides**

## ¿Qué hace el sistema?

- Creación e inicio de partidos desde **EventMaker**.
- Registro de goles, faltas y tarjetas.
- Marcador y cronómetro actualizados en el dashboard.
- Revisión VAR de goles y faltas, con la posibilidad de confirmar o anular jugadas.
- Visualización de eventos, estadísticas y campo de juego ilustrativo.
- Visualización del estado de las apuestas.
- Registro del historial de eventos y resultados.
- Escudos SVG de los equipos registrados en el catálogo Premier League.

## Tecnologías

| Tecnología | Uso |
|---|---|
| Java 17 y Spring Boot | Lógica de negocio y microservicios |
| Angular y TypeScript | Dashboard de partidos |
| HTML, CSS y SVG | Interfaz de EventMaker y escudos |
| Apache Kafka | Publicación y consumo de eventos |
| RabbitMQ | Mensajería entre servicios |
| PostgreSQL | Almacenamiento de historial y catálogo |
| Docker Compose | Ejecución de servicios |
| Git y GitHub | Control de versiones |

## Arquitectura del sistema

Cuando se registra una jugada en **EventMaker**, el evento pasa al backend y se publica en Kafka. Otros servicios lo procesan, actualizan el partido y envían la información al dashboard por medio de RabbitMQ y WebSocket. PostgreSQL guarda el historial de eventos y resultados.

| Servicio | Función |
|---|---|
| `live_feed_producer` | Crea partidos y publica eventos |
| `match_state_service` | Actualiza el estado y el marcador |
| `dashboard_backend` | Comunica el marcador al frontend |
| `notification_backend` | Gestiona notificaciones |
| `betting_suspension_service` | Controla la suspensión de apuestas |
| `betting_worker` | Procesa operaciones de apuestas |
| `historical_archiver` | Almacena historial y resultados |

## Reglas de negocio

1. El marcador no puede tener goles negativos.
2. Solo se puede anular una jugada previamente registrada y habilitada para revisión.
3. Una misma jugada no se puede anular dos veces.
4. Cada evento debe tener un identificador único.
5. Solo puede haber una revisión VAR abierta a la vez para un partido.
6. Los goles y las faltas se pueden revisar dentro de los dos minutos siguientes a la jugada.
7. Durante la revisión VAR, las apuestas quedan detenidas y su estado se actualiza según la decisión y el marcador.

## Premier League y base de datos

Para los partidos de la Premier League agregamos **20 equipos**, sus escudos SVG y el logo de la competición.

- `database/premier_league.sql`: crea las tablas `ligas` y `equipos` y registra el catálogo.
- `frontend/public/assets/equipos/`: contiene los 20 escudos SVG.
- `frontend/public/assets/ligas/premier-league.svg`: contiene el logo de la liga.

Cada equipo pertenece a una liga mediante `liga_id`. En el dashboard, los escudos aparecen según el nombre del equipo que participa. Tanto el catálogo SQL como las imágenes están incluidos en el repositorio.

## Diagramas del sistema

### 1. Diagrama de clases

Muestra las clases que manejan los partidos, los eventos y las revisiones VAR.

![Diagrama de clases](docs/diagramas/01_clases.png)

### 2. Diagrama de componentes

Muestra el frontend, los microservicios y la comunicación con Kafka, RabbitMQ y PostgreSQL.

![Diagrama de componentes](docs/diagramas/02_componentes.png)

### 3. Diagrama de secuencia

Sigue el recorrido de un evento desde que se registra hasta que aparece en el marcador.

![Diagrama de secuencia](docs/diagramas/03_secuencia.png)

### 4. Diagrama de paquetes

Muestra cómo está organizado el código del proyecto.

![Diagrama de paquetes](docs/diagramas/04_paquetes.png)

### 5. Diagrama de contexto

Explica quiénes usan el sistema y con qué servicios se comunica.

![Diagrama de contexto](docs/diagramas/05_contexto.png)

### 6. Diagrama entidad-relación (ER)

Muestra las tablas de ligas y equipos, además de una vista general de cómo se relacionan los datos de partidos, eventos y resultados. Algunas relaciones son ilustrativas y no están creadas como claves foráneas en el SQL.

![Diagrama entidad-relación](docs/diagramas/06_er.png)

## Evidencias del funcionamiento

### 1. Marcador y campo de juego

Partido de prueba **Arsenal vs. Aston Villa**. Se observan los escudos SVG, el marcador 0-0, el minuto de juego y la cancha ilustrativa.

![Marcador y campo de juego](docs/evidencias/01_marcador_campo.png)

### 2. Eventos y apuestas

Se visualizan el inicio del partido, la sección de eventos y el estado de apuestas abiertas.

![Eventos y apuestas](docs/evidencias/02_eventos_apuestas.png)

### 3. Estadísticas del partido

El dashboard muestra goles, faltas y tarjetas amarillas y rojas de ambos equipos.

![Estadísticas del encuentro](docs/evidencias/03_estadisticas.png)

Las capturas son evidencias de la interfaz durante una simulación del partido.

---

**VAR-Enabled — Gestión de partidos y marcadores en tiempo real.**
