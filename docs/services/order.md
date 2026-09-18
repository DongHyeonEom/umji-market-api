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
