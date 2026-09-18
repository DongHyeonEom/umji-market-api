# 결제 서비스

## 책임

결제 요청, PG·계좌이체 승인 결과, 결제 거래 이력, 환불 실행 담당

## Controller

```text
POST /api/orders/{orderId}/payments
POST /api/payments/callbacks/{provider}
GET  /api/payments/{paymentId}
```

## 핵심 규칙

- PG callback은 외부 거래 ID unique 제약과 잠금으로 멱등 처리
- 결제 승인 시 주문 상태·재고 예약 확정 흐름과 연결
- 거래 원문에서 카드·계좌 등 민감정보를 저장하지 않음
- 환불 승인·실행은 주문 서비스 및 운영 서비스의 상태 전이와 일치 필요
