# Multi-stage build to avoid credential and JAR path issues
FROM eclipse-temurin:17-jdk-alpine as builder
WORKDIR /build

# Copy Maven wrapper and pom files
COPY pom.xml .
COPY sms-channel/pom.xml ./sms-channel/
COPY common/pom.xml ./common/

# Copy source code
COPY sms-channel/src ./sms-channel/src
COPY common/src ./common/src

# Build the project
RUN apk add --no-cache maven && \
    mvn clean package -pl sms-channel -am -DskipTests && \
    find . -name "*.jar" -type f

# Runtime stage
FROM eclipse-temurin:17-jre-alpine as runtime
WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /build/sms-channel/target/sms-channel-*.jar app.jar

EXPOSE 8087
ENTRYPOINT ["java","-jar","/app/app.jar"]


