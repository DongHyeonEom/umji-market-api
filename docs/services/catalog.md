# 카탈로그

## 공개 조회

### 알고리즘 흐름

```mermaid
flowchart TD
    A[카탈로그 조회 요청] --> B{요청 종류}
    B -- 카테고리 목록 --> C[노출 카테고리 조회]
    B -- 상품 목록 --> D[페이지 입력 검증 및 공개 상품 조회]
    B -- 상품 상세 --> E[공개 상품 ID로 상세 조회]
    C --> F[application 조회 모델]
    D --> F
    E --> G{상품 조회 결과}
    G -- 없음 또는 비공개 --> H[미발견 오류]
    G -- 공개 상품 --> F
    F --> I[HTTP 응답 변환]
```

- 공개 조회 조건과 응답 구성은 catalog query adapter가 적용함.
- 관리자 카탈로그 변경 흐름은 [operation.md](operation.md)의 관리자 카탈로그 흐름을 기준으로 함.

- `GET /api/categories`
- `GET /api/products?page=&size=`
- `GET /api/products/{productId}`

공개 조회는 노출·판매 가능한 상품과 SKU를 반환함.
상품의 판매·재고 단위는 SKU임.

## 관리자 관리

- `GET /api/operation/categories`
- `POST /api/operation/categories`
- `GET /api/operation/brands?page=&size=`
- `POST /api/operation/brands`
- `GET /api/operation/products?page=&size=`
- `GET /api/operation/products/{productId}`
- `POST /api/operation/products`
- `PATCH /api/operation/products/{productId}`
- `PATCH /api/operation/products/{productId}/status`
- `POST /api/operation/products/{productId}/images`
- `POST /api/operation/products/{productId}/options`
- `POST /api/operation/products/{productId}/skus`

상품 생성 요청은 이미지·옵션·SKU를 함께 포함할 수 있음.
별도 SKU 추가 시 옵션값은 해당 상품에 속한 값이어야 함.
JPA Entity는 catalog persistence adapter 밖으로 전달하지 않음.
