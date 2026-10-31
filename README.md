# PromptVision AI

### Full-Stack Generative AI Studio | Spring Boot + Spring AI + React

PromptVision AI is a full-stack Generative AI application built using Java, Spring Boot, Spring AI, React.js, and PostgreSQL. The platform integrates AI-powered image generation, intelligent chat, recipe assistance, user authentication, and history management into a single application.

## Features

- **AI Image Generation:** Generate images from text prompts.
- **Image History:** Access previously generated images.
- **AI Chat Assistant:** Interact with an AI-powered chatbot.
- **Chat History:** Store and retrieve previous conversations.
- **AI Recipe Assistant:** Generate recipe suggestions using AI.
- **Saved Recipes:** Save and manage recipes.
- **User Authentication:** Registration and login using Spring Security and JWT.
- **RESTful APIs:** Backend communication with the React frontend.
- **Database Persistence:** Store user information, chat history, image metadata, and saved recipes.
- **Exception Handling:** Centralized handling of backend errors.

AI services require a configured provider and sufficient API credits.

## Tech Stack

| Category | Technologies |
| --- | --- |
| Languages | Java, JavaScript, SQL |
| Backend | Spring Boot, Spring MVC, Spring AI |
| Frontend | React.js, Vite, HTML, CSS |
| Security | Spring Security, JWT |
| Persistence | Spring Data JPA, Hibernate |
| Database | PostgreSQL |
| APIs | RESTful APIs |
| Testing | JUnit, Spring Boot Test, Postman |
| Build Tools | Maven, npm |
| Development Tools | IntelliJ IDEA, VS Code, Git, GitHub |

## System Architecture

```text
                 React.js Frontend
                        |
                   REST API
                        |
                Spring Boot Backend
                        |
          +-------------+-------------+
          |             |             |
       Spring AI   Spring Security  Spring Data JPA
          |             |             |
     AI Provider   JWT Authentication PostgreSQL
```

## Project Structure

```text
PromptVision-AI/
|
+-- promptvision-ai-backend/
|   +-- .mvn/
|   +-- mvnw
|   +-- mvnw.cmd
|   +-- pom.xml
|   +-- src/
|       +-- main/
|       |   +-- java/com/ai/SpringAiDemo/
|       |   |   +-- GenAIController.java
|       |   |   +-- ChatService.java
|       |   |   +-- ImageService.java
|       |   |   +-- ImageHistoryService.java
|       |   |   +-- RecipeService.java
|       |   |   +-- auth/
|       |   |   +-- chat/
|       |   |   +-- config/
|       |   |   +-- recipe/
|       |   |   +-- user/
|       |   +-- resources/application.properties
|       +-- test/
|
+-- promptvision-ai-frontend/
|   +-- public/
|   +-- src/
|   |   +-- components/
|   |   |   +-- AuthPanel.jsx
|   |   |   +-- ChatHistory.jsx
|   |   |   +-- ImageHistory.jsx
|   |   |   +-- MyRecipes.jsx
|   |   +-- App.jsx
|   |   +-- App.css
|   |   +-- main.jsx
|   |   +-- index.css
|   +-- package.json
|   +-- vite.config.js
|
+-- README.md
```

## Getting Started

### Prerequisites

- Java JDK compatible with pom.xml
- Node.js and npm
- Maven or Maven Wrapper
- PostgreSQL (when enabled in configuration)
- Git
- AI provider API key with sufficient credits

### 1. Clone Repository

```bash
git clone https://github.com/Shahbaz-Ahmad98/PromptVision-AI.git
cd PromptVision-AI
```

### 2. Backend Configuration

Open:

```text
promptvision-ai-backend/src/main/resources/application.properties
```

Configure the database connection, AI provider, and application security settings according to the project configuration.

For an OpenAI integration, an API key may be supplied using an environment variable:

```powershell
$env:OPENAI_API_KEY = "your-api-key"
```

Never commit API keys, JWT secrets, or database passwords.

### 3. Run Backend

```powershell
cd promptvision-ai-backend
.\mvnw.cmd spring-boot:run
```

Default Spring Boot server address, unless configured otherwise:

```text
http://localhost:8080
```

### 4. Run Frontend

Open another terminal in the project root:

```powershell
cd promptvision-ai-frontend
npm install
npm run dev
```

Open the local URL printed by Vite. The default Vite port is 5173 unless overridden in vite.config.js.

## Backend Modules

### AI Services

- `GenAIController` — Handles AI-related API requests.
- `ChatService` — Processes AI chat requests.
- `ImageService` — Integrates AI image generation.
- `RecipeService` — Handles AI-powered recipe requests.

### Authentication and Security

- `AuthController` — Authentication endpoints.
- `AuthService` — Registration and login logic.
- `JwtService` — JWT operations.
- `SecurityConfig` — Spring Security configuration.

### Database and History

- `GeneratedImage` — Image metadata entity.
- `GeneratedImageRepository` — Image persistence.
- `ImageHistoryService` — Image history operations.
- `ChatConversation` — Conversation entity.
- `ChatMessage` — Chat message entity.
- `ChatHistoryService` — Conversation history operations.
- `SavedRecipe` — Saved recipe entity.
- `SavedRecipeRepository` — Recipe persistence.

## Testing

Backend tests are included for chat functionality, recipes, chat history, saved recipes, and application startup.

Run backend tests:

```powershell
cd promptvision-ai-backend
.\mvnw.cmd test
```

Run frontend linting if available:

```powershell
cd promptvision-ai-frontend
npm run lint
```

Postman can be used to test backend REST endpoints. Consult controller mappings for exact URLs and request bodies.

## Security Best Practices

- Never expose API keys or database passwords.
- Use environment variables for secrets.
- Protect authenticated endpoints using Spring Security.
- Validate input data on backend endpoints.
- Configure CORS for trusted frontend origins.
- Keep generated uploads and local build artifacts out of Git.

## Known Limitations

- AI image generation and chat depend on model provider availability and valid credentials.
- API requests may fail if the provider quota or credits are exhausted.
- Database-backed features require an accessible database.
- Verify environment variables and local configuration before running.

## Author

**Md Shahbaz Ahmad**

GitHub: [Shahbaz-Ahmad98](https://github.com/Shahbaz-Ahmad98)

## GitHub Repository

[PromptVision AI](https://github.com/Shahbaz-Ahmad98/PromptVision-AI)

---

Developed as a full-stack Generative AI project using Java, Spring Boot, Spring AI, React.js, and PostgreSQL.
