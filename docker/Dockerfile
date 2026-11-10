FROM eclipse-temurin:25-jdk

COPY build/libs/app.jar /var/www/application/app.jar

COPY docker/execute.sh /var/www/application/execute.sh

# Interactive Spring Shell: docker exec -it <container> scripts/shell.sh
COPY scripts/shell.sh /var/www/application/scripts/shell.sh

RUN chmod +x /var/www/application/execute.sh /var/www/application/scripts/shell.sh

WORKDIR /var/www/application

ENTRYPOINT ["/var/www/application/execute.sh"]
