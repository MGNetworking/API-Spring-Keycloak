# API

This project is an example to implementation with Spring With Keycloak and Database H2

NB: To use this project, you need a running Keycloak identity manager.

## Speed access

Launch of Spring Sans profile

````bash
./mvnw spring-boot:run
````

Launching unit tests

````shell
# linux
./mvnw clean test -Dspring.profiles.active=test
````

````bash
# Windows
./mvnw clean test -D spring.profiles.active=test
````

[H2 Access IP](http://localhost:8080/h2-console)

le mot de passe : ``password``

# Maven Wrapper: How It Works

## What is Maven Wrapper?

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