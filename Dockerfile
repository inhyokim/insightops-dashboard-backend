# Spring Boot Runtime
FROM openjdk:17-jdk-slim

WORKDIR /app

# Copy the pre-built jar file
COPY target/dashboard-backend-1.0.0.jar app.jar

# Expose port
EXPOSE 3002

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:3002/health || exit 1

# Start the application
ENTRYPOINT ["java", "-jar", "app.jar"]