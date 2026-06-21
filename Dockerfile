# 1. Étape de compilation (On utilise JDK 21)
FROM gradle:8-jdk21 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN chmod +x gradlew
RUN ./gradlew :server:installDist --no-daemon

# 2. Étape d'exécution (On passe de Java 17 à Java 21-alpine)
FROM eclipse-temurin:21-jre-alpine
EXPOSE 8080
RUN mkdir /app

COPY --from=build /home/gradle/src/server/build/install/server/ /app/
WORKDIR /app/bin

RUN chmod +x ./server

CMD ["./server"]