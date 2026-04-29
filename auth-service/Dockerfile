FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /build
COPY ../../pom.xml ./parent-pom.xml
COPY pom.xml .
RUN mvn dependency:go-offline -f pom.xml -q
COPY src ./src
RUN mvn clean package -DskipTests -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"
COPY --from=builder /build/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]