# Domain Model

엄지철물마켓 MVP 도메인 초기 개념. Entity 설계 전 용어·관계 정렬 목적

DB 테이블·식별자·재고 예약·JPA 매핑 기준: `docs/database-design.md`

## Core Domains

### auth

로그인·토큰·세션·role/permission

초기 개념:

- Account
- Role
- Permission
- AccessToken
- RefreshToken

### account

서비스 계정. 일반 사용자·관리자 계정의 단일 Account role 구분 또는 별도 타입 여부 추후 결정

초기 개념:

- Member
- AdminProfile
- Address
- Contact

### catalog

상품 탐색·노출

초기 개념:

- Product
- ProductOption
- Category
- Brand
- ProductImage
- DisplayStatus

### cart

장바구니

초기 개념:

- Cart
- CartItem
- SelectedOption

### order

주문·주문 상태

초기 개념:

- Order
- OrderItem
- OrderStatus
- ShippingRequest
- CancelRequest
- RefundRequest

### payment

결제 요청·결과

초기 개념:

- Payment
- PaymentMethod
- PaymentStatus
- PaymentProviderTransaction

### inventory

재고

초기 개념:

- Stock
- StockChange
- StockReservation

### operation

관리자 운영 기능·감사 로그

초기 개념:

- AdminActionLog
- OperatorMemo
- ProductChangeHistory
- OrderHandlingHistory

## Open Questions

- 일반 사용자와 관리자 계정을 하나의 Account로 통합할지, 별도 테이블로 분리할지
- 상품 옵션을 단순 옵션으로 시작할지 복합 옵션으로 시작할지
- 재고 차감을 주문 생성 시점에 할지 결제 완료 시점에 할지
- 배송과 매장 픽업을 MVP에 동시에 넣을지
- 결제 PG사를 무엇으로 할지
