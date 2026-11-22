# Contexto de build: la carpeta backend/ completa (monorepo Maven)
#   docker build -f ../dockerizacion/servicios/betting_suspension_service/Dockerfile -t var-enabled/betting_suspension_service .  (desde backend/)

# Etapa 1: build
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app
# El pom padre declara todos los modulos: Maven necesita todos los pom.xml
# presentes aunque solo se construya este servicio.
COPY pom.xml .
COPY common-domain/pom.xml common-domain/
COPY live_feed_producer/pom.xml live_feed_producer/
COPY match_state_service/pom.xml match_state_service/
COPY dashboard_backend/pom.xml dashboard_backend/
COPY notification_backend/pom.xml notification_backend/
COPY betting_suspension_service/pom.xml betting_suspension_service/
COPY betting_worker/pom.xml betting_worker/
COPY historical_archiver/pom.xml historical_archiver/
# common-domain se excluye porque aun no esta compilado; -am lo construye despues
RUN mvn -pl betting_suspension_service -am dependency:go-offline -B -DexcludeGroupIds=com.varenabled
COPY common-domain/src common-domain/src
COPY betting_suspension_service/src betting_suspension_service/src
RUN mvn -pl betting_suspension_service -am package -DskipTests -B

# Etapa 2: runtime
FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app
COPY --from=build /app/betting_suspension_service/target/*.jar app.jar
USER appuser
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
