# 제품 작업 체크리스트

현재 구현 현황은 [implementation-status.md](implementation-status.md), 도메인별 API 계약·알고리즘 흐름은 [services/README.md](services/README.md), 아키텍처 기준은 [architecture.md](architecture.md)를 기준으로 함.<br>완료된 Task와 수용 기준은 [completed-todo.md](completed-todo.md)에 보관.<br>
각 Task에서 서비스 개발과 자동화 테스트 개발·실행을 분리해 관리함.<br>

## 진행 규칙

- `[ ]`는 미완료, `[x]`는 실제 코드·문서 반영과 필요한 검증까지 완료된 상태
- Task 일부만 완료된 경우 체크 상태를 유지하며 `todo.md`에 남김. 모든 수용 기준이 완료되면 Task 전체를 `completed-todo.md`로 이동
- 서비스 코드를 착수·수정하기 전에 관련 `docs/services/<service>.md`의 Mermaid 알고리즘 흐름과 수용 기준을 먼저 확정
- 각 Task를 `서비스 개발`과 `서비스 자동화 테스트 개발 및 테스트`로 나눠 추적
- 범위가 없으면 체크리스트에 항목 및 수용 기준을 추가한 뒤 구현
- 테스트를 작성했더라도 실행하지 않았거나 결과를 확인하지 못한 항목은 `[ ]` 유지
- 정책 미확정 항목은 임의로 확정하지 않고 설계 결정 항목으로 유지
- 구현 중 정책·운영 절차·외부 연동 제약이 발견되면 관련 수용 기준을 먼저 갱신하고 검토 지점에서 결정 확인
- Task의 개발·테스트가 완료되면 동일 feature 브랜치에 수용 기준별 커밋·push 후 feature 단위 PR 하나로 제출
- 공통 완료 기준은 개별 미완료 작업이 아니라 각 Task에 적용하는 완료 규칙으로 관리

## 추후 개발 — 현재 개발 범위 제외

- PG 제공자 선정 및 실제 도입 시 내부 결제 변경의 transaction·보상 경계 정의·구현<br>
  도입 전제: PG 제공자·callback 계약·결제 취소 및 환불 흐름 확정.<br>
  후속 검증: 중복 callback·동시 요청·실패 보상 및 기존 수동 계좌이체 흐름 회귀 검증.<br>
- SMS 휴대폰 본인 확인과 계정 활성화 연동<br>
  도입 전제: SMS 제공자 및 본인 확인·신규/비활성 계정 정책 확정.<br>
  후속 검증: 인증 코드 단회 사용·만료·재시도·요청 한도·발송 실패와 계정 활성화 검증.<br>

## Task 11 — 계층형 아키텍처 전환

### 서비스 개발

- [x] 모든 도메인 요청 흐름을 `Controller → 도메인 Service → JpaEntityService → Spring Data Repository → JPA Entity`로 통일
- [x] `adapter/in/web` Controller와 `application` Service·model을 도메인별 `controller`·`service`·`model` 패키지로 이동
- [x] UseCase·Port 계약 및 위임 adapter 제거, 구체 Service·JpaEntityService 직접 주입으로 전환
- [x] 외부 연동 구현을 `integration` 패키지로 이동하고 SDK callback/provider 인터페이스만 내부 구현 세부로 유지
- [x] Controller에서 인증 조회 전용 Port를 거치지 않고 구체 인증 Service 직접 호출
- [x] 트랜잭션 경계를 도메인 Service에 통합하고 모든 wrapper 제거
- [x] `AGENTS.md`·README·architecture·서비스 문서 및 구현 현황을 새 패키지 경계에 맞춰 갱신
- [x] `persistence.jpa.<domain>` 아래 JPA Entity·Spring Data Repository·JpaEntityService를 각각 `entity`·`repository`·`service` 패키지로 정리

### 서비스 자동화 테스트 개발 및 테스트

- [x] Service 단위 테스트 및 Controller·JPA 통합 테스트를 계층별로 작성·실행
- [x] 계층·도메인·DTO 구조 변경 뒤 테스트의 import와 타입 참조 갱신 및 unit·integration 테스트 소스 컴파일
- [x] unit 테스트 224개 실행 및 통과
- [ ] 전체 기존 동작·권한·트랜잭션 회귀 검증. `check` 실행 중 integration 테스트 97개 가운데 14개 실패

## Task 12 — Organization 구조와 공통 사업자 정보

### 서비스 개발

