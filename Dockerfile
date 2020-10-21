FROM openjdk:11

COPY target/lazo.jar /

CMD ["java", "-cp", "/lazo.jar", "lazo.core"]
