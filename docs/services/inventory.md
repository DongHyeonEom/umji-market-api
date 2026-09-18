# 재고 서비스

## 책임

SKU 재고, 주문 재고 예약·해제·확정, 재고 이동 이력 담당

## Controller

```text
GET   /api/operation/inventory/skus/{skuId}
PATCH /api/operation/inventory/skus/{skuId}
GET   /api/operation/inventory/movements
```

## 핵심 규칙

- 가용 재고 = 실재고 - 예약 재고
- 주문 생성 시 비관 잠금으로 재고를 예약, 결제 승인 시 확정 차감
- 결제 실패·만료·취소 시 예약 해제
- 재고 이동 이력은 수정·삭제하지 않음
- 모든 운영 변경은 `INVENTORY_WRITE` 권한과 감사 로그 필수
