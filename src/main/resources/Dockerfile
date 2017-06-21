FROM jamesdbloom/docker-java8-maven

ADD gate-8.4.1-build5753-installer.jar /

RUN java -jar /gate-8.4.1-build5753-installer.jar; exit 0

CMD ["java", "-classpath","/usr/local/GATE_Developer_8.4.1/bin/gate.jar", "/usr/local/GATE_Developer_8.4.1/lib"]

WORKDIR /code


