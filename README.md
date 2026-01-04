# Junk Chat

Lightweight, real-time chat application prototype combining a Java backend with a JavaScript-based web client. Junk Chat is designed as a simple starting point for building chat systems, demos, or experiments in real-time messaging, presence, and UI/UX for small teams or personal projects.

> Note: This README is written to be implementation-agnostic. Replace the example commands and configuration with the concrete build/tooling choices used in this repository (Maven/Gradle, framework names, package.json scripts, etc.) if they differ.

## Table of contents
- Project overview
- Features
- Tech stack
- Setup instructions
- Folder structure
- Future enhancements
- Contributing
- License

## Project overview
Junk Chat is a small chat application that demonstrates how to build a real-time messaging system using Java on the server and a web-based client using JavaScript, CSS, and HTML. It can be used as:
- A learning project to explore WebSocket-based real-time communication.
- A minimal prototype to extend into a full production-ready chat service.
- A reference for integrating frontend and backend components in a mixed-language codebase.

## Features
- Real-time messaging between connected clients (WebSocket or Socket-based transport)
- Multi-room / channel support (basic)
- User presence (online / offline indicator)
- Simple client UI with message list, input box, and user list
- Message timestamping and lightweight metadata
- Configurable server ports and basic environment-driven configuration
- Extensible codebase for adding authentication, persistence, and moderation

## Tech stack
Languages:
- Java (primary backend language) — ~53.6% of the repository
- JavaScript (frontend interactivity) — ~32.6%
- CSS (styling) — ~13%
- HTML (markup) — ~0.8%

Typical frameworks & libraries (examples you may see or want to use):
- Backend: Spring Boot or another Java web framework (for WebSocket / REST endpoints)
- WebSockets: Java WebSocket API, Spring WebSocket, or similar
- Frontend: Plain JavaScript, or a framework like React / Vue / Svelte (if used)
- Build tools: Maven or Gradle for Java, npm / yarn for frontend
- Optional: Docker for containerization, PostgreSQL or MongoDB for persistence

## Setup instructions

Prerequisites
- Java JDK 11+ installed and JAVA_HOME configured
- Node.js 14+ and npm (if client is a separate JS project)
- Git
- (Optional) Docker and Docker Compose if using containerized setup

1. Clone the repository
```bash
git clone https://github.com/premkumar3616/junk-chat.git
cd junk-chat
```

2. Backend (Java)
- With Maven
  ```bash
  # from repository root or backend/ directory
  mvn clean package
  java -jar target/junk-chat-<version>.jar
  ```
- With Gradle
  ```bash
  ./gradlew build
  ./gradlew bootRun
  ```
- Common environment variables
  - `SERVER_PORT` — port for the backend (default: 8080)
  - `DATABASE_URL` — connection string for persistent storage (if used)
  - `JWT_SECRET` — secret for authentication tokens (if added)

3. Frontend (JavaScript)
If the frontend is a standalone client directory:
```bash
cd client
npm install
npm run dev        # run development server
# or
npm run build      # produce production build
```
If frontend is served as static files by the backend, place the build artifacts in the appropriate static resources folder (e.g., `src/main/resources/static`).

4. Running with Docker (optional)
Create a Dockerfile for the backend and optionally one for the client, then:
```bash
docker build -t premkumar3616/junk-chat .
docker run -p 8080:8080 -e SERVER_PORT=8080 premkumar3616/junk-chat
```
If using Docker Compose, provide a `docker-compose.yml` to orchestrate backend, client, and database services.

5. Open the app
- Visit http://localhost:8080 (or the configured frontend port) to access the chat UI.

Troubleshooting
- Check logs produced by the Java process for stack traces
- Verify ports are not in use by other processes
- Ensure web socket connection URLs match the backend host/port and path

## Folder structure
This is a recommended / typical structure for a Java + JavaScript chat project. Adjust to match this repository’s actual layout.

- / (repo root)
  - README.md
  - LICENSE
  - .gitignore
  - backend/ or server/
    - pom.xml or build.gradle
    - src/
      - main/
        - java/           -> Java source code
        - resources/      -> application.properties, static resources
    - Dockerfile
  - client/ or web/
    - package.json
    - public/             -> index.html and static assets (if SPA)
    - src/                -> JS, CSS, components
    - build/ or dist/     -> compiled frontend artifacts
  - docs/                 -> design notes, API docs, sequence diagrams
  - scripts/              -> helper scripts (start, stop, deploy)
  - .github/              -> CI/CD workflows and issue templates

Example tree
```
/junk-chat
├─ backend/
│  ├─ src/main/java/...
│  ├─ src/main/resources/application.properties
│  └─ pom.xml
├─ client/
│  ├─ src/
│  └─ package.json
├─ docs/
└─ README.md
```

## Future enhancements
Potential improvements and next steps you may consider:
- Authentication & Authorization
  - Add username/password sign-in, OAuth, or SSO
  - Integrate JWT-based auth and protected channels
- Persistent storage
  - Save messages to a database (Postgres, MongoDB)
  - Implement message history and search
- Scalability & reliability
  - Horizontal scaling of backend with stateless servers and a message broker (Redis Pub/Sub, Kafka)
  - Use sticky sessions or centralized session management for WebSockets
- Advanced client features
  - Typing indicators, read receipts, message editing & deletion
  - File/image attachments and previews
  - Responsive UI and accessibility improvements
- Moderation & safety
  - Profanity filtering, user reporting, rate limiting
- Tests & CI
  - Unit and integration tests for backend and frontend
  - Add GitHub Actions for build/test/deploy pipelines
- Deployment
  - Provide Helm charts or Terraform configs for Kubernetes deployment
  - Provide production-grade Docker Compose / Dockerfile optimizations

## Contributing
Contributions are welcome. To contribute:
1. Fork the repository
2. Create a topic branch: `git checkout -b feat/your-feature`
3. Commit your changes and push to your fork
4. Open a pull request describing your changes and rationale

Please follow code style conventions and include tests where appropriate.

## License
Specify your license here (e.g., MIT, Apache-2.0). If you do not yet have a license file in the repository, add one and update this section.

---

If you’d like, I can:
- Draft a Dockerfile and docker-compose.yml for a standard backend+frontend setup
- Create a basic Spring Boot skeleton or a minimal frontend (React/Vite) scaffold
- Add a sample configuration file (application.properties / .env) for quick start

Tell me which of those you'd like next and I’ll generate the files. 