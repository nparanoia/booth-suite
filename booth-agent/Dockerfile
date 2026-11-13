# Agent 进容器（compose 默认构建本镜像）。改代码后需 mvn package 再 compose up --build。
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
ARG JAR_FILE=target/booth-agent-1.0.0-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar
EXPOSE 8091
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
