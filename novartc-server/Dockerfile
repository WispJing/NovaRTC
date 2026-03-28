FROM eclipse-temurin:21-jre
WORKDIR /app

ENV TZ=UTC
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Duser.timezone=UTC"

COPY target/novartc-server-1.0.0-SNAPSHOT.jar /app/app.jar

EXPOSE 7000 9688

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
