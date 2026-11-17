# Etapa 1: compila o projeto com Maven (imagem disponível para amd64 e arm64)
FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

COPY src/ src/
RUN mvn -q -DskipTests package

# Etapa 2: só o JRE e o .jar, rodando com um usuário sem privilégios
FROM eclipse-temurin:21-jre

WORKDIR /app

RUN groupadd --system flowsense && useradd --system --gid flowsense --no-create-home flowsense

ENV SERVER_PORT=8080
# VM de 1 GB: a JVM usa no máximo 70% da memória do container e o coletor de lixo mais leve
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

COPY --from=build /app/target/*.jar app.jar

USER flowsense

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
