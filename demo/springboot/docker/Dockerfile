FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /build
COPY . .

# Build starter modules first, then package demo.
RUN mvn -f starters/pom.xml clean install -DskipTests && \
    mvn -f demo/pom.xml clean package spring-boot:repackage -DskipTests

FROM eclipse-temurin:21-jre

RUN apt-get update && \
    apt-get install -y --no-install-recommends ffmpeg && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=builder /build/demo/target/*.jar /app/app.jar

EXPOSE 48888
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
