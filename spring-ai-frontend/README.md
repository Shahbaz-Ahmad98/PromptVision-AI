
# PromptVision AI

### AI-Powered Creative Workspace

PromptVision AI is a full-stack AI-powered web application built using React, Spring Boot, Spring AI, and PostgreSQL.

It provides an interactive platform where users can chat with AI, generate images from text prompts, create personalized recipes, and manage their saved content.

The application features JWT-based authentication, user-specific data management, AI integration, and a responsive frontend.

## Features

### 1. AI Chat
- Interactive AI chatbot powered by Google Gemini.
- Create and manage AI conversations.
- Store chat history in PostgreSQL.
- View and delete previous conversations.
- Maintain conversation context.

### 2. AI Image Generation
- Generate images using text prompts.
- AI Horde integration for image generation.
- Standard and High quality options.
- Generate up to four images per request.
- Download generated images.
- View previously generated images.

### 3. AI Recipe Generator
- Generate recipes using available ingredients.
- Select preferred cuisine.
- Specify dietary restrictions.
- Generate personalized recipes using Google Gemini.
- Save generated recipes to PostgreSQL.
- View and delete saved recipes.

### 4. Authentication and Security
- User registration and login.
- JWT-based authentication.
- Spring Security integration.
- Protected REST API endpoints.
- User-specific chat, image, and recipe history.

## Tech Stack

### Frontend
- React.js
- JavaScript
- Vite
- HTML5
- CSS3

### Backend
- Java 25
- Spring Boot 4.1.1
- Spring AI 2.0.1
- Spring Security
- JWT Authentication
- Spring Data JPA
- Hibernate
- REST APIs
- Maven

### Database
- PostgreSQL

### AI Integrations
- Google Gemini - Chat and recipe generation
- AI Horde - Image generation
- NVIDIA AI - Configured backup integrations

### Testing
- JUnit 5
- Mockito
- Spring Security testing dependencies

## Project Structure

```text
Spring-AI-Demo/
|
|-- spring-ai-backend/
|   |-- src/
|   |   |-- main/
|   |   |   |-- java/
|   |   |   |-- resources/
|   |   |       |-- application.properties
|   |   |
|   |   |-- test/
|   |
|   |-- pom.xml
|   |-- mvnw.cmd
|
|-- spring-ai-frontend/
|   |-- src/
|   |   |-- components/
|   |   |   |-- AuthPanel.jsx
|   |   |   |-- ChatHistory.jsx
|   |   |   |-- ImageHistory.jsx
|   |   |   |-- MyRecipes.jsx
|   |   |
|   |   |-- App.jsx
|   |   |-- App.css
|   |   |-- main.jsx
|   |
|   |-- index.html
|   |-- package.json
|
|-- README.md
```

## Prerequisites

Install the following tools:

- Java JDK 25
- Node.js and npm
- PostgreSQL
- Git
- IntelliJ IDEA or VS Code

## Database Configuration

Create a PostgreSQL database:

```sql
CREATE DATABASE spring_ai_db;
```

Database configuration in:

`spring-ai-backend/src/main/resources/application.properties`

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/spring_ai_db
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=update
```

## Environment Variables

The backend requires environment variables for API credentials, database access, and JWT authentication.

Example for PowerShell:

```powershell
$env:GEMINI_API_KEY = "your-gemini-api-key"
$env:NVIDIA_API_KEY = "your-nvidia-api-key"
$env:DB_PASSWORD = "your-postgresql-password"
$env:JWT_SECRET = "your-secure-jwt-secret"
```

The JWT secret must be sufficiently long and randomly generated.

AI Horde can use the configured anonymous key for development or an API key supplied through:

```powershell
$env:AI_HORDE_API_KEY = "your-ai-horde-api-key"
```

Never commit real API keys, database passwords, or JWT secrets to GitHub.

## Run the Backend

Open PowerShell:

```powershell
cd spring-ai-backend
.\mvnw.cmd spring-boot:run
```

The Spring Boot backend runs at:

`http://localhost:8080`

## Run the Frontend

Open a second terminal:

```powershell
cd spring-ai-frontend
npm install
npm run dev -- --port 5173 --strictPort
```

Open the application at:

`http://localhost:5173`

The Vite development server proxies frontend API requests to the Spring Boot backend.

## API Overview

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET/POST | /chats | Manage conversations |
| GET | /generate-image | Generate an AI image |
| GET | /generate-images | Generate multiple images |
| GET | /generate-recipe | Generate a recipe |
| GET/POST | /recipes | View and save recipes |
| DELETE | /recipes/{id} | Delete a saved recipe |

Authentication endpoints and other routes are defined in their respective Spring Boot controllers.

Protected endpoints require a valid JWT access token.

## Testing

Run all backend tests:

```powershell
cd spring-ai-backend
.\mvnw.cmd test
```

The project includes unit tests for AI chat, recipes, and conversation management using JUnit and Mockito.

Basic manual security testing has also confirmed that unauthenticated requests and malformed JWTs are rejected by protected endpoints.

## Security

- JWT-based API authentication.
- Password handling through Spring Security.
- User-specific data access.
- Protected chat and recipe endpoints.
- API credentials supplied through environment variables.
- Database-managed user data.

## Project Purpose

PromptVision AI demonstrates practical full-stack development with AI integration.

It combines secure backend development, REST API design, React frontend development, relational database management, and external AI services in a single application.

The project is designed for academic demonstration, hands-on learning, and a Java Developer portfolio.

## Author

Md Shahbaz Ahmad

B.Tech - Computer Science and Engineering

Java Developer

GitHub: https://github.com/Shahbaz-Ahmad98

## License

No license has been specified for this project.
```
