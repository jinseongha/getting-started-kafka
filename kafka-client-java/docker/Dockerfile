FROM amazoncorretto:17

WORKDIR /app
COPY build/libs/*.jar app.jar

USER 1001
EXPOSE 8080

CMD ["java", "-jar", "/app/app.jar"]
