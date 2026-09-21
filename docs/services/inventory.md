# 재고 서비스

## 책임

SKU 재고, 주문 재고 예약·해제·확정, 재고 이동 이력 담당

## Controller

```text
GET   /api/operation/inventory/skus/{skuId}
PATCH /api/operation/inventory/skus/{skuId}
GET   /api/operation/inventory/skus/{skuId}/movements
```

## 핵심 규칙

- 가용 재고 = 실재고 - 예약 재고
- 주문 생성 시 비관 잠금으로 재고를 예약, 결제 승인 시 확정 차감
- 결제 실패·만료·취소 시 예약 해제
- 재고 이동 이력은 수정·삭제하지 않음
- 모든 운영 변경은 `INVENTORY_WRITE` 권한과 감사 로그 필수

## 현재 구현 범위

- SKU별 실재고·예약 재고·안전 재고 조회
- 운영자의 수량 증감 및 안전 재고 조정, append-only 재고 변동 이력
- 주문 도메인이 사용할 재고 예약·해제·확정 Service 기반

주문 테이블은 아직 없으므로 예약은 내부 `reservationKey`로 기록함. 주문 생성 feature에서 주문 항목 식별자와 연결함. 운영 API의 서버 권한 검사는 인증 권한 매핑 feature에서 `INVENTORY_WRITE`로 연결함
