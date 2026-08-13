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

## How to configure the application

Copy the environment template:

```bash
cp docker/.env.dist docker/.env
```

`bootRun`, `runJar`, and the test tasks automatically load variables from `docker/.env`. Configure these values for your local services:

| Variable | Example | Purpose |
| --- | --- | --- |
| `MONGO_URI` | `mongodb://localhost:27017/skeleton?authSource=admin` | MongoDB connection URI |
| `MYSQL_URL` | `mysql://localhost:3306` | MySQL host URL without the database name |
| `MYSQL_DB` | `skeleton` | MySQL database/schema |
| `MYSQL_USER` | `root` | MySQL user |
| `MYSQL_PASSWORD` | `root` | MySQL password |
| `RABBITMQ_HOST` | `localhost` | RabbitMQ host |
| `RABBITMQ_USER` | `guest` | RabbitMQ user |
| `RABBITMQ_PASS` | `guest` | RabbitMQ password |
| `RABBITMQ_PORT` | `5672` | RabbitMQ AMQP port |
| `JWT_PRIVATE_KEY` | `src/main/resources/jwt/private.pem` | RSA private-key path |
| `JWT_PUBLIC_KEY` | `src/main/resources/jwt/public.pem` | RSA public-key path |

Create the MySQL database before starting the service:

```sql
CREATE DATABASE skeleton;
```

Flyway applies migrations from `src/main/resources/db/migration` when the application starts.

### How to generate JWT keys

Create a local RSA key pair at the paths used by `docker/.env.dist`:

```bash
mkdir -p src/main/resources/jwt
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 \
  -out src/main/resources/jwt/private.pem
openssl rsa -pubout \
  -in src/main/resources/jwt/private.pem \
  -out src/main/resources/jwt/public.pem
```

Keep private keys out of version control. In deployed environments, mount keys or provide paths through secret management.

## How to run the service

Start the required databases and RabbitMQ, then run:

```bash
./gradlew bootRun
```

The default Spring profile is `dev`, and the HTTP server listens on port `8080` unless overridden.

### How to run the executable JAR

Use the custom `runJar` task to build and immediately start the executable Spring Boot JAR:

```bash
./gradlew runJar
```

`runJar` performs the following steps:

1. Runs `bootJar` and creates `build/libs/app.jar`.
2. Loads environment variables from `docker/.env`.
3. Starts the JAR with the Java 25 toolchain.
4. Connects standard input, output, and error to the current terminal.

Set the active Spring profile in the environment when running the task:

```bash
SPRING_PROFILES_ACTIVE=staging ./gradlew runJar
```

You can also build the JAR without starting it:

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

## How to use the API documentation

With the `dev` or `staging` profile, open:

```text
http://localhost:8080/docs
```

Swagger resources use HTTP Basic authentication:

- Username: `staging`
- Password: `z3xafXjfgUpN`

The API itself does not use these credentials. Routes under `/api/**` require a valid JWT using the configured authorization header, normally:

```http
Authorization: Bearer <token>
```

Example requests:

```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/me

curl -G -H "Authorization: Bearer $TOKEN" \
  --data-urlencode "completed=false" \
  --data-urlencode "sort=title,desc" \
  http://localhost:8080/api/todos
```

Tokens may also be supplied through the `token` query parameter, as enabled in `application.yml`.

## How to call actuator endpoints

The service exposes `health`, `info`, and `metrics`. Actuator requests require the configured probe header:

```bash
curl -H "x-k8s: secret" http://localhost:8080/actuator/health
```

Do not retain the example Swagger password or probe secret in a production application. Move them to environment-backed configuration before deployment.

## How to run the shell command

The `shell` profile disables the web server and starts Spring Shell:

```bash
./gradlew bootRun --args='--spring.profiles.active=shell'
```

At the shell prompt, publish a demo message to the configured RabbitMQ destination with:

```text
test hello
```

The command sends a message through the `supplier-out-0` binding to `demo.queue`.

## How to run tests and quality checks

Tests use the same infrastructure and JWT environment variables as local development. Run the complete test suite with:

```bash
./gradlew test
```

Run formatting, tests, and the minimum 50% JaCoCo coverage verification with:

```bash
./gradlew check
```

Other useful tasks:

```bash
./gradlew spotlessCheck       # verify Kotlin and Gradle Kotlin formatting
./gradlew spotlessApply       # apply formatting fixes
./gradlew jacocoTestReport    # generate HTML and XML coverage reports
```

The HTML coverage report is written to:

```text
build/reports/jacoco/test/html/index.html
```

## How to build and run with Docker

Build the application JAR first, then build the image from the repository root:

```bash
./gradlew bootJar
docker build -f docker/Dockerfile -t skeleton-service .
```

The Dockerfile expects these key files to exist while building:

```text
src/main/resources/jwt/private.pem
src/main/resources/jwt/prod/public.pem
```

The supplied `docker/prod-compose.yml` is a deployment template. Replace `your-image`, provide all referenced environment variables, and ensure the container can reach MySQL, MongoDB, and RabbitMQ before running it:

```bash
docker compose -f docker/prod-compose.yml up
```

## Project structure

```text
src/main/kotlin/io/kostack/skeleton/
├── cloud/stream/       RabbitMQ consumer configuration
├── command/            Spring Shell commands
├── configuration/      Security, database, JSON, and OpenAPI configuration
├── controller/         WebFlux REST controllers and DTOs
├── db/                 MongoDB and R2DBC models and repositories
├── fixture/            Development and test data fixtures
├── security/           Authorization rules and audit support
└── service/            Application services

src/main/resources/
├── db/migration/       Flyway SQL migrations
├── jwt/                Local key-file location (keys are not committed)
└── application*.yml    Base and profile-specific configuration
```

## How to extend the skeleton

1. Rename the project and package from `skeleton` to your service name.
2. Replace the example `Todo` domain, fixtures, controllers, and tests.
3. Add new Flyway migrations instead of editing an already-applied migration.
4. Replace all embedded development credentials and probe values with external configuration.
5. Update the RabbitMQ bindings and consumer functions for your service events.
6. Add tests for new authorization rules and API behavior.
7. Run `./gradlew check` before committing.
