# Skeleton Service

A Kotlin and Spring Boot skeleton for building reactive services with WebFlux, JWT authentication, MongoDB, MySQL/R2DBC, Flyway, RabbitMQ, OpenAPI, and Spring Shell.

## Technology stack

- Java 25 and Kotlin
- Spring Boot with WebFlux and coroutines
- Spring Security with RSA-signed JWTs
- Reactive MongoDB and MySQL/R2DBC
- Flyway database migrations
- Spring Cloud Stream with RabbitMQ
- OpenAPI with a Scalar API reference
- JUnit 5, MockK, Reactor Test, JaCoCo, and Spotless

## Prerequisites

Install or provide:

- JDK 25 (the Gradle toolchain can also resolve a compatible JDK when toolchain download repositories are configured)
- MySQL running on port `3306`
- MongoDB running on port `27017`
- RabbitMQ running on port `5672`
- OpenSSL, if you need to generate local JWT keys

The repository includes the Gradle wrapper, so a separate Gradle installation is not required.

### Build conventions

The build setup (plugins, Java toolchain, shared dependencies, Spotless, JaCoCo, `bootRun`, `bootRunConsumer`, `installGitHook`) comes from the [kostack gradle-conventions](https://github.com/kostack/gradle-conventions) plugins. `settings.gradle.kts` applies `io.github.kostack.settings`, and `build.gradle.kts` applies `io.github.kostack.spring-service` and adds only what is specific to this service (MongoDB, MySQL/R2DBC, Flyway, logback-gelf). To upgrade the shared setup, change the `io.github.kostack.settings` version in `settings.gradle.kts`. Plugins are resolved from Maven Local first, then Maven Central and the Gradle Plugin Portal, so a version published locally with `publishToMavenLocal` (for example a release candidate) takes precedence.

The plugins are published to Maven Central, which `settings.gradle.kts` lists in its `pluginManagement` repositories.

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

Start the application with the Gradle `bootRun` task (Gradle tool window → **Tasks → application → bootRun**, or `./gradlew bootRun` in the terminal). You can also use the run icon next to `main()` in `Application.kt` if the run configuration sets `SPRING_PROFILES_ACTIVE=dev` and its working directory is the project root: the `dev` profile imports `docker/.env` itself. Other profiles do not import the file, so their run configurations need the variables in the **Environment variables** field.

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
| `MANAGEMENT_SERVER_PORT` | `8081` | Actuator port (health, info, metrics); keep it unreachable from the public network |
| `LOGGING_LEVEL` | `INFO` | Root log level (`TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`, `OFF`); defaults to `ERROR` when unset |
| `MONGO_URI` | `mongodb://localhost:27017/skeleton?authSource=admin` | MongoDB connection URI |
| `MYSQL_URL` | `mysql://localhost:3306` | MySQL host URL without the database name |
| `MYSQL_DB` | `skeleton` | MySQL database/schema |
| `MYSQL_USER` | `root` | MySQL user |
| `MYSQL_PASSWORD` | `root` | MySQL password |
| `RABBITMQ_HOST` | `localhost` | RabbitMQ host |
| `RABBITMQ_USER` | `guest` | RabbitMQ user |
| `RABBITMQ_PASS` | `guest` | RabbitMQ password |
| `RABBITMQ_PORT` | `5672` | RabbitMQ AMQP port |
| `BASIC_AUTH_USER` | `staging` | API docs HTTP Basic username |
| `BASIC_AUTH_PASSWORD` | `set_pass` | API docs HTTP Basic password; replace the template value. Required (with `BASIC_AUTH_USER`) only for the `dev` and `staging` profiles |
| `CORS_ALLOWED_ORIGINS` | `https://app.example.com,https://*.example.com` | Comma-separated CORS origin patterns; empty allows no cross-origin requests. The `dev` profile allows `http://localhost:*` and `null` itself |
| `USER_SERVICE_JWKS_URI` | `http://user-service.local/.well-known/jwks.json` | JWKS of the `user-service.local` trusted issuer; use HTTPS outside local development |
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

Gradle selects the `dev` profile unless another is given. `application.yml` defines no default profile, so `java -jar` without `SPRING_PROFILES_ACTIVE` runs with none (strict CORS, no API docs authentication chain). The HTTP server listens on `SERVER_PORT` from `docker/.env` (default `8080`).

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

- `application-dev.yml`
- `application-test.yml` (in `src/test/resources`, so it is not packaged into the JAR)
- `application-shell.yml`
- `application-staging.yml`
- `application-production.yml`
- `application-cloud-consumer.yml`

### RabbitMQ producers and consumers

Producer bindings are part of `application.yml` and apply to every profile, so `StreamBridge` can publish from any of them. For example, `demo-out` sends to `demo.queue`.

Consumers belong to the `cloud-consumer` profile, defined in `application-cloud-consumer.yml`: the `demo` function on `demo.queue` (group `skeleton-service`), its retries, and the dead-letter queue. The `demo` bean in `cloud/stream/DemoStream.kt` is created only with this profile.

The `dev`, `staging`, and `test` profiles include `cloud-consumer` through a profile group. `shell` and `production` do not, so they publish but never consume. Add the profile explicitly when a deployment must consume, for example `SPRING_PROFILES_ACTIVE=production,cloud-consumer`.

To run only the consumer, without other profiles:

```bash
./gradlew bootRunConsumer
```

`application-cloud-consumer.yml` also sets `spring.main.web-application-type: none`, so no HTTP server starts in any profile that includes `cloud-consumer`.

When adding messaging, put new `*-out-*` producer bindings in `application.yml`, and consumer functions with their `*-in-*` bindings in `application-cloud-consumer.yml`.

## How to use the API documentation

With the `dev` or `staging` profile, open the Scalar API reference:

```text
http://localhost:8080/docs
```

The API reference (`/docs`) and the OpenAPI document (`/v3/api-docs`) use HTTP Basic authentication with `BASIC_AUTH_USER` and `BASIC_AUTH_PASSWORD` from your environment.

The API is versioned by path (`/api/v1/...`). To add a version, add it to `spring.webflux.apiversion.supported` and add `version = "2+"` handlers only for endpoints whose contract changes; handlers declared with `version = "1+"` keep serving later versions. A single OpenAPI document lists all versions, with the version filled into each path (e.g. `/api/v1/me`, `/api/v2/me`).

Routes under `/api/**` require a valid JWT. The current configuration uses the `x-auth` header with an empty prefix:

```http
x-auth: <token>
```

Example requests:

```bash
curl -H "x-auth: $TOKEN" \
  http://localhost:8080/api/v1/me

curl -G -H "x-auth: $TOKEN" \
  --data-urlencode "completed=false" \
  --data-urlencode "sort=title,desc" \
  http://localhost:8080/api/v1/todos
```

With the `dev` profile, tokens may also be supplied through the `token` query parameter. It is disabled in other profiles because query strings end up in access logs and `Referer` headers.

Tokens must use the audience `kostack`, the `RS512` signature algorithm, and one of the trusted issuers configured in `application.yml`:

| Issuer | Verified with |
| --- | --- |
| `local` | `JWT_PUBLIC_KEY`; this is also the issuer the service signs its own tokens with |
| `user-service.local` | The JWKS at `USER_SERVICE_JWKS_URI` (default `http://user-service.local/.well-known/jwks.json`) |

For local development, generate a token with the `generate-token` shell command described below. The JWT subject identifies the user, and the `roles` claim supplies their roles. `UserProviderImpl` loads an existing user by identity or creates an in-memory user when no database record exists; roles come from the token in either case.

| Endpoint | Behavior | Access |
| --- | --- | --- |
| `GET /api/v1/me` | Returns the current user | Requires `ROLE_ADMIN` |
| `GET /api/v1/todos` | Returns paginated todos created by the current user | Requires an authenticated user |

The OpenAPI security scheme currently describes a Bearer header. Use the `x-auth` header shown above for API requests, or align the OpenAPI scheme and application security configuration before using Scalar's authentication settings.

## How to call actuator endpoints

The service exposes `health`, `info`, and `metrics` on `MANAGEMENT_SERVER_PORT` (default `8081`), separate from the API port. Health endpoints need no authentication, so load balancer health checks and Kubernetes probes can call them directly; `info` and `metrics` require a JWT like the API:

```bash
curl http://localhost:8081/actuator/health
curl -H "x-auth: $TOKEN" http://localhost:8081/actuator/metrics
```

Never expose the management port publicly. Replace the template API docs password before deployment.

Health groups:

| Endpoint | Includes | Use for |
| --- | --- | --- |
| `/actuator/health/liveness` | `livenessState` | Liveness probe; never fails because of a backing service, so outages do not restart pods |
| `/actuator/health/readiness` | `readinessState`, `r2dbc`, `mongo`, `rabbit` | Readiness probe; takes the pod out of rotation while MySQL, MongoDB or RabbitMQ is unreachable |
| `/actuator/health/alb` | All of the above | Load balancer target health check |

A shared outage of one of these services marks every pod not ready at the same time, so the service returns errors from the load balancer instead of from the application until it recovers. Remove a service from the `readiness` and `alb` groups in `application.yml` if its outage should not stop traffic.

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

The command sends a message with `StreamBridge` through the `demo-out` binding to `demo.queue`. The `shell` profile has no consumer; start one with the `cloud-consumer` profile to see the message logged.

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

The minimum coverage (50%) and the packages excluded from coverage (`command`, `configuration`, and `fixture`) are defaults of the [kostack gradle-conventions](https://github.com/kostack/gradle-conventions) plugin. This project raises the minimum to 70% and also excludes generated code, in `build.gradle.kts`:

```kotlin
kostack {
  coverageMinimum = 0.7
  coverageExclusions.add("**/generated/**")
}
```

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

The task copies the pre-commit hook bundled with the gradle-conventions plugin into the repository's Git hooks directory as `pre-commit`. To use a custom hook instead, add it as `scripts/pre-commit.sh`. It works in Git worktrees and respects a custom `core.hooksPath`. Before each commit, the hook runs `spotlessCheck`; if formatting fails, it runs `spotlessApply` and aborts the commit so you can review and stage the fixes.

Run the task again after the hook changes. It overwrites any existing `pre-commit` hook.

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

The supplied `docker/prod-compose.yml` is a deployment template. Replace `your-image` and check the Spring profile (the template uses `production`, which disables the API docs). Add `BASIC_AUTH_USER` and `BASIC_AUTH_PASSWORD` to the backend's `environment` mapping only if you switch to `staging`. Provide all referenced variables, including the encoded JWT keys, and ensure the container can reach MySQL, MongoDB, and RabbitMQ. Hostnames such as `localhost` inside the container refer to the container itself.

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
4. Replace all embedded development credentials with external configuration.
5. Update the RabbitMQ producer bindings (`application.yml`) and consumer functions (`application-cloud-consumer.yml`) for your service events.
6. Add tests for new authorization rules and API behavior.
7. Install the pre-commit hook with `./gradlew installGitHook`, and run `./gradlew check` before committing.
