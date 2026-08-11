# ---------- Etapa 1: build ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copiamos primero el pom.xml para aprovechar la cache de dependencias
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Ahora copiamos el código fuente y empaquetamos
COPY src ./src
RUN mvn clean package -DskipTests -B

# ---------- Etapa 2: runtime ----------
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Usuario no root para ejecutar la aplicación
RUN addgroup --system spring && adduser --system --ingroup spring spring

# Copiamos únicamente el jar generado en la etapa de build
COPY --from=build /app/target/*.jar app.jar
RUN mkdir -p /app/storage && chown -R spring:spring /app

USER spring
EXPOSE 8080

# Healthcheck usando el endpoint de Actuator
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
