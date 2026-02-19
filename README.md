# HelpMe Backend

## 소개
이 저장소는 GitHub 리포지터리의 README 및 코드 구조를 분석하고, OpenAI/GPT 기반의 평가와 README 초안 생성을 지원하는 Spring Boot 기반 백엔드 서비스입니다. 주요 목적은 다음과 같습니다:

- GitHub 리포지터리에서 중요 파일과 구조를 추출하여 자동으로 README 초안을 생성하거나 평가 결과를 제공
- 긴 작업(예: GPT 평가 / 초안 생성)에 대해 Server-Sent Events(SSE)를 통해 실시간 진행 상태 전송
- GitHub OAuth2 연동, JWT 기반 인증 및 Redis 캐싱을 통해 안전하고 효율적인 API 제공

이 프로젝트는 실무 적용을 목표로 한 중간 규모의 백엔드 서비스로 설계되어 있으며, CI/CD 및 Docker 배포를 고려하여 구성되어 있습니다.

## 주요 기능
- GitHub OAuth2 (App 설치/로그인) 기반 인증 플로우
- JWT(Access Token) + Refresh Token(쿠키 기반) 인증/재발급 로직
- GitHub 리포지터리 목록/브랜치/README 등 상세 정보 조회 API (Repository 분석)
- README 자동 초안 생성 및 평가 (OpenAI/GPT 통합)
- SSE(Server-Sent Events)를 통한 비동기 작업 진행 알림 및 폴백(리디스 캐시) 처리
- 섹션(Section) 단위의 README 분할/생성/수정/재정렬/초기화 기능
- Redis 기반 캐싱(인증 상태, GitHub 응답, GPT 캐시 등)으로 비용 및 호출 최적화
- Swagger(OpenAPI) 문서화 및 커스텀 에러 응답/예시 지원
- Docker 멀티스테이지 빌드 및 GitHub Actions 기반 CI/CD 워크플로우
- 예외/에러 코드 상세화 및 글로벌 예외 처리

