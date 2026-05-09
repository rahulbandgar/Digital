FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/settlement-service-1.0.0.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "app.jar"]
