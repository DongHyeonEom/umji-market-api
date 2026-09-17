# umji-market-api 작업 원칙

`umji-market-api` 저장소 작업 기준 문서

## Repository Role

엄지철물마켓 통합 Kotlin/Spring Boot API. Flutter WebView와 React 프론트 호출용 API 제공

WebView 기반 단일 서비스 도메인. 사용자·관리자 화면 분리 대신 로그인 계정 role/permission 기반 UI·API 접근 제어

## Source Of Truth

- 전체 프로젝트 방향성: `E:\buyeong_dev\umji-market\AGENTS.md`
- 이 저장소 작업 기준: `E:\buyeong_dev\umji-market-api\docs\AGENTS.md`
- 상세 설계 문서: `E:\buyeong_dev\umji-market-api\docs\*.md`

## Tech Stack

- Language: Kotlin
- Framework: Spring Boot
- Runtime: Java 21
- Build Tool: Gradle Kotlin DSL
- Test Tool: Kotest, MockK, Testcontainers
- API Docs: Springdoc OpenAPI / Swagger

## Local Commands

```bash
./gradlew build
./gradlew bootRun
./gradlew bootRun --args='--spring.profiles.active=local'
./gradlew test
./gradlew integrationTest
./gradlew e2eTest
./gradlew check
./gradlew ktlintCheck
./gradlew ktlintFormat
```

## Architecture Rules

- Package root: `com.buyeong.umji.api`
- Controller: HTTP request/response 처리
- Service: 비즈니스 규칙
- Persistence: DB 접근
- Model: Controller 입출력, DTO: Service 계층 전달, Entity: DB 매핑
- 사용자·관리자 기능: 동일 API, controller/service/model 패키지 역할 분리
- 인증·권한: role/permission 기반 설계
- 외부 request/response: Entity 직접 사용 금지

## Documentation Rules

- Markdown 문서 위치: `docs/`
- DB 구현·변경 기준: `docs/database-design.md`의 식별자·금액·시간·이력 정책
- 중요 설계 결정: 관련 문서 즉시 반영
- 문서·코드 충돌: 코드 수정 전 기준 확인
- 문서 비대화: 분리 및 중복 제거
- 문서 종결: 간결한 명사형 우선, 명사형이 부자연스러운 경우 `함.`, `다.` 종결 금지

## Current Decisions

- WebView 기반 사용자·관리자 백엔드 서비스 미분리
- 단일 API 프로젝트의 계정 role/permission 기반 UI·기능 접근
- 관리자 기능: UI 노출과 별도 서버 권한 검사
- 스켈레톤 원본 보존 위치: `E:\buyeong_dev\buyeong-spring-boot-internal-api-skeleton`

## Working Notes

새 기능 시작 시 검토 순서

1. 기능 도메인 확인
2. 사용자·관리자 기능 범위 확인
3. 필요 role/permission 정의
4. API request/response 선행 정리
5. Controller, Service, Persistence 경계 유지 구현
6. 변경 범위별 테스트 추가
