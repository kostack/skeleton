# Skeleton Service

A Kotlin and Spring Boot skeleton for building reactive services with WebFlux, JWT authentication, MongoDB, MySQL/R2DBC, Flyway, RabbitMQ, OpenAPI, and Spring Shell.

## Technology stack

- Java 25 and Kotlin
- Spring Boot with WebFlux and coroutines
- Spring Security with RSA-signed JWTs
- Reactive MongoDB and MySQL/R2DBC
- Flyway database migrations
- Spring Cloud Stream with RabbitMQ
- OpenAPI and Swagger UI
- JUnit 5, MockK, Reactor Test, JaCoCo, and Spotless

## Prerequisites

Install or provide:

- JDK 25 (the Gradle toolchain can also resolve a compatible JDK when toolchain download repositories are configured)
- MySQL running on port `3306`
- MongoDB running on port `27017`
- RabbitMQ running on port `5672`
- OpenSSL, if you need to generate local JWT keys

The repository includes the Gradle wrapper, so a separate Gradle installation is not required.

## How to develop in a devcontainer

As an alternative to installing JDK 25 locally, open the project in a devcontainer (IntelliJ IDEA, VS Code, or the `devcontainer` CLI). The container provides Ubuntu 24.04 with JDK 25 (Temurin).

MySQL, MongoDB, and RabbitMQ are not part of the devcontainer. They are shared with other services and must already be running on the host. The container uses host networking, so:

- the `localhost` values in `docker/.env` reach the shared services unchanged, and
- the application listens directly on the host, on `SERVER_PORT` (default `8080`). If another application already uses that port, set a different `SERVER_PORT` in `docker/.env`.

Host networking works natively on Linux. Docker Desktop for macOS and Windows requires host networking to be enabled in its settings.

On first creation, `.devcontainer/post-create.sh`:

1. Creates `docker/.env` from `docker/.env.dist` with a freshly generated JWT key pair, if `docker/.env` does not exist yet. An existing file is left unchanged.
2. Downloads Gradle and the project dependencies.

Then run `./gradlew bootRun` or `./gradlew test` inside the container as usual.

### How to open the devcontainer in IntelliJ IDEA

This requires IntelliJ IDEA Ultimate (or another paid JetBrains IDE) and a running local Docker.

1. Open `.devcontainer/devcontainer.json`.
2. Click the container icon in the gutter next to the opening `{` and select **Create Dev Container and Mount Sources…**. Use **Mount Sources**, not **Clone Sources**: mounting uses your existing project directory, including `docker/.env` and `.git`, whereas cloning creates a fresh copy without them.
3. Confirm IntelliJ IDEA as the IDE backend; it is preselected by `customizations.jetbrains.backend`.
4. Wait for the first build, which installs JDK 25, runs `post-create.sh`, and downloads dependencies. A JetBrains Client window then opens, connected to the IDE running inside the container.

To reopen it later, use **Remote Development → Dev Containers** on the Welcome screen. This reuses the existing container instead of rebuilding it.

Start the application with the Gradle `bootRun` task (Gradle tool window → **Tasks → application → bootRun**, or `./gradlew bootRun` in the terminal). Do not use the run icon next to `main()` in `Application.kt`: only the Gradle tasks load `docker/.env`, so a plain Spring Boot or Kotlin run configuration starts without the required variables and fails. To use such a run configuration anyway, add the variables from `docker/.env` to its **Environment variables** field.

Because the container uses host networking, the application is reachable from the host browser at `http://localhost:<SERVER_PORT>` without port forwarding.

The Gradle caches live in Docker volumes. The container's project `.gradle` directory is kept separate from the host's, so Gradle in the container and on the host never wait for each other's locks.

## How to configure the application

Copy the environment template:

```bash
cp docker/.env.dist docker/.env
```

`bootRun` and the test tasks automatically load variables from a file in `docker/`. Gradle uses `docker/.env.<profile>` when it exists for the active Spring profile (for example `docker/.env.staging`), and falls back to `docker/.env` otherwise. The profile comes from the `springProfile` Gradle property or `SPRING_PROFILES_ACTIVE`, and defaults to `dev`. Use a single profile name; a list such as `dev,local` is not resolved to a profile-specific file.

