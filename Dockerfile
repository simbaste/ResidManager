# 1. Étape de compilation (Prend tout le projet KMP)
FROM gradle:8-jdk21 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN chmod +x gradlew
RUN ./gradlew :server:installDist --no-daemon --no-configuration-cache

# 2. Étape d'exécution (Image alpine légère)
FROM eclipse-temurin:21-jre-alpine
EXPOSE 8080
RUN mkdir /app

# IMPORTANT : Le chemin contient bien /server/build/... car Gradle compile le sous-module server
COPY --from=build /home/gradle/src/server/build/install/server/ /app/
WORKDIR /app/bin

RUN chmod +x ./server

CMD ["./server"]