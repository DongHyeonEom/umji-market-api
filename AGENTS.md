# umji-market-api 작업 기준

## 프로젝트

Flutter 앱의 React WebView가 호출하는 Kotlin/Spring Boot 통합 API.<br>
사용자와 관리자는 별도 서비스가 아니라 role·permission 기반으로 구분.<br>
패키지 루트: `com.buyeong.umji.api`.<br>

## 문서 기준과 작업 전 확인

작업 공간 공통 기준은 `../AGENTS.md`, 저장소별 규율은 이 문서를 기준으로 함.<br>
모든 Markdown 문서를 매번 읽지 않고 변경 범위에 해당하는 기준 문서만 확인함.<br>

| 작업 범위 | 확인할 문서 |
| --- | --- |
| 모든 작업 | `../AGENTS.md`, `AGENTS.md`, `docs/todo.md`의 관련 Task |
| 완료된 선행 작업의 결정·수용 기준 확인 | 필요한 경우 `docs/completed-todo.md` |
| 제품 전체 구매·운영 흐름 변경 | 작업 공간의 `../SERVICE_FLOW.md` |
| 패키지·계층·Request/Response/DTO 경계 변경 | `docs/architecture.md` |
| 서비스 동작·API 변경 | `docs/services/<domain>.md`; 문서 위치 탐색이 필요할 때 `docs/services/README.md` |
| DB schema·영속 규칙 변경 | 관련 서비스 문서, `docs/database.md`, `docs/database-erd.md` |
| 실행 방법·환경 변수 변경 | `README.md` |
| 구현 범위 요약 변경 | `docs/implementation-status.md`와 해당 상세 기준 문서 |

각 문서의 기준 범위는 다음과 같음.<br>

- `AGENTS.md`: 에이전트 작업 순서, 저장소 규율, 문서 탐색 기준
- `docs/architecture.md`: 계층 책임, 패키지 구조, API 모델·내부 DTO 규칙
- `docs/services/README.md`: 도메인 상세 문서의 목차
- `docs/services/<domain>.md`: 해당 도메인의 현재 API 동작, 흐름, 명시적 미구현 범위
- `docs/database.md`: DB 규칙과 migration 변경 요약
- `docs/database-erd.md`: 현재 schema의 테이블·컬럼·관계
- `docs/implementation-status.md`: 구현 상태의 간결한 요약과 남은 기능 범위
- `docs/todo.md`: 진행 중·미완료 작업과 수용 기준
- `docs/completed-todo.md`: 모든 기준이 완료된 Task의 체크리스트
- `README.md`: 로컬 실행 방법과 필수 환경 변수
- `../SERVICE_FLOW.md`: 저장소를 가로지르는 제품·채널 전체 흐름

같은 규칙이나 기능 설명을 여러 문서에 복사하지 않고 기준 문서를 연결함.<br>

## 구현·운영 규율

- 아키텍처와 모델 명명 규칙은 `docs/architecture.md`를 단일 기준으로 사용함.
- 관리자 API는 UI 노출 여부와 무관하게 서버 권한 검사를 수행함.
- 신규 DB 변경은 Flyway migration으로만 적용하고, 이미 배포된 migration은 수정하지 않음.
- 시크릿은 설정 파일에 저장하지 않고 환경 변수, 저장소 밖 파일 또는 Secret Manager로 주입함.
- 사용자가 실운용 전이라고 명시한 동안 구현·검증에 필요한 테이블과 코드만 유지함. 추정성 테이블, 사용처 없는 추상화·호환 계층은 추가하지 않음.
- 테스트 데이터는 검증 후 정리함. 정리 실패 시 후속 작업에서 잔여 데이터를 확인해 제거함.
- 실운용 데이터 보존·감사·이력 요구는 임의로 적용하지 않음. 실운용 전환 시 보존 규율을 사용자와 다시 정리함.

## 서비스 변경 순서

- 서비스 동작을 새로 만들거나 변경할 때 관련 `docs/services/<domain>.md`의 실제 흐름도와 수용 기준을 먼저 갱신함.
- 흐름도에는 입력, 인증·인가, 분기·검증, Service·JpaEntityService·외부 연동, 저장·상태 변경, 성공·실패 결과를 반영함.
- 기존 흐름도가 없으면 먼저 코드 흐름을 확인해 작성함. 계획 흐름과 구현 흐름을 섞지 않음.
- `docs/todo.md` Task는 서비스 코드와 자동화 테스트 작성·실행 항목을 구분함. 실행하지 않은 테스트는 완료 처리하지 않음.
- DB schema 또는 영속 규칙 변경 시 migration과 함께 `docs/database.md`, `docs/database-erd.md`를 갱신하고 실제 DDL 및 전체 migration과 대조함.

## 브랜치·커밋

- 신규 기능은 최신 `dev`에서 `codex/feature/<기능명>` 브랜치를 생성해 작업함.
- PR 전까지 같은 feature 브랜치에서 수정하며, 검토 가능한 상태가 되면 원격에 push함. PR 생성·병합은 사용자가 진행함.
- PR 제목은 `feat: <English summary>`, `fix: <English summary>`, `test: <English summary>`, `docs: <English summary>` 등 Conventional Commit 형식으로 작성함.
- dev 병합은 GitHub merge commit 방식을 유지함. PR 제목과 자동 생성 merge commit 제목은 별도 규칙으로 취급함.
- 기능 브랜치 push 전에 `docs/todo.md`에서 검증된 수용 기준만 완료 처리함.
- 같은 Task의 자동화 테스트는 수용 기준별로 같은 feature 브랜치에 반영하고, 전체 완료 후 feature 단위 PR 하나로 제출함. 완료·병합된 테스트는 반복하지 않음.
- 이미 진행 중이거나 병합된 기능의 일반 수정은 `dev`에서 작업하고 `fix: <English summary>` 형식으로 커밋함. 수정 범위가 방대하면 `codex/feature/<기존 기능명>-fix` 브랜치와 PR을 사용함.
- 문서 전용 변경은 `dev`에서 `docs: <English summary>` 형식으로 커밋함.
- 모든 커밋 메시지는 영문으로 작성함.
- 다음 feature 시작 시 `origin/dev`를 fetch하고 로컬 `dev`를 fast-forward한 뒤 브랜치를 생성함.

## 문서 작성

- 코드·설정·migration과 현재 동작이 일치하도록 작성함. 계획과 구현을 명확히 구분함.
- 진행 중 Task의 완료되지 않은 기준은 `docs/todo.md`에 유지함. 모든 기준이 완료된 Task는 삭제하지 않고 `docs/completed-todo.md`로 이동함.
- 완료 Task는 결과 체크리스트만 보관하고 시간순 진행 메모·이전 구조 설명·중복 설명은 남기지 않음.
- 문장은 명사형 종결 또는 간결한 서술형으로 작성함. 독자에게 직접 지시하거나 `~입니다`, `~합니다`, `~습니다`로 끝내지 않음.
- 문장 끝에서 줄바꿈하고 마침표 뒤에 `<br>`을 붙임. 문단 구분에는 빈 줄을 사용하고 줄 끝 역슬래시는 사용하지 않음.

상위 경로 `../AGENTS.md`의 공통 기준도 적용함.
