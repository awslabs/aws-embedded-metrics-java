#Make sure the jdk pulled in dockerfile matches your IDE compile version used for compiling Jar file
FROM openjdk:11
RUN mkdir -p /app

# copy the source files over
COPY build/libs/*.jar /app/app.jar

ENV JAVA_OPTS=""
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar" ]
