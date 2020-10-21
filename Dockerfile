FROM openjdk:11

COPY target/lazo.jar /

EXPOSE 8890

CMD ["java", "-cp", "/lazo.jar", "lazo.core"]