- [x] `ACCOUNT`와 분리된 `organization`, 공통 사업자 프로필, 구성원 모델 적용. 조직은 구매자·판매자·운영 capability를 복수로 보유 가능
- [x] 구매자 조직의 주문·초대·가입·배송지 경계를 보존하고 판매자 조직도 복수 계정 구성원을 가질 수 있도록 지원
- [x] 계정 단위 `business_profile`과 그룹 단위 `buyer_group_business_profile`을 공통 Organization 프로필로 통합하는 신규 Flyway migration 및 값 보존 로직 추가
- [x] 운영자 계정 생성·수정 및 사업자 등록 API를 계정 프로필 저장에서 Organization 생성·연결 API로 전환
- [x] 사용자 그룹 기반 API 경로·요청·응답을 Organization 용어로 전환하고 구매자 조직의 세금계산서·국세청 확인은 capability 검증
- [x] Organization 조회·수정·생성·구성원 연결 API와 서버 권한 검사 적용. 판매자도 Organization 생성 및 구성원 초대·가입 가능
- [x] 기존 개인 조직·계정 인증·구매 조직 주문·배송지 계약 유지 및 관련 문서·OpenAPI 갱신
- [x] Organization 기본 세금계산서 발행 설정으로 이동하고 주문별 발행 선택은 주문에 유지

### 서비스 자동화 테스트 개발 및 테스트

- [ ] Flyway migration의 기존 계정·그룹 사업자 데이터 이관과 관계·필드 보존 검증
- [ ] 사용자 사업자 등록·구매/판매 Organization 생성·세금계산서·국세청 상태 확인의 Organization 연결 검증
- [ ] 운영자 Organization 등록·수정·계정 연결·권한 검사 검증
- [ ] 복수 capability Organization, 단일 Organization 소속 계정, 구매자·판매자 다중 구성원 접근 범위 및 capability별 API 권한 검증
- [ ] 개인 그룹과 기존 주문·주소·세금계산서 snapshot 회귀 검증
- [ ] 기존 계정별 세금계산서 기본값의 Organization 이관 및 대표자 변경 권한·구성원 간 공유 검증

## Task 13 — 주문 세금계산서 데이터 분리

### 서비스 개발

- [x] 세금계산서 발행 요청 주문에만 `purchase_order_tax_invoice` 행을 생성하고 `PURCHASE_ORDER`의 발행 전용 컬럼 제거
- [x] V41 migration에 기존 발행 요청 주문의 상태·날짜·공급자·공급받는자 snapshot 이관 쿼리 추가
- [x] 주문 상세·목록 API의 `taxInvoiceRequested`와 snapshot 응답, 송장 등록 시 상태 전환 계약 유지
- [x] 주문 흐름도·ERD·데이터 규칙 갱신

### 서비스 자동화 테스트 개발 및 테스트

- [ ] 요청 주문·미요청 주문의 테이블 행 생성 및 기존 데이터 migration 보존 검증
- [ ] 주문 목록·상세 API 응답과 배송 송장 등록 시 발행 상태·일자 전환 회귀 검증

## Task 15 — 관리자 전화 주문·세금계산서 수기 등록

관리자 전용 주문 대행과 홈택스 수기 발행 결과 기록을 위한 관리자 API·저장·인가 기능 추가. React 관리 화면은 별도 범위이며 이 Task에는 포함하지 않음.<br>
사용자 주문 기능을 이용할 수 없어 전화 주문으로 접수한 거래를 시스템에 남기고, 자동 세금계산서 발행 연동이 없거나 실패한 경우 관리자가 홈택스에서 발행한 결과를 주문에 기록하는 범위.<br>

### 서비스 개발

- [x] 전화 주문·세금계산서 수기 발행 흐름을 서비스 문서에 Mermaid로 기록
- [x] 관리자가 구매자를 대신해 기존 주문·재고·결제 흐름과 연결된 전화 주문 API 구현
- [x] 전화 주문 출처, 생성 관리자, 구매자·주문·배송 정보와 주문 시점 상품·가격 snapshot 및 감사 이력 기록
- [x] 일반 주문과 동일한 주문·재고 예약·판매자별 분리·결제 상태 규칙 적용
- [x] 관리자가 홈택스 수기 발행 정보를 주문에 등록·조회하는 API 구현
- [x] 수기 발행 상태, 승인번호 등 발행 식별 정보, 발행일자·공급가액·세액·합계와 처리 관리자 기록
- [x] 수기 발행 완료 상태 및 기존 주문 세금계산서 snapshot·발행 상태와의 관계 정의·구현
- [x] 기존 `ORDER_WRITE` permission으로 API 인가 및 변경 감사 이력 적용
- [x] 신규 Flyway migration 작성 및 `database.md`·`database-erd.md` 갱신

### 구현 전·중 검토 지점

