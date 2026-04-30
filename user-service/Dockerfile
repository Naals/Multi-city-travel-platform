FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /build

COPY pom.xml ./parent-pom.xml
COPY user-service/pom.xml ./pom.xml
COPY proto/ ../proto/

RUN mvn dependency:go-offline -q

COPY user-service/src ./src
RUN mvn clean package -DskipTests -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+UseG1GC \
               -Djava.security.egd=file:/dev/./urandom"

COPY --from=builder /build/target/*.jar app.jar

# Expose both HTTP and gRPC ports
EXPOSE 8082 9090

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]