Configure these values for your local services:

| Variable | Example | Purpose |
| --- | --- | --- |
| `SERVER_PORT` | `8080` | HTTP port of the application; change it if another local app uses `8080` |
| `MONGO_URI` | `mongodb://localhost:27017/skeleton?authSource=admin` | MongoDB connection URI |
| `MYSQL_URL` | `mysql://localhost:3306` | MySQL host URL without the database name |
| `MYSQL_DB` | `skeleton` | MySQL database/schema |
| `MYSQL_USER` | `root` | MySQL user |
| `MYSQL_PASSWORD` | `root` | MySQL password |
| `RABBITMQ_HOST` | `localhost` | RabbitMQ host |
| `RABBITMQ_USER` | `guest` | RabbitMQ user |
| `RABBITMQ_PASS` | `guest` | RabbitMQ password |
| `RABBITMQ_PORT` | `5672` | RabbitMQ AMQP port |
| `BASIC_AUTH_USER` | `staging` | Swagger HTTP Basic username |
| `BASIC_AUTH_PASSWORD` | `set_pass` | Swagger HTTP Basic password; replace the template value |
| `PROBE_HEADER` | `x-k8s` | Required actuator request header name |
| `PROBE_VALUE` | `set_value` | Required actuator header value; replace the template value |
| `JWT_PRIVATE_KEY` | Base64-encoded PEM | PKCS#8 RSA private key for signing tokens |
| `JWT_PUBLIC_KEY` | Base64-encoded PEM | X.509 RSA public key for validating tokens |

Populate `JWT_PRIVATE_KEY` and `JWT_PUBLIC_KEY` as described below.

Create the MySQL database before starting the service:

```sql
CREATE DATABASE skeleton;
```

Flyway applies migrations from `src/main/resources/db/migration` when the application starts.

### How to generate JWT keys

Create a local RSA key pair in a temporary directory outside the project:

```bash
KEY_DIR="$(mktemp -d)"
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 \
  -out "$KEY_DIR/private.pem"
openssl rsa -pubout \
  -in "$KEY_DIR/private.pem" \
  -out "$KEY_DIR/public.pem"

base64 < "$KEY_DIR/private.pem" | tr -d '\n'
printf '\n'
base64 < "$KEY_DIR/public.pem" | tr -d '\n'
printf '\n'

rm -r "$KEY_DIR"
```

Copy the first Base64 output into `JWT_PRIVATE_KEY` and the second into `JWT_PUBLIC_KEY` in `docker/.env`. Both values encode the entire PEM file, including its header and footer, on a single line. The application expects encoded key contents rather than file paths, so the `.pem` files are not needed afterwards.

Do not place key files under `src/main/resources`: everything there is packaged into `app.jar` and the Docker image, even when Git ignores it.

Keep private keys and encoded private-key values out of version control. In deployed environments, supply these values through secret management.

## How to run the service

Start the required databases and RabbitMQ, then run:

```bash
./gradlew bootRun
```

The default Spring profile is `dev`, and the HTTP server listens on `SERVER_PORT` from `docker/.env` (default `8080`).

### How to run the executable JAR

Build the executable Spring Boot JAR and start it with Java 25:

```bash
./gradlew bootJar
java -jar build/libs/app.jar
```

When invoking `java -jar` directly, variables from `docker/.env` are not loaded automatically. Export them first or provide them through your shell or deployment environment.

To select another profile:

```bash
SPRING_PROFILES_ACTIVE=staging ./gradlew bootRun
```

Available profile-specific configuration files are:

- `application-test.yml`
- `application-shell.yml`
- `application-staging.yml`
- `application-production.yml`
- `application-cloud-consumer.yml`

### RabbitMQ producers and consumers

Producer bindings are part of `application.yml` and apply to every profile, so `StreamBridge` can publish from any of them. For example, `supplier-out-0` sends to `demo.queue`.

Consumers belong to the `cloud-consumer` profile, defined in `application-cloud-consumer.yml`: the `consumer` function on `demo.queue` (group `skeleton-service`), its retries, and the dead-letter queue. The `consumer` bean in `cloud/stream/Demo.kt` is created only with this profile.

