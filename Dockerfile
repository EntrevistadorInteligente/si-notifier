FROM amazoncorretto:17-alpine3.18-jdk

EXPOSE 8085

RUN mkdir -p /app/

COPY target/notificaciones-0.0.1-SNAPSHOT.jar /app/notificaciones.jar

ENTRYPOINT ["java", "-jar", "/app/notificaciones.jar"]
