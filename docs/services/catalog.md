# 카탈로그 서비스

## 책임

카테고리, 브랜드, 상품, 이미지, 옵션, SKU의 공개 조회와 관리자 관리 담당

## Controller

```text
GET  /api/categories
GET  /api/products
GET  /api/products/{productId}
POST /api/operation/products
PATCH /api/operation/products/{productId}
```

현재 공개 조회 구현 범위: `GET /api/categories`, `GET /api/products`, `GET /api/products/{productId}`. 운영 등록·수정 endpoint는 후속 구현 대상

## 핵심 규칙

- 공개 조회는 판매·노출 상태가 유효한 상품만 반환
- 상품 가격·재고의 실제 단위는 SKU, 상품은 대표 정보 담당
- 운영 변경은 `PRODUCT_WRITE` 권한과 감사 로그 필수
- 이미지 원본은 object storage, DB에는 파일 key와 메타데이터만 저장
