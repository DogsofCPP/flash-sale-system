FROM m.daocloud.io/docker.io/eclipse-temurin:17-jre
WORKDIR /app
COPY target/user-service-1.0.0.jar app.jar
EXPOSE 8081
ENV MYSQL_HOST=flash-sale-mysql-master
ENV MYSQL_DATABASE=flash_sale_user
ENV REDIS_HOST=flash-sale-redis
ENTRYPOINT ["java", "-jar", "app.jar"]
