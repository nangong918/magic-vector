FROM eclipse-temurin:21-jre

WORKDIR /app

ARG JAR_FILE=demo/target/*.jar
COPY ${JAR_FILE} app.jar

EXPOSE 48888

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
