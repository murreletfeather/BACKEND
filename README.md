# Spring Boot Demo Project

This is a basic Spring Boot project that includes:

- Spring Web
- Basic configuration
- Sample REST controller

## Requirements

- Java 21
- Maven 3.6+

## How to Run

1. Clone the repository
2. Navigate to the project directory
3. Run the following command:
   ```bash
   mvn spring-boot:run
   ```

## Testing the Application

Once the application is running, you can test it by visiting:
http://localhost:8080

You should see the message "Welcome to Spring Boot!"

## Project Structure

```
src
├── main
│   ├── java
│   │   └── com
│   │       └── example
│   │           └── demo
│   │               ├── DemoApplication.java
│   │               └── controller
│   │                   └── HelloController.java
│   └── resources
│       └── application.properties
```
