FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app
# Cache all Maven dependencies before copying source
COPY pom.xml ./
RUN mvn dependency:go-offline -q

# Now copy source and build (deps layer above is reused on source-only changes)
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
RUN apk add --no-cache graphviz ttf-dejavu
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8003

CMD ["java", "-jar", "app.jar"]
