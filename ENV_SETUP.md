# 환경변수 설정 가이드

로컬 개발 환경에서 환경변수를 사용하여 설정을 관리할 수 있습니다.

## Spring Boot 3 + dotenv-java 설정

이 프로젝트는 Spring Boot 3와 `dotenv-java` 라이브러리를 사용하여 `.env` 파일을 자동으로 로드합니다.

### 자동 .env 파일 로드
- 애플리케이션 시작 시 `DotenvConfig` 클래스가 자동으로 `.env` 파일을 로드
- `.env` 파일의 변수들을 시스템 환경변수로 설정
- 기존 시스템 환경변수가 있는 경우 우선순위를 가짐
- `.env` 파일이 없어도 에러 없이 정상 동작

## 환경변수 설정 방법

### 1. 환경변수 파일 생성
프로젝트 루트에 `.env` 파일을 생성하고 다음 내용을 추가하세요:

```bash
# Database Configuration
SPRING_DATASOURCE_URL=jdbc:mysql://insightops-admin.mysql.database.azure.com:3306/insightops_dashboard?useSSL=true&serverTimezone=UTC&characterEncoding=UTF-8&allowPublicKeyRetrieval=true
SPRING_DATASOURCE_USERNAME=insightopsadmin
SPRING_DATASOURCE_PASSWORD=Strongpassword@

# Server Configuration
PORT=3002

# External MSA Services
NORMALIZATION_SERVICE_URL=http://localhost:8001
MAIL_SERVICE_URL=http://localhost:8003
DATA_INGESTION_SERVICE_URL=http://localhost:8000
VOICEBOT_SERVICE_URL=http://localhost:8002
ADMIN_SERVICE_URL=http://localhost:8004

# OpenAI API Configuration
OPENAI_API_KEY=your_openai_api_key_here
OPENAI_MODEL=gpt-4o-mini
OPENAI_MAX_TOKENS=2000
OPENAI_TEMPERATURE=0.7
OPENAI_TIMEOUT=30000

# AI Insights Configuration
AI_INSIGHTS_ENABLED=true
AI_CACHE_ENABLED=true
AI_CACHE_TTL=3600
AI_RATE_LIMIT_ENABLED=true
AI_RATE_LIMIT_RPM=10
AI_FALLBACK_ENABLED=true
AI_FALLBACK_STATISTICAL=true
```

### 2. 환경변수 로드 방법

#### 방법 1: 자동 로드 (권장)
Spring Boot 애플리케이션을 실행하면 `DotenvConfig`가 자동으로 `.env` 파일을 로드합니다:

```bash
# Maven으로 실행
./mvnw spring-boot:run

# 또는 JAR 파일로 실행
java -jar target/dashboard-backend-1.0.0.jar
```

#### 방법 2: IDE에서 실행
- IntelliJ IDEA: Run Configuration에서 `.env` 파일이 자동으로 로드됨
- VS Code: `.vscode/launch.json`에서 envFile 설정 (선택사항)

#### 방법 3: 수동 환경변수 설정 (고급 사용자)
```bash
# .env 파일을 소스로 로드
source .env

# 또는 export 명령어로 개별 설정
export SPRING_DATASOURCE_URL="your_database_url"
export SPRING_DATASOURCE_USERNAME="your_username"
export SPRING_DATASOURCE_PASSWORD="your_password"
export OPENAI_API_KEY="your_openai_api_key"
```

#### 방법 4: Spring Boot 실행 시 환경변수 지정
```bash
SPRING_DATASOURCE_URL="your_url" \
SPRING_DATASOURCE_USERNAME="your_username" \
SPRING_DATASOURCE_PASSWORD="your_password" \
OPENAI_API_KEY="your_api_key" \
./mvnw spring-boot:run
```

### 3. 주요 환경변수 설명

- `SPRING_DATASOURCE_URL`: 데이터베이스 연결 URL
- `SPRING_DATASOURCE_USERNAME`: 데이터베이스 사용자명
- `SPRING_DATASOURCE_PASSWORD`: 데이터베이스 비밀번호
- `OPENAI_API_KEY`: OpenAI API 키 (AI 인사이트 기능용)
- `PORT`: 서버 포트 (기본값: 3002)
- `NORMALIZATION_SERVICE_URL`: 정규화 서비스 URL
- `MAIL_SERVICE_URL`: 메일 서비스 URL
- `DATA_INGESTION_SERVICE_URL`: 데이터 수집 서비스 URL
- `VOICEBOT_SERVICE_URL`: 음성봇 서비스 URL
- `ADMIN_SERVICE_URL`: 관리자 서비스 URL

### 4. 보안 주의사항

- `.env` 파일은 절대 Git에 커밋하지 마세요
- 실제 API 키나 비밀번호는 환경변수로만 관리하세요
- 프로덕션 환경에서는 시스템 환경변수나 시크릿 관리 도구를 사용하세요

### 5. 환경변수 우선순위

환경변수는 다음 순서로 우선순위를 가집니다 (높은 순서부터):

1. **시스템 환경변수** (가장 높음)
2. **.env 파일의 변수**
3. **application.yml의 기본값** (가장 낮음)

### 6. 환경변수 확인

애플리케이션 실행 후 다음 엔드포인트로 설정 확인이 가능합니다:
- Health Check: `http://localhost:3002/actuator/health`
- Info: `http://localhost:3002/actuator/info`

### 7. 로그 확인

애플리케이션 시작 시 다음과 같은 로그를 확인할 수 있습니다:
```
INFO  - Successfully loaded .env file with X variables
DEBUG - Loaded environment variable from .env: SPRING_DATASOURCE_URL=***
```

### 8. 문제 해결

#### .env 파일이 로드되지 않는 경우
1. `.env` 파일이 프로젝트 루트에 있는지 확인
2. 파일 권한 확인: `ls -la .env`
3. 애플리케이션 로그에서 에러 메시지 확인

#### 환경변수가 적용되지 않는 경우
1. 시스템 환경변수와 충돌하는지 확인
2. `application.yml`의 기본값이 올바른지 확인
3. 환경변수 이름이 정확한지 확인 (대소문자 구분)
