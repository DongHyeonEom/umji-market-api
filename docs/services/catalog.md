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
POST /api/operation/products/{productId}/images
POST /api/operation/products/{productId}/options
POST /api/operation/products/{productId}/skus
```

공개 조회 구현 범위는 `GET /api/categories`, `GET /api/products`, `GET /api/products/{productId}`임. 운영자는 상품 기본 정보와 상태를 수정하고, 이미지·옵션·SKU를 추가할 수 있음. 옵션 SKU는 옵션 생성 후 상품 상세 응답의 옵션값 ID를 사용해 별도 등록함

## 핵심 규칙

- 공개 조회는 판매·노출 상태가 유효한 상품만 반환
- 상품 가격·재고의 실제 단위는 SKU, 상품은 대표 정보 담당
- 운영 변경은 `PRODUCT_WRITE` 권한과 감사 로그 필수
- 이미지 원본은 object storage, DB에는 파일 key와 메타데이터만 저장
- SKU에 연결하는 옵션값은 반드시 같은 상품의 옵션값이어야 함