The `dev`, `staging`, and `test` profiles include `cloud-consumer` through a profile group. `shell` and `production` do not, so they publish but never consume. Add the profile explicitly when a deployment must consume, for example `SPRING_PROFILES_ACTIVE=production,cloud-consumer`.

To run only the consumer, without other profiles:

```bash
./gradlew bootRunConsumer
```

`application-cloud-consumer.yml` also sets `spring.main.web-application-type: none`, so no HTTP server starts in any profile that includes `cloud-consumer`.

When adding messaging, put new `*-out-*` producer bindings in `application.yml`, and consumer functions with their `*-in-*` bindings in `application-cloud-consumer.yml`.

## How to use the API documentation

With the `dev` or `staging` profile, open:

```text
http://localhost:8080/docs
```

Swagger resources use HTTP Basic authentication with `BASIC_AUTH_USER` and `BASIC_AUTH_PASSWORD` from your environment.

Routes under `/api/**` require a valid JWT. The current configuration uses the `x-auth` header with an empty prefix:

```http
x-auth: <token>
```

Example requests:

```bash
curl -H "x-auth: $TOKEN" \
  http://localhost:8080/api/me

curl -G -H "x-auth: $TOKEN" \
  --data-urlencode "completed=false" \
  --data-urlencode "sort=title,desc" \
  http://localhost:8080/api/todos
```

Tokens may also be supplied through the `token` query parameter, as enabled in `application.yml`.

Tokens must use the audience `kostack`, the `RS512` signature algorithm, and one of the trusted issuers configured in `application.yml`:

| Issuer | Verified with |
| --- | --- |
| `local` | `JWT_PUBLIC_KEY`; this is also the issuer the service signs its own tokens with |
| `user-service.local` | The JWKS at `http://user-service.local/.well-known/jwks.json` |

For local development, generate a token with the `generate-token` shell command described below. The JWT subject identifies the user, and the `roles` claim supplies their roles. `UserProviderImpl` loads an existing user by identity or creates an in-memory user when no database record exists; roles come from the token in either case.

| Endpoint | Behavior | Access |
| --- | --- | --- |
| `GET /api/me` | Returns the current user | Requires `ROLE_ADMIN` |
| `GET /api/todos` | Returns paginated todos created by the current user | Requires an authenticated user |

The OpenAPI security scheme currently describes a Bearer header. Use the `x-auth` header shown above for API requests, or align the OpenAPI scheme and application security configuration before using Swagger's authorization controls.

## How to call actuator endpoints

The service exposes `health`, `info`, and `metrics`. Actuator requests require the configured probe header:

```bash
curl -H "${PROBE_HEADER}: ${PROBE_VALUE}" http://localhost:8080/actuator/health
```

Export `PROBE_HEADER` and `PROBE_VALUE` in the calling shell first, using the same values configured for the application. Replace the template Swagger password and probe value before deployment.

## How to run shell commands

Gradle connects the application only to pipes, not to a terminal, so the shell started by `bootRun` has no Tab completion, history or line editing. For an interactive session, run the JAR directly in your terminal:

```bash
./gradlew bootJar
scripts/shell.sh
```

The script does not build the JAR; run `./gradlew bootJar` (or `bootRun`) after code changes. It loads the variables from `docker/.env.shell` if it exists (otherwise `docker/.env`), and starts it with only the `shell` profile, ignoring any `SPRING_PROFILES_ACTIVE` already set.

Commands print their results directly to the terminal.

### Generate a local token

```text
generate-token -u admin-1 -r ROLE_ADMIN
```

The command signs a token with the `local` issuer and prints only the token, so it can be copied directly.

| Option | Required | Default | Purpose |
| --- | --- | --- | --- |
| `-u`, `--identity` | Yes | — | Identity of an existing user; the fixtures create `admin-1` |
| `-r`, `--role` | No | `ROLE_CUSTOMER` | Role written to the token's `roles` claim |

The command fails with `User not found` if the identity does not exist in the database.

### Publish a demo message

```text
test hello
```

The command sends a message with `StreamBridge` through the `supplier-out-0` binding to `demo.queue`. The `shell` profile has no consumer; start one with the `cloud-consumer` profile to see the message logged.

## How to run tests and quality checks

