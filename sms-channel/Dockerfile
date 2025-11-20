FROM eclipse-temurin:17-jre-alpine as runtime
WORKDIR /app

# Build artifact expected at ../target/sms-channel-*.jar when using multi-module build
ARG JAR_PATH=target/sms-channel-1.0.0.jar
COPY ${JAR_PATH} app.jar

EXPOSE 8087
ENTRYPOINT ["java","-jar","/app/app.jar"]


