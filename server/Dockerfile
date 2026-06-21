# Base image using JDK 21 JRE on Alpine for maximum performance and security
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the pre-built install distribution from the gradle :server:installDist task
COPY build/install/server /app

# Expose Ktor default server port
EXPOSE 8080

# Execute the packaged Ktor startup script
ENTRYPOINT ["/app/bin/server"]
