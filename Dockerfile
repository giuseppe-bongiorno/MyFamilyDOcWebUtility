# FASE 1: Build dell'applicazione Spring Boot (Build Stage)
# Usa un'immagine Maven o Gradle per compilare il tuo JAR
FROM maven:3.9.6-amazoncorretto-17 AS build

# Imposta la directory di lavoro all'interno del container di build
WORKDIR /app

# Copia il file pom.xml e il codice sorgente
COPY pom.xml .
COPY src ./src

# Esegui il build Maven
RUN mvn clean package -DskipTests

# FASE 2: Creazione dell'immagine finale (Runtime Stage)
# Usa un'immagine OpenJDK più leggera per il runtime
#FROM openjdk:17-jdk-slim
FROM eclipse-temurin:17-jdk-jammy

# Imposta la directory di lavoro all'interno del container finale
WORKDIR /app

# Copia il JAR compilato dalla fase di build
COPY --from=build /app/target/*.jar app.jar

# Espone la porta su cui il tuo Spring Boot service è in ascolto
# Per default, Spring Boot usa la porta 8080
EXPOSE 8080

# Comando per eseguire l'applicazione quando il container viene avviato
#CMD ["java", "-jar", "app.jar"]
ENTRYPOINT ["java", "-jar", "app.jar"]

# Curl per health check:
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*