- [x] 전화 주문은 기존 활성 구매자 Organization 구성원만 허용
- [x] 전화 주문은 활성 도매 판매 오퍼 가격·재고 사용, 별도 수기 가격·할인 미허용
- [x] 전화 주문 상태는 `PENDING_PAYMENT`, 기존 계좌 안내 snapshot·선택형 세금계산서 요청·주문 생성 outbox 사용. 기존 주문 모델에 배송비 항목 없음
- [x] API·서버 전체 장애 중 전화 주문은 별도 기록 후 복구 시 사후 등록
- [x] 발행 요청 주문 중 `READY_FOR_ISSUANCE` 상태만 허용. 홈택스 발행일·작성일·공급일은 각각 실제 값으로 입력 가능
- [x] 홈택스 승인번호 필수, 발행 완료 기록 불변. 정정·취소·재발행은 이번 범위 제외
- [x] 수기 발행 승인번호를 unique로 보존해 향후 발행 API 도입 시 중복 확인 기준으로 사용
- [x] 수기 세금계산서 발행 완료 시 주문자에게 `TAX_INVOICE_ISSUED` 정보성 push outbox 이벤트 기록
- [ ] 자동 전자세금계산서 발행 연동 완료 callback에서 `TAX_INVOICE_ISSUED` 이벤트 기록 및 자동 발행 알림 테스트 실행
- [x] 기존 `ORDER_WRITE` permission 사용. `ADMIN`·`SUPER_ADMIN`·`ORDER_MANAGER` 권한 mapping 재사용

### 서비스 자동화 테스트 개발 및 테스트

- [x] 전화 주문의 관리자 권한·입력 검증·주문 snapshot·재고 예약 및 실패 시 rollback 자동화 테스트 개발 및 실행
- [x] 기존 활성 계정/BUYER Organization 전화 주문 범위 및 `ORDER_WRITE` 없는 사용자 접근 차단 자동화 테스트 개발 및 실행
- [x] 다중 판매자 전화 주문 분리·오퍼 가격 snapshot·재고·결제 상태 통합 테스트 개발 및 실행
- [x] 수기 발행 필수 필드·금액·완료 상태 불변·승인번호 중복 검증 및 permission 자동화 테스트 개발 및 실행
- [x] 전화 주문·수기 발행 MySQL migration·주문/이벤트 저장 테스트 개발 및 실행
- [x] 전화 주문·수기 발행 작업자의 operation audit resource type·대상 ID 생성 및 MySQL 조회 통합 테스트 개발·실행

## Task 17 — MVP 전 미사용 스키마·코드 정리

실운용 전 현재 구현에서 사용하지 않는 레거시 스키마와 서비스 코드를 참조 분석 후 제거. MVP 전체 데이터 초기화는 정해진 테스트 전·후 시점에 각각 별도 수행.<br>

### 서비스 개발

- [x] 현재 Flyway schema와 Kotlin 엔티티·저장소·서비스 참조를 대조해 미사용 후보 확인
- [x] Organization 프로필로 데이터 이관이 완료된 레거시 `business_profile` 테이블을 신규 migration으로 제거하고 DB 문서 갱신
- [x] 활성 Service·JpaEntityService 정의와 주입 참조를 확인. 제거 가능한 미사용 서비스 코드 없음
- [ ] MVP 테스트 직전 애플리케이션 데이터 전체 초기화 수행
- [ ] MVP 테스트 완료 후 애플리케이션 데이터 전체 초기화 수행

### 서비스 자동화 테스트 개발 및 테스트

- [x] 레거시 `business_profile` 제거 migration 및 전체 migration 통합 검증

## Task 18 — Request·Response·내부 DTO 구조 정리

외부 HTTP 계약과 내부 데이터 전달 객체의 이름·파일·패키지 경계를 일관되게 정리.<br>

### 서비스 개발

- [x] 외부 요청·응답 모델을 도메인 `model` 패키지에 두고 각각 `<Action>Request`, `<Action>Response`로 분리
- [x] Controller 이하 내부 전달 객체를 각각 `<Action>Dto`로 명명하고 도메인 `dto` 패키지에 배치
- [x] 데이터 클래스 중첩 선언 제거 및 데이터 클래스별 독립 파일 구성
- [x] Controller 패키지에 선언된 HTTP 요청·응답 모델과 요청·응답이 한 파일에 혼재된 구조 제거

### 서비스 자동화 테스트 개발 및 테스트

- [ ] 전체 자동화 테스트 실행으로 API 계약과 내부 DTO 분리 후 회귀 검증

## Task 19 — 서비스 도메인 패키지 그룹화

서비스 도메인 패키지를 `domain/` 아래로 모으고 영속성 구현은 별도 영역으로 유지.<br>

### 서비스 개발

- [x] 서비스 도메인 패키지를 `api/domain/<domain>/` 아래로 이동하고 package/import 및 문자열 기반 참조 갱신
- [x] `persistence/jpa`와 공통 인프라 패키지를 도메인 그룹 바깥에 유지
- [x] `AGENTS.md`와 `docs/architecture.md`에 도메인·영속성 패키지 경계 반영

### 서비스 자동화 테스트 개발 및 테스트

- [ ] 전체 자동화 테스트 실행으로 패키지 이동 후 회귀 검증
