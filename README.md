# Helpme Backend

## 소개
Helpme Backend는 GitHub 저장소의 README를 분석하고 초안 생성 및 평가를 하는 API 서버입니다. GitHub API와 OpenAI(GPT) 통합, Redis 캐시, JWT 기반 인증, Server-Sent Events(SSE)를 활용하여 긴 작업(예: 코드·문서 분석, README 생성/평가)도 안정적으로 처리하도록 설계되었습니다.

## 주요 기능 (Key Features)
- OAuth2 / GitHub 로그인 및 인증 흐름
- JWT 기반 인증 및 Refresh Token 관리
- GitHub 리포지토리 조회·분석 / 브랜치 및 README 조회
- README 초안 생성 및 평가를 위한 GPT 통합
- Section(섹션) 기반 README 분할/편집/재정렬 API
- 긴 처리 작업을 위한 SSE 기반 실시간 응답 스트리밍
- Redis 캐싱 및 폴백(결과 저장/재전송) 전략
- Swagger / OpenAPI 문서화 및 커스텀 에러 스키마
- Docker & CI/CD (GitHub Actions) 기반 빌드 및 배포 자동화

## 기술 스택 (Tech Stack)
| 구분 | 기술 스택 |
|------|-----------|
| Backend | ![Java](https://img.shields.io/badge/Java-007396?logo=Java&logoColor=white) ![Spring_Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?logo=spring&logoColor=white) ![Spring_Security](https://img.shields.io/badge/Spring_Security-6DB33F?logo=spring&logoColor=white) ![Spring_Data_JPA](https://img.shields.io/badge/Spring_Data_JPA-6DB33F?logo=spring&logoColor=white) ![JWT](https://img.shields.io/badge/JWT-000000?logo=jwt&logoColor=white) |
| Cache / Message | ![Redis](https://img.shields.io/badge/Redis-DC382D?logo=redis&logoColor=white) |
| API Docs | ![Swagger](https://img.shields.io/badge/Swagger-85EA2D?logo=swagger&logoColor=white) |
| GPT / AI | ![OpenAI](https://img.shields.io/badge/OpenAI-412991?logo=openai&logoColor=white) ![GPT_Client](https://img.shields.io/badge/GPT_Client-7A1EA1?logo=openai&logoColor=white) |
| Integrations | ![GitHub](https://img.shields.io/badge/GitHub-181717?logo=github&logoColor=white) ![SSE](https://img.shields.io/badge/SSE-FF8800?logo=server&logoColor=white) |
| Build / CI | ![Gradle](https://img.shields.io/badge/Gradle-02303A?logo=gradle&logoColor=white) ![Docker](https://img.shields.io/badge/Docker-2496ED?logo=docker&logoColor=white) ![GitHub_Actions](https://img.shields.io/badge/GitHub_Actions-2088FF?logo=github-actions&logoColor=white) |

## 시스템 구조 및 아키텍처
프로젝트 주요 폴더(요약):

```
src/
├─ main/
│  ├─ java/seungyong/helpmebackend/
│  │  ├─ adapter/in/web/ — HTTP 어댑터: Controllers, Config, Filters, Mapper (웹 레이어)
│  │  │  ├─ controller/ (AuthController, RepoController, SectionController, SSEController 등)
│  │  │  ├─ config/ (SecurityConfig, WebConfig)
│  │  │  └─ filter/ (AuthenticationFilter, RequestLoggingFilter)
│  │  ├─ usecase/ — 애플리케이션 비즈니스 로직: PortIn, Service 계층 
│  │  ├─ infrastructure/ — 외부 통합 및 인프라
│  │  ├─ domain/ — 엔티티 및 도메인 예외
│  │  └─ common/ — 공통 예외/핸들러
│  └─ resources/ — 설정/리소스(exclude lists 등)

루트
├─ build.gradle, settings.gradle, gradlew(+wrapper)
├─ .github/workflows/docker-image.yml — CI/CD 워크플로우
├─ .dockerignore
```

설명 요약:
- Clean Architecture의 **Hexagonal Architecture**를 적용
- adapter/in/web: 외부 요청을 받아 usecase(서비스)로 위임
- usecase: 핵심 비즈니스 로직과 유즈케이스를 캡슐화
- infrastructure: 3rd-party 통합(GitHub, OpenAI/GPT, Redis, JWT 등)
- domain: 영속화 모델과 도메인 규칙

## 개발자 (Contributors)
| 이름 | 역할 | 기능 |
|------|------|------|
|   김승용   |   Backend   |   기획, 개발, CI/CD   |

## 라이선스 (License)
This project is licensed under the MIT License.

## 추가 노트

해당 README는 Helpme.md에서 **초안 AI 생성**으로 생성된 내용입니다.