# --- Etapa 1: Compilar con Maven ---
FROM eclipse-temurin:21-jdk AS buildstage


RUN apt-get update && apt-get install -y maven

WORKDIR /app


COPY pom.xml .
COPY src /app/src


RUN mvn clean package -DskipTests

# --- Etapa 2: Imagen final optimizada ---
FROM eclipse-temurin:21-jdk

WORKDIR /app


COPY --from=buildstage /app/target/*.jar /app/app.jar


COPY src/main/resources/Wallet_MBD /app/src/main/resources/Wallet_MBD

# Directorio local para enlazar el montaje de Amazon EFS
RUN mkdir -p /app/efs

EXPOSE 8081


CMD ["java", "-jar", "/app/app.jar"]