FROM eclipse-temurin:25-jdk

COPY build/libs/app.jar /var/www/application/app.jar

COPY docker/execute.sh /var/www/application/execute.sh

RUN chmod +x /var/www/application/execute.sh

WORKDIR /var/www/application

ENTRYPOINT ["/var/www/application/execute.sh"]