Tests use the configured MySQL, MongoDB, RabbitMQ, and JWT environment variables. Controller integration tests clear application tables and load fixtures, including purging fixture data in the configured databases. Use dedicated test databases and override `MYSQL_DB` and `MONGO_URI` in the process environment before running them.

Tests always run with the `test` profile (which also includes `cloud-consumer`), whatever `springProfile` or `SPRING_PROFILES_ACTIVE` is set to; the fixtures they load exist only with the `test` and `shell` profiles. Gradle loads `docker/.env.test` if it exists, otherwise `docker/.env`.

Run the complete test suite with:

```bash
./gradlew test
```

`test` always generates the JaCoCo coverage report afterwards, including when tests fail.

Run formatting, tests, and coverage verification with:

```bash
./gradlew check
```

`check` runs in this order: the Spotless formatting check, the tests, the coverage report, and the coverage verification. A formatting error fails the build before the slow test suite starts, and the coverage report is available even when the coverage threshold is not met.

The minimum coverage (50%) and the packages excluded from coverage (`command`, `configuration`, and `fixture`) are configured in `settings.gradle.kts`.

Other useful tasks:

```bash
./gradlew spotlessCheck       # verify Kotlin and Gradle Kotlin formatting
./gradlew spotlessApply       # apply formatting fixes
./gradlew jacocoTestReport    # generate HTML and XML coverage reports
./gradlew installGitHook      # install the pre-commit formatting hook
```

The HTML coverage report is written to:

```text
build/reports/jacoco/test/html/index.html
```

### How to install the pre-commit hook

```bash
./gradlew installGitHook
```

The task copies `scripts/pre-commit.sh` into the repository's Git hooks directory as `pre-commit`. It works in Git worktrees and respects a custom `core.hooksPath`. Before each commit, the hook runs `spotlessCheck`; if formatting fails, it runs `spotlessApply` and aborts the commit so you can review and stage the fixes.

Run the task again after `scripts/pre-commit.sh` changes. It overwrites any existing `pre-commit` hook.

## How to build and run with Docker

Build the application JAR first, then build the image from the repository root:

```bash
./gradlew bootJar
docker build -f docker/Dockerfile -t skeleton-service .
```

The Dockerfile copies the JAR and startup script; JWT keys are supplied as encoded environment values at runtime.

It also copies `scripts/shell.sh`, which starts a second JVM with only the `shell` profile, using the container's environment:

```bash
docker exec -it <container> scripts/shell.sh
```

The supplied `docker/prod-compose.yml` is a deployment template. Replace `your-image`, select the intended Spring profile (the template uses `dev`), and add `BASIC_AUTH_USER`, `BASIC_AUTH_PASSWORD`, `PROBE_HEADER`, and `PROBE_VALUE` to the backend's `environment` mapping. Provide all referenced variables, including the encoded JWT keys, and ensure the container can reach MySQL, MongoDB, and RabbitMQ. Hostnames such as `localhost` inside the container refer to the container itself.

Pass the environment file explicitly when starting Compose from the repository root:

```bash
docker compose --env-file docker/.env -f docker/prod-compose.yml up
```

## Project structure

```text
src/main/kotlin/io/kostack/skeleton/
├── cloud/stream/       RabbitMQ consumer functions (cloud-consumer profile)
├── command/            Spring Shell commands
├── configuration/      Security, database, JSON, and OpenAPI configuration
├── controller/         WebFlux REST controllers and DTOs
├── db/                 MongoDB and R2DBC models and repositories
├── fixture/            Development and test data fixtures
├── security/           Authorization rules and audit support
└── service/            Application services

src/main/resources/
├── db/migration/       Flyway SQL migrations
└── application*.yml    Base and profile-specific configuration
```

## How to extend the skeleton

1. Rename the project and package from `skeleton` to your service name.
2. Replace the example `Todo` domain, fixtures, controllers, and tests.
3. Add new Flyway migrations instead of editing an already-applied migration.
4. Replace all embedded development credentials and probe values with external configuration.
5. Update the RabbitMQ producer bindings (`application.yml`) and consumer functions (`application-cloud-consumer.yml`) for your service events.
6. Add tests for new authorization rules and API behavior.
7. Install the pre-commit hook with `./gradlew installGitHook`, and run `./gradlew check` before committing.
