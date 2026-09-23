# 재고

## 운영 endpoint

- `GET /api/operation/inventory/skus/{skuId}`
- `PATCH /api/operation/inventory/skus/{skuId}`
- `GET /api/operation/inventory/skus/{skuId}/movements?page=&size=`

## 동작

- 가용 재고는 실재고에서 예약 재고를 뺀 값입니다.
- 운영 조정은 재고 이동 이력을 함께 기록합니다.
- 주문 생성은 재고를 예약하고 예약 식별자를 주문 항목에 보관합니다.
- 결제 승인/실패·만료·취소 흐름은 결제 기능이 아직 없어 미연결입니다. 예약 확정·해제 use case는 제공됩니다.

재고 UseCase는 SKU 식별자와 재고 Port를 사용합니다. 카탈로그 SKU 저장 구현은 outbound adapter를 통해 조회합니다.
