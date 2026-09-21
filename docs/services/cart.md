# 장바구니 서비스

## 책임

활성 회원의 장바구니와 SKU별 수량 관리 담당

## Controller

```text
GET    /api/cart
POST   /api/cart/items
PATCH  /api/cart/items/{itemId}
DELETE /api/cart/items/{itemId}
```

## 핵심 규칙

- 회원당 활성 장바구니 하나 유지
- 항목은 SKU 기준으로 중복 금지
- 장바구니는 현재 가격을 표시할 뿐 가격을 확정하지 않음
- 주문 생성 시점에 가격·상품 정보 스냅샷과 재고 예약 수행
- 장바구니 API는 Access Token의 `sub`로 현재 `ACTIVE` 계정을 식별하며, 요청에서 계정 식별자를 받지 않음

## 현재 구현 범위

- `V8__add_cart_tables.sql`의 장바구니·항목 테이블
- 장바구니 조회, SKU 항목 추가·수량 변경·삭제
- 판매 중인 SKU만 추가 가능하며 동일 SKU 추가 시 수량 합산
