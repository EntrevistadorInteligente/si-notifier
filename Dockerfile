FROM amazoncorretto:17-alpine3.18-jdk

# Crear el directorio de la aplicación
RUN mkdir -p /app/

# Instalar sudo y otros paquetes necesarios
RUN apk add --no-cache sudo docker

# Crear el usuario jenkins y agregarlo al grupo docker
RUN addgroup -S docker && adduser -S jenkins -G docker \
    && echo "jenkins ALL=NOPASSWD: ALL" >> /etc/sudoers

# Ajustar permisos para el socket de Docker
RUN chmod 666 /var/run/docker.sock

# Copiar el archivo JAR de la aplicación
COPY target/notificaciones-0.0.1-SNAPSHOT.jar /app/notificaciones.jar

# Exponer el puerto
EXPOSE 8085

# Definir el punto de entrada
ENTRYPOINT ["java", "-jar", "/app/notificaciones.jar"]
