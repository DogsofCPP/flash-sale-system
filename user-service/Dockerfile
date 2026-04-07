FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/user-service-1.0.0.jar app.jar
EXPOSE 8081
ENV MYSQL_HOST=host.docker.internal
ENV MYSQL_SLAVE_HOST=host.docker.internal
ENV REDIS_HOST=host.docker.internal
ENV ELASTICSEARCH_HOST=host.docker.internal
ENTRYPOINT ["java", "-jar", "app.jar"]
