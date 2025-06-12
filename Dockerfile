# Étape 1 : Construction avec Maven
FROM maven:3.9.6-eclipse-temurin-17 AS builder

# Dossier de travail
WORKDIR /app

# Copier les fichiers du projet
COPY pom.xml .
COPY src ./src

# Construction du projet (skip tests pour gagner du temps)
RUN mvn clean package -DskipTests

# Étape 2 : Image finale légère avec JDK 17
FROM eclipse-temurin:17-jdk-jammy

# Dossier d'exécution
WORKDIR /app

# Copier le JAR depuis l'étape précédente
COPY --from=builder /app/target/*.jar app.jar

# Port d’écoute (à adapter si différent)
EXPOSE 8080

# Commande de démarrage
ENTRYPOINT ["java", "-jar", "app.jar"]
