# API Spring Keycloak

User Management API with Profile Extension and Keycloak Integration
This project is a secure RESTful API built with Spring Boot, designed to handle user account registration, role
administration through Keycloak, and the extension of custom business data per user.

⚙️ Core Features

1. User Management (UsersController)
    * Registers users in Keycloak, which handles identity and authentication.
    * Upon registration, custom business-related data is created and stored in the API's own database, associated with
      the user’s unique ID from the Keycloak token.
    * No personal identity data (name, email, password) is stored in the API; these are managed entirely within
      Keycloak.
    * The user’s Keycloak ID (sub from the JWT token) is used to link and manipulate business-related data.

2. Administration Tools (AdminController)
    * Allows administrators to retrieve and manage user roles at the realm or client level within Keycloak.
    * Provides endpoints to assign/remove roles, enable/disable user accounts, and trigger password resets.
    * Authorization is enforced using roles extracted from the JWT token issued by Keycloak.

🔒 Security Model
Authentication is managed by Keycloak, and all endpoints are protected by role-based access control
(ROLE_USER_REALM, ROLE_ADMIN_REALM).
The JWT token is used both for authorization and to resolve the identity of the business data owner.

### Sommaire

* [Run projet](#run-projet)
* [Run test](#run-test)
* [Intellij Config](#intellij-config)
* [Security Configuration](#security-configuration)
* [Endpoints Main ](#endpoints-main)
    * [User management](#user-management)
    * [Administration](#administration)
* [Codes de Réponse HTTP](#codes-de-réponse-http)
* [Maven Wrapper: How It Works](#maven-wrapper-how-it-works)
    * [What is Maven Wrapper?](#what-is-maven-wrapper)
    * [How it works](#how-it-works)
    * [Benefits](#benefits)
    * [Common Usage](#common-usage)

## Run projet

Launch of Spring without profile

* Linux :

````shell
./mvnw spring-boot:run -Dspring-boot.run.profiles=test
````

* Windows :

````bash
./mvnw spring-boot:run -D spring-boot.run.profiles=test
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
* [Doc Swagger IP](http://localhost:8080/swagger-ui/index.html)

Database H2 password: ``password``

## Intellij Config

Here is the configuration required to launch the project in the Intellij IDE

![Mon image](img/intellij-config-01.png)

Here is an example of the configuration required to launch a test in the Intellij IDE

![Mon image](img/intellij-config-02.png)

## Security Configuration

Security is managed through the `SecurityConfig` class, with the following features:

- **CSRF Disabled**: Since the API is stateless.
- **JWT Configuration**: Roles are extracted from Keycloak tokens.
- **Authorization Rules**:
    - Public access is allowed for authentication and documentation endpoints.
    - Access to `/api/v1/admin/**` is restricted to users with the `ROLE_ADMIN` authority.
    - Authentication is required for all other endpoints.

## Endpoints Main

### User management

* class : UsersController

| Endpoint               | Méthode | Description              | Rôle Requis | Codes Réponse                     |
|------------------------|---------|--------------------------|-------------|-----------------------------------|
| /api/v1/users/register | POST    | Création d'utilisateur   | PUBLIC      | 201, 400, 401, 403, 409, 500      |
| /api/v1/users/user     | PUT     | Mise à jour utilisateur  | ROLE_USER   | 200, 400, 401, 403, 404, 500      |
| /api/v1/users/{id}     | DELETE  | Suppression utilisateur  | ROLE_USER   | 204, 400, 401, 403, 404, 409, 500 |
| /api/v1/users/{id}     | GET     | Récupération utilisateur | ROLE_USER   | 200, 400, 401, 403, 404, 500      |

📝 Description:
This controller manages user accounts. It allows anyone to register a new user, authenticated users to update or
retrieve their profile, and administrators to delete users.
Each action is protected by appropriate role-based access control (`ROLE_USER_REALM`, `ROLE_ADMIN_REALM`).

### Administration

* class : AdminController

| Endpoint                                  | Méthode | Description                                                              | Rôle Requis | Codes Réponse                |
|-------------------------------------------|---------|--------------------------------------------------------------------------|-------------|------------------------------|
| /api/v1/admin/roles/realm                 | GET     | Opérations liées à la récupération des rôles disponibles dans le realm   | ROLE_ADMIN  | 200, 401, 403,  500          |
| /api/v1/admin/roles/client                | GET     | Opérations liées à la récupération des rôles d’un client Keycloak        | ROLE_ADMIN  | 200, 401, 403,  500          |
| /api/v1/admin/users/{userId}/roles/realm  | POST    | Opérations d'ajout de rôles à un utilisateur dans le realm               | ROLE_ADMIN  | 204, 400, 401, 403, 404, 500 |
| /api/v1/admin/users/{userId}/roles/client | POST    | Opérations d'ajout de rôles à un utilisateur dans une sous domain client | ROLE_ADMIN  | 204, 400, 401, 403, 404, 500 |
| /api/v1/admin/deleteRoleRealm             | DELETE  | Supprime des rôles Realm à un utilisateur                                | ROLE_ADMIN  | 204, 400, 401, 403, 404, 500 |
| /api/v1/admin/deleteRoleClient            | DELETE  | Supprime des rôles client d’un utilisateur                               | ROLE_ADMIN  | 204, 400, 401, 403, 404, 500 |

📝 Description:

This controller provides administrative tools for managing Keycloak users.

* `/users` lists all users registered in the Keycloak realm.
* `/users/{id}` allows editing user profile details (name, email, etc.).
* `/users/roles` updates a user's role assignments (e.g., adding/removing `ROLE_USER`, `ROLE_ADMIN`).
* `/users/enable` enables or disables a user's Keycloak account.
* `/reset-password` forces a password reset with a new password, optionally marking it as temporary.

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
| 500  | **Internal Server Error** - Unexpected server-side error                         |

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