# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a **Relay Server** that acts as an intermediary between a backend service and multiple AI servers (Analysis AI, Image Generation AI, RAG AI). It handles request/response transformations, AI server communication via Spring WebFlux, and image uploads to AWS S3.

**Tech Stack:** Spring Boot 3.2.0, Java 17, Spring WebFlux, AWS SDK S3, Lombok

## Build & Run Commands

```bash
# Build the project
./gradlew build

# Run the server (port 8081)
./gradlew bootRun

# Run tests
./gradlew test

# Clean build artifacts
./gradlew clean
```

## Environment Configuration

The application requires environment variables defined in `.env`:

```bash
AI_ANALYSIS_URL=http://localhost:8000
AI_IMAGE_GENERATION_URL=http://localhost:8001
AI_RAG_URL=http://localhost:8002
AWS_S3_BUCKET=your-bucket-name
AWS_S3_REGION=ap-northeast-2
AWS_ACCESS_KEY=your-access-key
AWS_SECRET_KEY=your-secret-key
```

**Note:** Environment variables must be loaded manually when running. IntelliJ users should add them in Run Configurations. Terminal users should export them before running `./gradlew bootRun`.

## Architecture

### Three-Tier Communication Pattern

The relay server implements a three-service client pattern:

1. **AnalysisAiClient** (`src/main/java/com/story/relay/service/AnalysisAiClient.java`)
   - Communicates with Analysis AI server (port 8000)
   - Handles `/analyze` (novel text analysis, 3min timeout) and `/generate` (story generation, 10min timeout)
   - Returns raw `Map<String, Object>` responses

2. **ImageGenerationAiClient** (`src/main/java/com/story/relay/service/ImageGenerationAiClient.java`)
   - Communicates with Image Generation AI server (port 8001)
   - **Currently in MOCK mode** - returns placeholder images until AI server is ready
   - Will generate images and upload to S3 via `S3UploadService` when activated
   - Timeout: 30 seconds

3. **RagAiClient** (`src/main/java/com/story/relay/service/RagAiClient.java`)
   - Communicates with RAG server (port 8002) for character-based chatbot
   - Handles character indexing and message exchange
   - Implements fallback responses when RAG server is unavailable
   - Timeout: 10 seconds

### WebClient Configuration

All AI clients use dedicated `WebClient` beans configured in `WebClientConfig.java`:
- Each client has independent timeout settings (connection, read, write)
- Configured with Netty's `HttpClient` for reactive communication
- Timeouts are environment-specific and defined in `application.yml`

### S3 Integration

`S3UploadService` handles image uploads:
- Uses AWS SDK v2 S3Client (sync client, not async)
- Uploads PNG images with `image/png` content type
- Returns public S3 URLs in format: `https://{bucket}.s3.{region}.amazonaws.com/{key}`

### Controller Layer

`AiController` (`src/main/java/com/story/relay/controller/AiController.java`) exposes REST endpoints:
- `/ai/analyze` - Novel analysis
- `/ai/generate` - Story generation
- `/ai/generate-image` - Image generation (mock)
- `/ai/chat/index-character` - Index character for RAG
- `/ai/chat/message` - Send chat message to character
- `/ai/health` - Health check for all AI servers

## Key Implementation Details

### Timeout Strategy
- Analysis AI: 10 minutes (story generation is compute-intensive)
- Image Generation: 30 seconds
- RAG Server: 10 seconds
- All configured via `application.yml` under `ai-servers.<service>.timeout`

### Error Handling Pattern
All AI clients use `.onErrorResume()` with logging and throw `RuntimeException` with descriptive messages. The RAG client additionally implements graceful fallback responses.

### Mock Mode
`ImageGenerationAiClient.generateImage()` currently calls `generateMockImage()`. To activate real image generation:
1. Uncomment the commented-out implementation in `ImageGenerationAiClient.java:35-62`
2. Remove the mock method call on line 32
3. Ensure Image Generation AI server is running and responding at `/generate-image`

### DTO Patterns
- Request/Response DTOs use Lombok's `@Data` or `@Builder`
- Located in `src/main/java/com/story/relay/dto/`
- Image generation uses strongly-typed DTOs, while analysis AI uses generic `Map<String, Object>` for flexibility

## Testing

Health check all services:
```bash
curl http://localhost:8081/ai/health
```

Test image generation (mock):
```bash
curl -X POST http://localhost:8081/ai/generate-image \
  -H "Content-Type: application/json" \
  -d '{"nodeText":"어두운 복도","situation":"긴장감","episodeTitle":"첫 만남"}'
```

## Common Issues

1. **AWS S3 Upload Failures**: Verify AWS credentials in `.env` and ensure S3 bucket has PutObject permissions
2. **AI Server Connection Failures**: Check that AI servers are running and URLs in `.env` match actual ports
3. **Timeout Errors**: Adjust timeout values in `application.yml` under `ai-servers.<service>.timeout`

## Development Notes

- The server runs on port 8081 (configured in `application.yml`)
- CORS is enabled for all origins (`@CrossOrigin(origins = "*")`)
- Logging is set to DEBUG level for `com.story.relay` package
- Uses blocking `.block()` calls with WebFlux - this is intentional for simplicity in relay operations
