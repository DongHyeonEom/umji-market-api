# 데이터베이스 ERD

엄지마켓의 논리 데이터 모델. 현재 Flyway `V2`는 인증·계정 영역만 생성되어 있으며, 나머지는 후속 마이그레이션으로 추가할 목표 모델임. 상세 컬럼·제약·마이그레이션 순서는 `database.md`를 기준으로 함

## ExERD 작성 기준

- 내부 PK: `BIGINT` auto increment
- 외부 공개 식별자: `public_id BINARY(16)`
- 시간: UTC `DATETIME(3)`
- 금액: KRW 최소 단위 `BIGINT`
- 상태: `VARCHAR` 코드
- 주문·결제·재고·운영 이력은 append-only

## 관계도

```mermaid
erDiagram
    ACCOUNT {
        BIGINT id PK
        BINARY public_id UK
        VARCHAR phone
        VARCHAR status
        BIGINT token_version
    }
    ROLE {
        BIGINT id PK
        VARCHAR code UK
    }
    PERMISSION {
        BIGINT id PK
        VARCHAR code UK
    }
    ACCOUNT_ROLE {
        BIGINT account_id FK
        BIGINT role_id FK
    }
    ROLE_PERMISSION {
        BIGINT role_id FK
        BIGINT permission_id FK
    }
    REFRESH_TOKEN {
        BIGINT id PK
        BIGINT account_id FK
        BINARY token_hash UK
        DATETIME expires_at
        DATETIME revoked_at
    }
    ACCOUNT_ADDRESS {
        BIGINT id PK
        BIGINT account_id FK
        VARCHAR recipient_name
        VARCHAR recipient_phone
    }
    CONSENT_HISTORY {
        BIGINT id PK
        BIGINT account_id FK
        VARCHAR consent_type
        VARCHAR document_version
        VARCHAR method
        DATETIME consented_at
    }
    BUSINESS_PROFILE {
        BIGINT id PK
        BIGINT account_id FK
        VARCHAR business_name
        VARCHAR business_registration_number
        VARCHAR business_status
    }
    PHONE_AUTH_CHALLENGE {
        BIGINT id PK
        VARCHAR phone
        BINARY code_hash
        DATETIME expires_at
        DATETIME verified_at
    }
    CATEGORY {
        BIGINT id PK
        BIGINT parent_id FK
        BINARY public_id UK
        VARCHAR name
    }
    BRAND {
        BIGINT id PK
        BINARY public_id UK
        VARCHAR name
    }
    PRODUCT {
        BIGINT id PK
        BIGINT category_id FK
        BIGINT brand_id FK
        BINARY public_id UK
        VARCHAR name
    }
    PRODUCT_IMAGE {
        BIGINT id PK
        BIGINT product_id FK
        VARCHAR file_key
    }
    PRODUCT_OPTION {
        BIGINT id PK
        BIGINT product_id FK
        VARCHAR name
    }
    PRODUCT_OPTION_VALUE {
        BIGINT id PK
        BIGINT option_id FK
        VARCHAR value
    }
    PRODUCT_SKU {
        BIGINT id PK
        BIGINT product_id FK
        BINARY public_id UK
        VARCHAR sku_code UK
        BIGINT sale_price
    }
    SKU_OPTION_VALUE {
        BIGINT sku_id FK
        BIGINT option_value_id FK
    }
    CART {
        BIGINT id PK
        BIGINT account_id FK
    }
    CART_ITEM {
        BIGINT id PK
        BIGINT cart_id FK
        BIGINT sku_id FK
        INT quantity
    }
    PURCHASE_ORDER {
        BIGINT id PK
        BIGINT account_id FK
        BINARY public_id UK
        VARCHAR order_number UK
        VARCHAR status
        BIGINT total_amount
    }
    ORDER_ITEM {
        BIGINT id PK
        BIGINT order_id FK
        BIGINT sku_id FK
        BIGINT unit_price
        INT quantity
    }
    ORDER_STATUS_HISTORY {
        BIGINT id PK
        BIGINT order_id FK
        VARCHAR from_status
        VARCHAR to_status
    }
    PAYMENT {
        BIGINT id PK
        BIGINT order_id FK
        BINARY public_id UK
        VARCHAR status
        BIGINT requested_amount
    }
    PAYMENT_TRANSACTION {
        BIGINT id PK
        BIGINT payment_id FK
        VARCHAR provider_transaction_id UK
        BIGINT amount
    }
    CANCELLATION {
        BIGINT id PK
        BIGINT order_id FK
        VARCHAR status
    }
    REFUND {
        BIGINT id PK
        BIGINT cancellation_id FK
        BIGINT payment_id FK
        VARCHAR status
    }
    INVENTORY_STOCK {
        BIGINT id PK
        BIGINT sku_id FK
        INT on_hand_quantity
        INT reserved_quantity
    }
    INVENTORY_MOVEMENT {
        BIGINT id PK
        BIGINT sku_id FK
        INT quantity_delta
        VARCHAR movement_type
    }
    STOCK_RESERVATION {
        BIGINT id PK
        BIGINT sku_id FK
        BIGINT order_item_id FK
        INT quantity
        VARCHAR status
    }
    ADMIN_ACTION_LOG {
        BIGINT id PK
        BIGINT actor_account_id FK
        VARCHAR action_type
        VARCHAR target_type
    }

    ACCOUNT ||--o{ ACCOUNT_ROLE : has
    ROLE ||--o{ ACCOUNT_ROLE : grants
    ROLE ||--o{ ROLE_PERMISSION : has
    PERMISSION ||--o{ ROLE_PERMISSION : grants
    ACCOUNT ||--o{ REFRESH_TOKEN : owns
    ACCOUNT ||--o{ ACCOUNT_ADDRESS : has
    ACCOUNT ||--o{ CONSENT_HISTORY : consents
    ACCOUNT ||--o| BUSINESS_PROFILE : operates
    CATEGORY o|--o{ CATEGORY : parent
    CATEGORY ||--o{ PRODUCT : categorizes
    BRAND ||--o{ PRODUCT : brands
    PRODUCT ||--o{ PRODUCT_IMAGE : has
    PRODUCT ||--o{ PRODUCT_OPTION : has
    PRODUCT_OPTION ||--o{ PRODUCT_OPTION_VALUE : has
    PRODUCT ||--o{ PRODUCT_SKU : sells_as
    PRODUCT_SKU ||--o{ SKU_OPTION_VALUE : selects
    PRODUCT_OPTION_VALUE ||--o{ SKU_OPTION_VALUE : selected_by
    ACCOUNT ||--|| CART : owns
    CART ||--o{ CART_ITEM : contains
    PRODUCT_SKU ||--o{ CART_ITEM : selected
    ACCOUNT ||--o{ PURCHASE_ORDER : places
    PURCHASE_ORDER ||--o{ ORDER_ITEM : contains
    PRODUCT_SKU ||--o{ ORDER_ITEM : snapshot_source
    PURCHASE_ORDER ||--o{ ORDER_STATUS_HISTORY : changes
    PURCHASE_ORDER ||--o{ PAYMENT : has
    PAYMENT ||--o{ PAYMENT_TRANSACTION : records
    PURCHASE_ORDER ||--o{ CANCELLATION : requests
    CANCELLATION ||--o{ REFUND : results_in
    PAYMENT ||--o{ REFUND : refunds
    PRODUCT_SKU ||--|| INVENTORY_STOCK : has
    PRODUCT_SKU ||--o{ INVENTORY_MOVEMENT : moves
    PRODUCT_SKU ||--o{ STOCK_RESERVATION : reserves
    ORDER_ITEM ||--o| STOCK_RESERVATION : reserves
    ACCOUNT ||--o{ ADMIN_ACTION_LOG : acts
```

## 현재와 후속 범위

| 범위 | 테이블 | 상태 |
| --- | --- | --- |
| 인증·계정 | `account`, `role`, `permission`, 연결 테이블, `refresh_token`, `account_address` | V2 생성 |
| 회원 활성화 | `consent_history`, `phone_auth_challenge`, `business_profile` | 휴대폰 인증·신규 업체 정보 입력 구현 시 추가 |
| 카탈로그 | `category`부터 `sku_option_value` | 후속 Flyway |
| 장바구니 | `cart`, `cart_item` | 후속 Flyway |
| 주문·결제 | `purchase_order`부터 `refund` | 후속 Flyway |
| 재고·운영 | `inventory_stock`부터 `admin_action_log` | 후속 Flyway |

## ExERD 사용 방법

ExERD에서 이 문서의 엔터티·PK·FK 관계를 기준으로 테이블을 배치함. 실제 DDL을 반영할 때는 `database.md`의 컬럼, unique index, check constraint, 삭제·이력 정책을 우선함. ERD 변경은 먼저 Flyway 마이그레이션과 `database.md`를 갱신한 뒤 이 문서에 반영함
