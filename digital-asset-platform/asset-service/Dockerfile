FROM maven:3.9.5-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY wallet-service/pom.xml wallet-service/pom.xml
COPY asset-service/pom.xml asset-service/pom.xml
COPY blockchain-service/pom.xml blockchain-service/pom.xml
COPY transaction-ledger-service/pom.xml transaction-ledger-service/pom.xml
COPY settlement-service/pom.xml settlement-service/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
RUN mvn dependency:go-offline -B
COPY common/src common/src
COPY asset-service/src asset-service/src
RUN mvn clean package -DskipTests -pl asset-service -am

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /workspace/asset-service/target/asset-service-1.0.0.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
