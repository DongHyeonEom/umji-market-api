# umji-market-api 작업 기준

## 기반 아키텍처

이 서비스의 최상위 아키텍처 기준은 **헥사고날 아키텍처(Ports and Adapters)와 클린 아키텍처**임. 의존성은 항상 안쪽(도메인·애플리케이션)을 향해야 하며, Spring MVC·Spring Data JPA·DB·외부 서비스는 바깥 어댑터로 취급함.

- 도메인 모델과 애플리케이션 유스케이스가 중심이며, 프레임워크와 영속성 구현에 의존하지 않도록 함
- 애플리케이션은 입력 Port(UseCase)와 출력 Port(Repository/Gateway 인터페이스)에 의존함. 구현체는 바깥 adapter에서 제공함
- Controller는 입력 adapter로서 요청을 유스케이스 입력 모델로 변환하고 결과를 HTTP 응답으로 변환함
- JPA Entity, Spring Data Repository, HTTP DTO를 도메인·유스케이스 경계 밖으로 전달하지 않음. 영속성 adapter에서 도메인 모델로 매핑함
- 기능 추가·수정 시 기존 구조가 위 의존성 규칙을 지키는지 확인하고, 경계 침범을 새로 만들지 않음. 기존 코드의 점진적 정리는 변경 범위와 위험을 고려해 수행함
- 세부 패키지 경계와 현재 구조 평가는 `docs/architecture.md`를 기준으로 함

## 역할

Flutter WebView와 React 프론트엔드가 호출하는 Kotlin/Spring Boot 통합 API. 사용자·관리자 기능은 별도 서비스가 아니라 role/permission 기반 서버 권한 검사로 구분함

## 구현 원칙

- 패키지 루트: `com.buyeong.umji.api`
- Controller/HTTP adapter는 입출력 변환, Application Use Case는 유스케이스 조정, Domain은 핵심 업무 규칙, Persistence adapter는 DB 접근 담당
- 외부 request/response에 Entity 직접 노출 금지
- 관리자 API는 UI 노출 여부와 무관하게 서버 권한 검사 필수
- 신규 DB 변경은 Flyway 마이그레이션으로만 적용. 이미 배포된 마이그레이션 수정 금지
- 시크릿은 설정 파일에 저장하지 않고 환경 변수 또는 프로젝트 외부 파일 경로로 주입

## 브랜치 및 커밋 전략

- 신규 기능은 `codex/feature/<기능명>` 브랜치에서 작업하고, 구현·검토 가능한 상태가 되면 PR을 생성함. PR 전까지 해당 기능의 수정은 같은 feature 브랜치에서 진행함.
- 이미 진행 중이거나 병합된 기능의 일반 수정은 `dev`에서 바로 작업하고 커밋함. 커밋 메시지는 `fix: <수정 내용>` 형식으로 작성함.
- 기존 기능에 대한 수정 범위가 방대하면 `codex/feature/<기존 기능명>-fix` 브랜치에서 작업하고 PR을 생성함.
- 문서 수정 및 신규 문서 작업은 `dev`에서 바로 반영하고 커밋함. 커밋 메시지는 `docs: <문서 작업 내용>` 형식으로 작성함.
- 기능 브랜치는 최신 `dev`를 기준으로 생성함. 위 규칙의 작업 대상 브랜치와 PR 여부는 작업 종류별 기준을 따름.

## 문서

- `README.md`: 실행 방법과 필수 환경 변수
- `docs/architecture.md`: 현재 아키텍처, 의존성 규칙, 패키지 구조
- `docs/services/`: 구현된 API 동작과 도메인별 미구현 범위
- `docs/database.md`: 현재 DB 규칙과 적용된 migration 목록
- `docs/database-erd.md`: 현재 migration에 존재하는 테이블 관계
- `docs/implementation-status.md`: 현재 구현 상태 요약

## 문서 작성 규칙

- 문서의 현재 동작은 코드·설정·Flyway migration과 일치시킴. 불일치하면 실제 구현을 확인하고 수정함.
- 구현된 기능과 미구현 계획을 명확히 구분하고, 계획 endpoint나 테이블을 현재 제공되는 것처럼 적지 않음.
- 완료된 작업의 시간순 진행 내역, 이미 정리된 구조의 설명, 중복된 규칙은 문서에 남기지 않음. 현재 동작·유지해야 할 정책·명시적인 미구현 범위만 기록함.
- 자세한 내용은 한 문서를 단일 기준으로 유지하고, 다른 문서에서는 링크로 연결해 복사된 설명이 어긋나지 않게 함.

루트 `E:\buyeong_dev\umji-market\AGENTS.md`의 공통 기준도 함께 적용함
