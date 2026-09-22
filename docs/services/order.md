# 주문 서비스

## 책임

주문 생성, 주문 내역·상세 조회, 상태 조회, 취소·환불 요청 담당

## Controller

```text
POST /api/orders
GET  /api/orders
GET  /api/orders/{orderId}
POST /api/orders/{orderId}/cancellations
```

## 핵심 규칙

- 주문 생성은 가격 스냅샷과 재고 예약을 단일 업무 흐름으로 처리
- 주문 상태 변경 이력은 append-only로 보관
- 고객은 본인 주문만 조회·취소 요청 가능
- 운영 상태 변경은 운영 서비스에서 권한 검사와 감사 로그 적용

## 현재 구현 범위

- `POST /api/orders`: 현재 사용자의 장바구니 항목으로 주문 생성
- SKU의 상품명·SKU명·SKU 코드·판매가·수량·주문 금액을 주문 항목에 스냅샷으로 저장
- 주문 생성과 같은 트랜잭션에서 SKU 재고를 예약하고, 장바구니 항목을 비움
- 주문 목록·상세 조회는 토큰 `sub`의 본인 계정으로 제한

초기 주문 상태는 `PENDING_PAYMENT`이며, 결제 승인·실패·취소 상태 전이는 결제 feature에서 구현함
