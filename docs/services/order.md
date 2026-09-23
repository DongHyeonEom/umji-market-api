# 주문

## Endpoint

- `POST /api/orders`
- `GET /api/orders?page=&size=`
- `GET /api/orders/{orderId}`

## 주문 생성

현재 인증 계정의 장바구니를 기준으로 주문을 생성함. 주문 항목에는 상품명, SKU명·코드, 가격, 수량을 스냅샷으로 저장함. 같은 유스케이스 흐름에서 재고를 예약하고 장바구니 항목을 초기화. 초기 상태는 `PENDING_PAYMENT`임.

주문 조회는 토큰 subject의 계정으로 제한함. 결제·주문 취소·환불 API 및 상태 전이는 아직 구현되지 않았음. application UseCase가 장바구니·재고·주문 저장 Port를 조정함.
