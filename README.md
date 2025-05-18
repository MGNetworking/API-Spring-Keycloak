# API Spring Keycloak

This project is an example to implementation with Spring With Keycloak and Database H2

NB: You can run the unit tests to get an idea of how the project works. But to use this project,
you need a running Keycloak

### Sommaire

* [Run projet](#run-projet)
* [Run test](#run-test)
* [Security Configuration](#security-configuration)
* [Endpoints Principaux](#endpoints-principaux)
    * [Gestion Utilisateurs](#gestion-utilisateurs)
    * [Authentification](#authentification)
    * [Administration](#administration)
* [Codes de Réponse HTTP](#codes-de-réponse-http)
* [Maven Wrapper: How It Works](#maven-wrapper-how-it-works)
    * [What is Maven Wrapper?](#what-is-maven-wrapper)
    * [How it works](#how-it-works)
    * [Benefits](#benefits)
    * [Common Usage](#common-usage)
* [Explanation of Test Annotations](#-explanation-of-test-annotations)

## Run projet

Launch of Spring without profile

````bash
./mvnw spring-boot:run
````

## Run test

Launching unit tests with profile.

* Linux :

````shell
./mvnw clean test -Dspring.profiles.active=test
````

* Windows :

````bash
./mvnw clean test -D spring.profiles.active=test
````

Access browser:

* [Database H2 Access IP](http://localhost:8080/h2-console)
* [Doc Swagger IP](#http://localhost:8080/swagger-ui/index.html")

Database H2 password: ``password``

## Security Configuration

Security is managed through the `SecurityConfig` class, with the following features:

- **CSRF Disabled**: Since the API is stateless.
- **JWT Configuration**: Roles are extracted from Keycloak tokens.
- **Authorization Rules**:
    - Public access is allowed for authentication and documentation endpoints.
    - Access to `/api/v1/admin/**` is restricted to users with the `ROLE_ADMIN` authority.
    - Authentication is required for all other endpoints.

## Endpoints Principaux

### Gestion Utilisateurs

* class : UsersController

| Endpoint               | Méthode | Description              | Rôle Requis | Codes Réponse                     |
|------------------------|---------|--------------------------|-------------|-----------------------------------|
| /api/v1/users/register | POST    | Création d'utilisateur   | PUBLIC      | 201, 400, 401, 403, 409, 500      |
| /api/v1/users/user     | PUT     | Mise à jour utilisateur  | ROLE_USER   | 200, 400, 401, 403, 404, 500      |
| /api/v1/users/{id}     | DELETE  | Suppression utilisateur  | ROLE_ADMIN  | 204, 400, 401, 403, 404, 409, 500 |
| /api/v1/users/{id}     | GET     | Récupération utilisateur | ROLE_USER   | 200, 400, 401, 403, 404, 500      |

### Authentification

* class : AuthController

| Endpoint             | Méthode | Description            | Rôle Requis | Codes Réponse                |
|----------------------|---------|------------------------|-------------|------------------------------|
| /api/v1/auth/login   | POST    | Connexion (JWT)        | PUBLIC      | 200, 400, 401, 403, 404, 500 |
| /api/v1/auth/logout  | POST    | Déconnexion            | ROLE_USER   | 200, 400, 401, 403, 404, 500 |
| /api/v1/auth/refresh | POST    | Rafraîchissement token | ROLE_USER   | 200, 400, 401, 403, 404, 500 |

### Administration

* class : AdminController

| Endpoint                 | Méthode | Description                    | Rôle Requis | Codes Réponse |
|--------------------------|---------|--------------------------------|-------------|---------------|
| /api/v1/admin/users      | GET     | Liste tous les utilisateurs    | ROLE_ADMIN  | 200           |
| /api/v1/admin/users/{id} | PUT     | Modification admin utilisateur | ROLE_ADMIN  | 200, 404      |

## Codes de Réponse HTTP

The API uses the following standard HTTP status codes:

| Code | Description                                                                      |
|------|----------------------------------------------------------------------------------|
| 200  | **OK** - The request was successful                                              |
| 201  | **Created** - Successfully created resource                                      |
| 204  | **No Content** - Request processed successfully but no content to return         |
| 400  | **Bad Request** - The request contains errors or invalid data                    |
| 401  | **Unauthorized** - Authentication required or authentication failed              |
| 404  | **Not Found** - The requested resource does not exist                            |
| 409  | **Conflict** - The request cannot be processed due to a conflict (ex: duplicate) |

## Maven Wrapper: How It Works

### What is Maven Wrapper?

Maven Wrapper (`mvnw`) is a script that allows you to run Maven commands without having Maven installed on your system.

## How it works

When you execute the `./mvnw` command (or `mvnw.cmd` on Windows):

1. The Maven Wrapper script checks if Maven is already available in a local directory of the project (typically
   `.mvn/wrapper/`)

2. If Maven is not found locally, the script automatically downloads the appropriate version of Maven specified in your
   project

3. Then, the script executes the Maven command you requested using this downloaded version

## What about JDK?

* The Maven Wrapper downloads Maven, but not the JDK
* A JDK is still necessary to compile and run Java code
* If you don't have a JDK installed, you'll get an error like "JAVA_HOME is not set" or "java command not found"

## Benefits

* Ensures everyone on the team uses the same Maven version
* No need to install Maven globally on your machine
* Makes project setup easier for new developers
* Perfect for CI/CD pipelines where you want to control the Maven version

## Common Usage

```bash
# Linux/Mac
./mvnw clean install

# Windows
mvnw.cmd clean install
```

In a Spring development environment, you'll still need to install a JDK, but not necessarily Maven thanks to the
wrapper.

## Explanation of Test Annotations

Below are the main annotations used when testing Spring Boot controllers with `MockMvc`:

### `@WebMvcTest(UsersController.class)`

- Used to test only the **web layer (Controller)**.
- It does **not** load service (`@Service`) or repository (`@Repository`) beans.
- Ideal for testing HTTP endpoints in isolation without starting the full application context.

### `@ActiveProfiles("test")`

- Activates a specific Spring profile (`test` in this case).
- Useful for loading a dedicated configuration (`application-test.yml`) or customizing behavior during tests.

### `@AutoConfigureMockMvc(addFilters = false)`

- Automatically configures a `MockMvc` bean to simulate HTTP requests.
- The `addFilters = false` option disables **security filters** (e.g., Spring Security filters), making it easier to
  test controllers without authentication.

### `@TestPropertySource(properties = {"spring.sql.init.mode=never"})`

- Overrides specific configuration properties during test execution.
- In this case, disables automatic SQL database initialization by Spring Boot during tests.

---

These annotations, when combined, allow you to write fast, isolated, and database-independent controller tests.