(구현된 주요 엔드포인트 예시)
- /auth/**  — OAuth2 / 로그인 관련
- /repos/**  — 리포지터리 조회/분석
- /sections/** — README 섹션 관리
- /sse/** — SSE 연결 및 이벤트
- /users/** — 사용자 관련 API

## 기술 스택 (Tech Stack)
| 구분 | 기술 스택 |
|------|-----------|
| Backend | ![Java](https://img.shields.io/badge/Java-007396?logo=java&logoColor=white) ![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?logo=spring&logoColor=white) ![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?logo=spring&logoColor=white) ![JPA_Hibernate](https://img.shields.io/badge/JPA--Hibernate-59666C?logo=hibernate&logoColor=white) |
| Build / Tooling | ![Gradle](https://img.shields.io/badge/Gradle-02303A?logo=gradle&logoColor=white) |
| Cache / DB | ![Redis](https://img.shields.io/badge/Redis-D82C20?logo=redis&logoColor=white) |
| Auth / Token | ![JWT](https://img.shields.io/badge/JWT-000000?logo=jwt&logoColor=white) |
| Container / CI | ![Docker](https://img.shields.io/badge/Docker-2496ED?logo=docker&logoColor=white) ![GitHub_Actions](https://img.shields.io/badge/GitHub_Actions-2088FF?logo=githubactions&logoColor=white) |
| Real-time | ![SSE](https://img.shields.io/badge/SSE-FF9900?logo=server&logoColor=white) |
| AI / GPT Integration | ![OpenAI_GPT](https://img.shields.io/badge/OpenAI_GPT-412991?logo=openai&logoColor=white) |
| API Docs | ![Swagger](https://img.shields.io/badge/Swagger-85EA2D?logo=swagger&logoColor=white) |
| External API | ![GitHub](https://img.shields.io/badge/GitHub_API-181717?logo=github&logoColor=white) |

## 시스템 구조 및 아키텍처
프로젝트는 전형적인 포트-어댑터(헥사고날) 구조로 구성되어 있습니다. 주요 패키지/디렉토리와 역할은 다음과 같습니다:

- HelpmeBackendApplication.java — 애플리케이션 진입점

- adapter/in/web/
  - controller/ — REST 컨트롤러 (AuthController, RepoController, SSEController, SectionController, UserController)
  - config/ — Web 및 Security 설정 (WebConfig, SecurityConfig)
  - dto/ — 요청/응답 DTO 계층
  - filter/ — 인증/로깅 필터
  - mapper/ — 외부/내부 포맷 변환

- domain/
  - entity/ — JPA 엔티티 (User, Repository, Project, Section, Installation 등)
  - exception/ — 도메인/에러 코드 정의

- usecase/
  - port/in/ — 서비스 포트(인터페이스)
  - service/ — 비즈니스 로직 구현 (RepositoryService, SectionService, SSEService, OAuth2Service, UserService 등)

- infrastructure/
  - github/ — GitHub API 통신 클라이언트(GithubClient, GithubApiExecutor)
  - gpt/ — GPT/OpenAI 통신 클라이언트 및 DTO
  - jwt/ — JWT 생성/검증 유틸
  - redis/ — Redis store 및 키 설계
  - sse/ — SSE 관련 유틸 및 태스크
  - swagger/ — Swagger 커스터마이징
  - validation/ — 커스텀 밸리데이터

- config/ 및 설정 파일
  - build.gradle, settings.gradle — 빌드 설정
  - .github/workflows/docker-image.yml — CI/CD 이미지 빌드 워크플로우
  - Dockerfile (프로젝트 루트에 존재)

간단한 디렉터리 트리(요약):

src/main/java/seungyong/helpmebackend/
├─ adapter/in/web/
├─ domain/
├─ infrastructure/
└─ usecase/

각 폴더의 역할은 위 설명을 참고하세요. 전체적인 설계는 책임 분리(Controller → Usecase(Service) → Domain → Infrastructure) 원칙을 따릅니다.

## 시작 가이드 (Getting Started)
아래 지침은 로컬 개발 및 Docker 실행을 위한 기본 가이드입니다. 환경별 설정은 application.properties 또는 환경 변수로 관리되어 있으니 실제 배포 전 값을 검토하세요.

### 요구사항
- Java 17+ (프로젝트 설정에 따라 jlink 기반 경량 JRE를 사용)
- Gradle wrapper (제공됨)
- Redis (로컬/원격)
- Docker (컨테이너 빌드/실행 시)

### 설치 (Installation)
1. 저장소 클론

```bash
git clone <repository_url>
cd <repository>
```

2. 의존성 다운로드 / 빌드

```bash
./gradlew clean build
```

3. (옵션) 로컬 Redis 실행

```bash
docker run --name helpme-redis -p 6379:6379 -d redis:6
```

### 실행 (Run)
- 로컬에서 실행

```bash

# 애플리케이션 실행
./gradlew bootRun

# 또는 빌드된 jar로 실행
java -jar build/libs/<project>-0.0.1-SNAPSHOT.jar
```

- Docker로 빌드 및 실행

```bash

# Docker 이미지 빌드
docker build -t helpme-backend:latest .

# 컨테이너 실행 (환경 변수는 아래 예시 참조)
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e GITHUB_APP_ID=<app_id> \
  -e GITHUB_PRIVATE_KEY=<private_key_pem> \
  -e JWT_SECRET=<jwt_secret> \
  -e REDIS_HOST=<redis_host> \
  -e REDIS_PORT=<redis_port> \
  --name helpme-backend helpme-backend:latest
```

### 환경 변수(예시)
- GITHUB_APP_ID — GitHub App ID
- GITHUB_PRIVATE_KEY — GitHub App private key (PEM)
- JWT_SECRET — JWT 서명 키
- REDIS_HOST / REDIS_PORT — Redis 접속 정보
- SPRING_PROFILES_ACTIVE — active profile (dev/prod)
- OAUTH_CALLBACK_URL — OAuth 콜백 URL (환경별로 설정)

(구체적인 프로퍼티 키는 application.properties / application.yml을 확인하세요.)

### Swagger (API 문서)
서버가 실행되면 기본적으로 Swagger UI에서 API 문서를 확인할 수 있습니다:
- /swagger-ui.html 또는 /swagger-ui/index.html

## 개발 가이드
- 코드 스타일: 패키지 구조에 따라 adapter/usecase/infrastructure로 책임을 분리합니다.
- 긴 작업(생성/평가)은 SSE로 처리되며, 실패 시 Redis에 결과를 임시 저장하는 폴백 로직이 존재합니다.
- 중요한 외부 호출(GitHub, OpenAI)은 캐싱 계층을 통해 호출 비용/레이트를 관리합니다.

## 개발자 (Contributors)
| 이름 | 역할 | 기능 |
|------|------|------|
|      |      |      |

(참여자를 추가하려면 위 표에 이름과 역할, 담당 기능을 기입하세요.)

## 기여 방법
1. 이슈(또는 Discussion)로 제안/버그 리포트
2. 기능 브랜치 생성: git checkout -b feat/your-feature
3. 커밋 메시지 규칙 사용 및 PR 생성
4. 코드 리뷰 후 main(또는 develop)으로 병합

## 라이선스 (License)
This project is licensed under the MIT License.

## 추가 참고
- 오류 처리 및 에러 코드 세부화가 잘 되어 있어, 프론트엔드와 명확한 에러 상호작용 가능
- Redis 캐시 키 설계와 TTL 전략은 infra/redis 패키지에서 관리됩니다
- GitHub API 호출 시 레이트 제한 예외 처리가 도메인 예외(GithubRateLimitException)로 분리되어 있음

문서에서 빠진 설정 항목이나 이미지(프로젝트 로고/데모)가 있다면 원본 README 또는 리포지터리 루트의 README.md 내용을 제공해 주세요. 제공해 주시면 이미지 경로 보정 및 기능 설명 근거로 해당 스크린샷/데모를 각 기능 섹션 바로 아래에 배치해 README를 보강하겠습니다.