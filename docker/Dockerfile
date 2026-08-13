FROM eclipse-temurin:25-jdk

COPY build/libs/app.jar /var/www/application/app.jar

COPY src/main/resources/jwt/private.pem /var/www/application/src/main/resources/jwt/private.pem
COPY src/main/resources/jwt/prod/public.pem /var/www/application/src/main/resources/jwt/public.pem
COPY docker/execute.sh /var/www/application/execute.sh

RUN chmod +x /var/www/application/execute.sh

WORKDIR /var/www/application

ENTRYPOINT ["/var/www/application/execute.sh"]
