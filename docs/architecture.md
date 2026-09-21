# API 구조

## 서비스 경계

엄지마켓은 Flutter 앱 셸, React WebView, Kotlin/Spring Boot API로 구성함. 초기에는 사용자·관리자 기능을 하나의 API 서비스에서 제공하고 계정의 role/permission으로 UI와 API 접근을 구분함

```text
Flutter App -> WebView -> React Web -> umji-market-api -> MySQL / 외부 서비스
```

관리자 기능은 UI 은닉만으로 보호하지 않으며, 모든 관리자성 API에서 서버 권한 검사를 수행함

## 패키지와 계층

패키지 루트는 `com.buyeong.umji.api`임

```text
auth          인증·토큰·권한
account       계정·주소
catalog       상품·카테고리·브랜드
cart          장바구니
order         주문·취소·환불
payment       결제
inventory     재고·예약·변경 이력
operation     관리자 운영·감사 로그
notification  알림
file          파일 메타데이터
common        공통 설정·예외·웹·추적
```

각 도메인은 필요에 따라 `controller`, `model`, `dto`, `service`, `persistence`, `mapper`, `enums`, `exception`으로 분리함. Controller는 HTTP, Service는 비즈니스 규칙, Persistence는 DB 접근만 담당함. Entity는 persistence 계층 외부에 노출하지 않음

## 도메인 모델

| 도메인 | 핵심 개념 |
| --- | --- |
| auth | Account, Role, Permission, AccessToken, RefreshToken |
| account | Member, AdminProfile, Address, Contact |
| catalog | Product, ProductOption, Category, Brand, ProductImage |
| cart | Cart, CartItem, SelectedOption |
| order | Order, OrderItem, OrderStatus, ShippingRequest, CancelRequest, RefundRequest |
| payment | Payment, PaymentMethod, PaymentStatus, ProviderTransaction |
| inventory | Stock, StockChange, StockReservation |
| operation | AdminActionLog, OperatorMemo, ProductChangeHistory, OrderHandlingHistory |

상품의 판매·재고 단위는 SKU이며, 주문은 상품명·SKU명·가격을 스냅샷으로 보관함. 재고는 주문 생성 시 예약하고 결제 승인 시 확정 차감함

## API 규칙

- 기본 prefix: `/api`
- 역할 구분은 URL prefix가 아니라 서버 role/permission 검사로 수행
- 관리자성 후보 prefix: `/api/operation/**`
- 목록 API는 `page`, `size`, `sort`를 기본 후보로 사용하고 무한 스크롤 화면은 cursor 방식을 별도 검토
- OpenAPI tag는 도메인·역할이 드러나게 작성

응답 형식은 구현 전에 공통화함

```json
{ "data": {}, "traceId": "..." }
```

```json
{ "code": "ERROR_CODE", "message": "...", "traceId": "...", "details": [] }
```

WebView 흐름에서는 인증 만료, 앱 뒤로가기, 외부 PG 이동·복귀, 파일 업로드, 푸시 토큰 등록을 계약에 포함함

## 인증과 권한

초기 모델은 `Account`, `Role`, `Permission`, `AccountRole`, `RolePermission`임. Role 후보는 `CUSTOMER`, `ADMIN`, `PRODUCT_MANAGER`, `ORDER_MANAGER`, `INVENTORY_MANAGER`, `SUPER_ADMIN`임

토큰은 RS256 Access Token과 Refresh Token을 사용함. Access Token은 짧은 수명으로 유지하고, Refresh Token은 기기별 영속 세션으로 관리함. 명시적 토큰 삭제·무효화, 계정 정지·탈퇴, `tokenVersion` 증가 또는 운영자 강제 종료 전까지 Refresh Token의 시간 만료를 두지 않음. 원문은 클라이언트 보관·서버 SHA-256 hash 저장 방식이며, 갱신마다 계정 상태와 권한을 DB에서 재검증하고 토큰을 회전함. JWT 개인키는 설정 파일이 아닌 PEM 파일 경로로 주입함

JWT claim 후보: `sub`, `iss`, `aud`, `roles`, `tokenVersion`, `iat`, `exp`

휴대폰 번호 로그인, 계정 상태 기반 본인 인증, 기존 회원 활성화, 개인정보 동의 흐름은 `services/authentication.md`와 루트 `SERVICE_FLOW.md`를 기준으로 함

## 데이터 접근과 향후 분리

read/write datasource를 유지하고 `@Transactional(readOnly = true)`는 read 연결로 라우팅함. 접속 정보는 환경 변수로 주입함

아래 조건이 누적되면 모듈 또는 서비스 분리를 검토함

- 특정 도메인의 독립 배포·확장 요구
- Kafka consumer·batch의 별도 확장 요구
- 관리자 운영 기능과 사용자 트래픽의 보안·성능 요구 차이
- 공통 코드보다 도메인 독립성이 중요해진 경우
