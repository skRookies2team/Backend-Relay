# Relay Server

AI 서버들(분석 AI, 이미지 생성 AI 등)과의 통신을 전담하는 중계 서버입니다.

## 개요

이 서버는 백엔드와 여러 AI 서버들 사이의 중계 역할을 수행합니다:
- 소설 분석 AI와 통신
- 이미지 생성 AI와 통신
- 생성된 이미지를 S3에 업로드
- 요청/응답 포맷 변환

## 기술 스택

- **Spring Boot 3.2.0**
- **Java 17**
- **Spring WebFlux** (AI 서버 통신)
- **AWS SDK S3** (이미지 업로드)
- **Lombok**

## 프로젝트 구조

```
relay-server/
├── src/main/java/com/story/relay/
│   ├── RelayServerApplication.java     # Main Application
│   ├── controller/
│   │   └── AiController.java           # REST API 엔드포인트
│   ├── service/
│   │   ├── AnalysisAiClient.java       # 분석 AI 통신
│   │   ├── ImageGenerationAiClient.java # 이미지 AI 통신
│   │   └── S3UploadService.java        # S3 업로드
│   ├── dto/
│   │   ├── ImageGenerationRequestDto.java
│   │   └── ImageGenerationResponseDto.java
│   └── config/
│       ├── WebClientConfig.java        # WebClient 설정
│       └── S3Config.java               # S3 Client 설정
└── src/main/resources/
    └── application.yml                 # 애플리케이션 설정
```

## API 엔드포인트

### 1. 소설 분석
```http
POST /ai/analyze
Content-Type: application/json

{
  "novelText": "소설 텍스트..."
}
```

### 2. 스토리 생성
```http
POST /ai/generate
Content-Type: application/json

{
  "novelText": "...",
  "selectedGaugeIds": ["hope", "trust"],
  "numEpisodes": 3,
  ...
}
```

### 3. 이미지 생성 (신규)
```http
POST /ai/generate-image
Content-Type: application/json

{
  "nodeText": "어두운 복도...",
  "situation": "긴장감 넘치는 상황",
  "npcEmotions": {
    "주인공": "긴장"
  },
  "episodeTitle": "첫 만남"
}
```

**Response:**
```json
{
  "imageUrl": "https://s3.../images/abc123.png",
  "fileKey": "images/abc123.png",
  "generatedAt": "2025-12-02T10:30:00Z"
}
```

### 4. Health Check
```http
GET /ai/health
```

## 환경 설정

### 1. .env 파일 생성

프로젝트 루트에 `.env` 파일을 생성하고 다음 값들을 설정하세요:

```bash
# AI Servers
AI_ANALYSIS_URL=http://localhost:8000
AI_IMAGE_GENERATION_URL=http://localhost:8001

# AWS S3
AWS_S3_BUCKET=your-bucket-name
AWS_S3_REGION=ap-northeast-2
AWS_ACCESS_KEY=your-access-key
AWS_SECRET_KEY=your-secret-key
```

### 2. 환경 변수 로드

**Option 1: IntelliJ IDEA**
1. Run → Edit Configurations
2. Environment variables 섹션에 `.env` 파일 내용 복사

**Option 2: 터미널에서 실행**
```bash
# Windows (PowerShell)
$env:AI_ANALYSIS_URL="http://localhost:8000"
$env:AWS_S3_BUCKET="your-bucket"
# ... 기타 변수들

# Linux/Mac
export AI_ANALYSIS_URL=http://localhost:8000
export AWS_S3_BUCKET=your-bucket
# ... 기타 변수들
```

## 빌드 및 실행

### 빌드
```bash
./gradlew build
```

### 실행
```bash
./gradlew bootRun
```

서버는 기본적으로 **포트 8081**에서 실행됩니다.

### 테스트
```bash
./gradlew test
```

## 사용 방법

### 1. Relay Server 시작
```bash
cd relay-server
./gradlew bootRun
```

### 2. Health Check 확인
```bash
curl http://localhost:8081/ai/health
```

**예상 응답:**
```json
{
  "status": "healthy",
  "relayServer": "up",
  "aiServers": {
    "analysisAi": {
      "status": "up"
    },
    "imageGenerationAi": {
      "status": "up"
    }
  }
}
```

### 3. 이미지 생성 테스트 (Mock)
```bash
curl -X POST http://localhost:8081/ai/generate-image \
  -H "Content-Type: application/json" \
  -d '{
    "nodeText": "어두운 복도",
    "situation": "긴장감",
    "episodeTitle": "첫 만남"
  }'
```

**현재는 Mock 응답을 반환합니다** (AI 서버 개발 대기 중):
```json
{
  "imageUrl": "https://via.placeholder.com/800x600/1a1a1a/ffffff?text=첫+만남",
  "fileKey": "mock/image_uuid.png",
  "generatedAt": "2025-12-02T10:30:00Z"
}
```

## AI 서버 연동

### 이미지 생성 AI 서버가 준비되면

`ImageGenerationAiClient.java` 파일에서 주석 처리된 실제 구현을 활성화하세요:

```java
// TODO 주석 제거
public ImageGenerationResponseDto generateImage(ImageGenerationRequestDto request) {
    // Mock 응답 부분 삭제
    // return generateMockImage(request);

    // 실제 구현 주석 해제
    byte[] imageBytes = imageGenerationAiWebClient.post()
        .uri("/generate-image")
        .bodyValue(request)
        .retrieve()
        .bodyToMono(byte[].class)
        .timeout(Duration.ofSeconds(30))
        .block();

    // ... 나머지 코드
}
```

## 트러블슈팅

### 1. AWS S3 연결 오류
- `.env` 파일의 AWS 자격 증명 확인
- S3 버킷 권한 확인 (PutObject 권한 필요)
- 리전 설정 확인

### 2. AI 서버 연결 실패
- AI 서버가 실행 중인지 확인
- `.env`의 AI 서버 URL 확인
- 네트워크 방화벽 확인

### 3. 타임아웃 오류
- `application.yml`의 타임아웃 설정 조정
- AI 서버 응답 시간 확인

## 개발 로드맵

- [x] Phase 1: 기본 프로젝트 구조 생성
- [x] Phase 2: WebClient 및 S3 설정
- [x] Phase 3: Mock 이미지 생성 API
- [ ] Phase 4: 분석 AI 연동 (기존 기능 이관)
- [ ] Phase 5: 실제 이미지 생성 AI 연동
- [ ] Phase 6: 에러 핸들링 강화
- [ ] Phase 7: 로깅 및 모니터링
- [ ] Phase 8: 캐싱 전략
- [ ] Phase 9: 성능 테스트
- [ ] Phase 10: 프로덕션 배포

## 참고 문서

- [RELAY_SERVER_ARCHITECTURE.md](../story-backend/docs/RELAY_SERVER_ARCHITECTURE.md) - 상세 아키텍처 설계
- [Backend Repository](../story-backend) - 메인 백엔드 서버
- [AI Server Repository](https://github.com/skRookies2team/AI/tree/feature/kwak) - Python AI 서버

## 라이선스

MIT License

# Auto Deploy Test
