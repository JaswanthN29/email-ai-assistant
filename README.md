# Email AI Assistant

A Spring Boot web application for an email AI assistant built in Java.

## Project Overview

This project is a Spring Boot service named `email-ai-assistant`. It includes a simple web application structure and is designed to support an AI assistant for email workflows. The app is configured with a Telegram bot token and may expose HTTP API endpoints for integration.

## Features

- Spring Boot web application
- Uses `application.properties` for configuration
- Designed to support Telegram bot integration
- Java 21 compatible

## Tech Stack

- Java 21
- Spring Boot 3.5.0
- Maven build system
- Lombok (provided scope)

## Prerequisites

- Java 21 JDK
- Maven 3.8+ or later
- Git
- SSH access configured for GitHub (recommended)

## Setup

1. Clone the repository:

```bash
git clone git@github.com:JaswanthN29/email-ai-assistant.git
cd email-ai-assistant
```

2. Configure the application properties.

### Telegram Bot Token

The app expects a Telegram bot token in `src/main/resources/application.properties`.

> Note: Do not store secrets directly in source code. Prefer using environment variables or a secure secrets store.

## Build and Run

Build the project with Maven:

```bash
./mvnw clean package
```

Run the Spring Boot application:

```bash
./mvnw spring-boot:run
```

The app will start on the default Spring Boot port `8080` unless otherwise configured.

## Test

Run the included test suite:

```bash
./mvnw test
```

## GitHub Repository

The repository is available at:

https://github.com/JaswanthN29/email-ai-assistant

## Notes

- If you want to keep the Telegram token secure, replace the hard-coded value in `application.properties` with an environment variable reference such as `telegram.bot.token=${TELEGRAM_BOT_TOKEN}`.
- If you add more services or endpoints, document them here so the project remains easy to understand.

## Gmail API: Fetch Unread Messages

This project includes a simple Gmail integration that lists unread messages using the Gmail API.

1. Enable the Gmail API in the Google Cloud Console and create OAuth 2.0 credentials for a "Desktop app" or "Web application". Download the credentials JSON and save it as `credentials.json` in the project root (or set `google.credentials.file` in `application.properties`).

2. Configure the token directory and credentials path in `src/main/resources/application.properties` (optional):

```properties
google.credentials.file=credentials.json
google.tokens.dir=tokens
app.name=email-ai-assistant
```

3. Start the application and open the URL printed by the auth flow (the first time you run it the OAuth flow will open a browser to authorize access). Then access the endpoint:

```
GET http://localhost:8080/gmail/unread
```

The endpoint returns a JSON array of unread messages with basic fields (`id`, `threadId`, `from`, `subject`, `snippet`, `body`).

Security note: The OAuth credentials and tokens allow access to your Gmail account — keep them secure and do not commit them to source control.
