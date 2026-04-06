FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
RUN apk add --no-cache graphviz ttf-dejavu
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8003

CMD ["java", "-jar", "app.jar"]
