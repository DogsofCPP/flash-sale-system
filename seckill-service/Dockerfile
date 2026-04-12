FROM m.daocloud.io/docker.io/library/eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY *.jar app.jar

EXPOSE 8086

ENTRYPOINT ["java", "-jar", "app.jar"]
