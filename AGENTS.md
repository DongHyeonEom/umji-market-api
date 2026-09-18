# umji-market-api 작업 기준

## 역할

Flutter WebView와 React 프론트엔드가 호출하는 Kotlin/Spring Boot 통합 API. 사용자·관리자 기능은 별도 서비스가 아니라 role/permission 기반 서버 권한 검사로 구분함

## 구현 원칙

- 패키지 루트: `com.buyeong.umji.api`
- Controller는 HTTP 입출력, Service는 비즈니스 규칙, Persistence는 DB 접근 담당
- 외부 request/response에 Entity 직접 노출 금지
- 관리자 API는 UI 노출 여부와 무관하게 서버 권한 검사 필수
- 신규 DB 변경은 Flyway 마이그레이션으로만 적용. 이미 배포된 마이그레이션 수정 금지
- 시크릿은 설정 파일에 저장하지 않고 환경 변수 또는 프로젝트 외부 파일 경로로 주입

## 문서

- `README.md`: 실행 방법과 필수 환경 변수
- `docs/architecture.md`: 도메인·API·인증 구조
- `docs/database.md`: DB 모델과 Flyway 기준
- `docs/implementation-status.md`: 현재 기반 상태와 다음 구현 순서

루트 `E:\buyeong_dev\umji-market\AGENTS.md`의 공통 기준도 함께 적용함
