FROM eclipse-temurin:25-jre

# Unprivileged runtime user; the numeric UID lets Kubernetes verify runAsNonRoot
RUN useradd --system --uid 10001 --no-create-home --shell /usr/sbin/nologin app

COPY build/libs/app.jar /var/www/application/app.jar

COPY docker/execute.sh /var/www/application/execute.sh

# Interactive Spring Shell: docker exec -it <container> scripts/shell.sh
COPY scripts/shell.sh /var/www/application/scripts/shell.sh

RUN chmod +x /var/www/application/execute.sh /var/www/application/scripts/shell.sh \
  # The app user may create files in the working directory (Spring Shell writes its history there) \
  # but the root-owned JAR and scripts stay read-only for it
  && chown app /var/www/application

WORKDIR /var/www/application

USER 10001

ENTRYPOINT ["/var/www/application/execute.sh"]
