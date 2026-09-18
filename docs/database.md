# Database Design

## 목표와 전제

엄지마켓 MVP 관계형 데이터베이스 기준 설계. 상품 탐색·장바구니·주문·결제·재고·관리자 권한 우선 지원과 JPA 엔티티 경계·운영 이력 고려

- DBMS: 실행 설정·Flyway 마이그레이션 기준 **MySQL 8.0+**
- 애플리케이션 database: `umji_market`, 별도 서비스 전 도메인별 database 미분리
- 내부 FK: `BIGINT AUTO_INCREMENT`, API 식별자: `public_id BINARY(16)`, 순차 PK 노출·데이터 병합 위험 방지 및 JPA 조인 성능 확보
- 금액: KRW 최소 단위 `BIGINT`, `FLOAT`·`DOUBLE`·결제 금액 `DECIMAL` 사용 금지
- 시각: UTC `DATETIME(3)` 저장, 애플리케이션 경계 `Instant` 매핑
- 상태값: `VARCHAR` 코드 저장, Kotlin `enum class`의 `EnumType.STRING` 매핑, ordinal 저장 금지
- 주문·결제·재고 **이력 테이블**: 삭제·갱신 금지, 정정 시 신규 이력 행. 현재 상태 테이블은 상태 전이용 갱신 허용

## 공통 JPA 규칙

신규 도메인 엔티티의 현 스켈레톤 `Int` 기반 `BaseEntity` 상속 금지. 서비스 전용 `Long` 식별자 `DomainBaseEntity` 도입으로 스켈레톤 예제와 타입 충돌 방지

```kotlin
@MappedSuperclass
abstract class DomainBaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Version
    var version: Long? = null

    @CreatedDate @Column(updatable = false)
    var createdAt: Instant? = null

    @LastModifiedDate
    var updatedAt: Instant? = null
}
```

- 신규 도메인 기반 클래스: `DomainBaseEntity`, `DomainPublicEntity`, `DomainSoftDeletableEntity`
- `DomainBaseEntity`: nullable `Long` identity, `@Version`, UTC `Instant` 생성·수정 시각, 외부·하위 Entity setter 비공개
- `DomainPublicEntity`: `@PrePersist` UUID 생성, `public_id` 외부 식별자, setter 비공개
- `DomainSoftDeletableEntity`: `Instant` 삭제 시각·삭제자, 서비스 계층 전달 시각 기반 삭제·복원
- 변경 가능 Aggregate Root: `version BIGINT NOT NULL DEFAULT 0`, 상품·장바구니·재고·주문 상태 변경의 낙관적 잠금 기준
- `created_by`, `updated_by`: 인증 도입 후 변경 가능 마스터에만 추가, 시스템 작업 `NULL` 허용, 감사 컬럼에 의한 계정 행 생성 차단 금지
- 소프트 삭제: 복원 수요 마스터에만 적용, 활성 행 고유 키는 생성 열 기반 unique index 또는 서비스 트랜잭션으로 보장
- `@ManyToOne`: `fetch = LAZY`, `optional = false` 명시. 컬렉션 양방향 연관관계 최소화, 목록 조회 fetch join 또는 DTO projection
- `CascadeType.ALL`: 장바구니-항목, 주문-항목 등 동일 수명 주기 구성에만 사용. Account·Product·Payment·Inventory 전파 금지
- FK의 scalar id·연관 엔티티 중복 매핑 금지. id 단독 필요 시 JPQL projection 또는 `entity.id` 접근

## 관계 개요

```text
account --< account_role >-- role --< role_permission >-- permission
account --< account_address
account --1 cart --< cart_item >-- product_sku --< sku_option_value >-- product_option_value

category --< category (parent)
brand --< product --< product_image
product --< product_option --< product_option_value
product --< product_sku

account --< purchase_order --< order_item >-- product_sku
purchase_order --< order_status_history
purchase_order --< payment --< payment_transaction
purchase_order --< cancellation / refund
product_sku --1 inventory_stock --< inventory_movement
order_item --< stock_reservation

account(operator) --< admin_action_log
```

`purchase_order`: SQL 예약어·JPA 가독성 문제 회피용 명칭. 상품 판매 단위: `product_sku`, 옵션 미사용 상품도 기본 SKU 한 건 필수

## 테이블 명세

### 인증·계정

| 테이블 | 핵심 컬럼 | 제약/인덱스 | 비고 |
| --- | --- | --- | --- |
| `account` | `id`, `public_id`, `login_id`, `password_hash`, `name`, `phone`, `email`, `status`, `last_login_at` | `UQ public_id`, `UQ login_id`, 활성 `phone`/`email` 생성 열 기반 unique | 회원과 관리자 통합 계정 |
| `role` | `id`, `code`, `name`, `is_system` | `UQ code` | CUSTOMER, ADMIN 등 |
| `permission` | `id`, `code`, `name` | `UQ code` | 코드 기반 권한 |
| `account_role` | `account_id`, `role_id`, `granted_at`, `granted_by` | PK `(account_id, role_id)`, `IX role_id` | 명시적 연결 엔티티 |
| `role_permission` | `role_id`, `permission_id` | PK `(role_id, permission_id)` | 명시적 연결 엔티티 |
| `account_address` | `id`, `account_id`, `recipient_name`, `recipient_phone`, `postal_code`, `address1`, `address2`, `is_default` | `IX account_id, is_default` | 주문 시에는 복사본 사용 |
| `refresh_token` | `id`, `account_id`, `token_hash`, `device_id`, `expires_at`, `revoked_at` | `UQ token_hash`, `IX account_id, expires_at` | 토큰 원문 저장 금지 |

로그인 ID·전화번호: 정규화 값만 저장. 비밀번호: Argon2id 또는 BCrypt hash만 저장. 다중 기본 배송지: DB 단독 제약 대신 주소 변경 서비스 트랜잭션의 해제 후 설정

### 카탈로그

| 테이블 | 핵심 컬럼 | 제약/인덱스 | 비고 |
| --- | --- | --- | --- |
| `category` | `id`, `public_id`, `parent_id`, `name`, `path`, `depth`, `display_order`, `status`, `deleted_at` | 활성 부모별 이름 생성 열 기반 unique, `IX path` | 인접 리스트 + materialized path |
| `brand` | `id`, `public_id`, `name`, `status`, `deleted_at` | 활성 이름 생성 열 기반 unique | 상품과 N:1 |
| `product` | `id`, `public_id`, `category_id`, `brand_id`, `name`, `description`, `display_status`, `sales_status`, `search_text`, `deleted_at` | `IX category_id, display_status, display_order`, `IX brand_id`, full-text search index | 상품 자체는 재고를 갖지 않음 |
| `product_image` | `id`, `product_id`, `file_key`, `image_type`, `display_order`, `alt_text` | `UQ product_id, image_type, display_order` | 파일은 object storage, DB에는 key만 |
| `product_option` | `id`, `product_id`, `name`, `display_order` | `UQ product_id, name` | 색상·규격 등의 옵션 축 |
| `product_option_value` | `id`, `option_id`, `value`, `display_order` | `UQ option_id, value` | 옵션 선택지 |
| `product_sku` | `id`, `public_id`, `product_id`, `sku_code`, `option_signature`, `name`, `sale_price`, `list_price`, `cost_price`, `weight_gram`, `sales_status` | `UQ sku_code`, `UQ product_id, option_signature`, `IX product_id, sales_status` | 실제 판매·재고 단위 |
| `sku_option_value` | `sku_id`, `option_value_id` | PK `(sku_id, option_value_id)` | SKU 조합 구성 |

`product_sku.option_signature`: 옵션 값 ID의 옵션 순서 기반 정규화 내부 키(예: `12:37`). 동일 옵션 조합 SKU 중복 등록 방지용, API 미노출. 옵션 미사용 기본 SKU 값: 빈 문자열(`''`)

`product`의 가격·재고 미보관. 목록 성능용 대표 SKU 가격·썸네일 조회 projection, 트래픽 확인 후 별도 read model 도입

### 장바구니

| 테이블 | 핵심 컬럼 | 제약/인덱스 | 비고 |
| --- | --- | --- | --- |
| `cart` | `id`, `account_id`, `version` | `UQ account_id` | 회원당 활성 장바구니 하나 |
| `cart_item` | `id`, `cart_id`, `sku_id`, `quantity`, `version` | `UQ cart_id, sku_id`, `IX sku_id` | 선택된 옵션은 SKU가 표현 |

장바구니 가격 미저장 및 현재 SKU 가격 표시. 주문 생성 시점의 주문 항목 가격·상품명 스냅샷

### 주문·배송·결제

| 테이블 | 핵심 컬럼 | 제약/인덱스 | 비고 |
| --- | --- | --- | --- |
| `purchase_order` | `id`, `public_id`, `order_number`, `account_id`, `status`, `fulfillment_type`, `subtotal_amount`, `shipping_amount`, `discount_amount`, `total_amount`, 배송지 스냅샷, `ordered_at`, `version` | `UQ public_id`, `UQ order_number`, `IX account_id, ordered_at DESC`, `IX status, ordered_at DESC` | 주문 Aggregate Root |
| `order_item` | `id`, `order_id`, `sku_id`, `product_name`, `sku_name`, `sku_code`, `unit_price`, `quantity`, `line_amount`, `status` | `IX order_id`, `IX sku_id` | 상품/가격의 불변 스냅샷 |
| `order_status_history` | `id`, `order_id`, `from_status`, `to_status`, `reason_code`, `memo`, `changed_by`, `changed_at` | `IX order_id, changed_at` | 주문 상태 변경 원장 |
| `payment` | `id`, `public_id`, `order_id`, `payment_method`, `status`, `requested_amount`, `approved_amount`, `provider`, `provider_order_id`, `approved_at`, `version` | `UQ public_id`, `UQ provider, provider_order_id`, `IX order_id, created_at` | 주문당 여러 결제 시도, 현재 `BANK_TRANSFER`, 향후 `CARD` 확장 |
| `bank_transfer_payment` | `id`, `payment_id`, `transfer_type`, `bank_code`, `depositor_name`, `virtual_account_ciphertext`, `deposit_due_at`, `deposited_at` | `UQ payment_id`, `IX deposit_due_at, deposited_at` | 계좌이체 전용 세부 정보, 계좌번호 평문 저장 금지 |
| `payment_transaction` | `id`, `payment_id`, `transaction_type`, `provider_transaction_id`, `amount`, `raw_payload`, `occurred_at` | `UQ provider_transaction_id`, `IX payment_id, occurred_at` | PG 웹훅 원문은 접근 통제 |
| `cancellation` | `id`, `public_id`, `order_id`, `status`, `reason_code`, `requested_by`, `requested_at`, `approved_at` | `IX order_id, created_at` | MVP는 주문 전체 취소, 부분 취소 확장 가능 |
| `refund` | `id`, `public_id`, `cancellation_id`, `payment_id`, `status`, `requested_amount`, `refunded_amount`, `provider_refund_id`, `completed_at` | `UQ provider_refund_id`, `IX payment_id` | 실제 PG 환불 기록 |

주문 번호: 사람이 읽을 수 있는 별도 컬럼, 예: `UMJ-20260914-000001`. 생성: 주문 번호 전용 allocator 행의 비관 잠금 또는 애플리케이션 직렬화, `MAX + 1` 방식 금지. 배송·수령 주소·수령인 정보: 회원 주소 변경 영향 차단용 `purchase_order` 스냅샷 컬럼

MVP 결제 수단: `BANK_TRANSFER`. `bank_transfer_payment`의 무통장·가상계좌 구분(`transfer_type`), 은행 코드, 입금자명, 입금 기한, 입금 완료 시각 보관. 가상계좌 번호: 암호문만 보관. 수동 계좌이체: 운영 확인 또는 은행·PG 입금 콜백 기반 승인 상태 전이

결제 성공 콜백: `payment` 비관 잠금 조회 후 PG 거래 ID unique 제약 기반 최종 멱등성. PG raw payload의 민감 카드·계좌 정보 미보존, 보관 필요 시 암호화·접근 로그 적용. 카드 결제 도입 시 `payment_method = CARD`와 카드 PG 전용 detail 테이블 추가, `payment`·`payment_transaction`·주문 상태 전이 계약 유지

### 재고·운영 감사

| 테이블 | 핵심 컬럼 | 제약/인덱스 | 비고 |
| --- | --- | --- | --- |
| `inventory_stock` | `id`, `sku_id`, `on_hand_quantity`, `reserved_quantity`, `safety_stock_quantity`, `version` | `UQ sku_id`, `CK quantities >= 0` | 가용 재고 = 실재고 - 예약재고 |
| `inventory_movement` | `id`, `sku_id`, `movement_type`, `quantity_delta`, `reference_type`, `reference_id`, `memo`, `occurred_at`, `created_by` | `IX sku_id, occurred_at DESC`, `IX reference_type, reference_id` | 변경 불가 원장 |
| `stock_reservation` | `id`, `sku_id`, `order_item_id`, `quantity`, `status`, `expires_at`, `released_at` | `UQ order_item_id`, `IX status, expires_at` | 주문 항목별 예약 |
| `admin_action_log` | `id`, `actor_account_id`, `action_type`, `target_type`, `target_id`, `before_data`, `after_data`, `trace_id`, `occurred_at` | `IX target_type, target_id, occurred_at`, `IX actor_account_id, occurred_at` | 관리자 mutation 감사 |
| `operator_memo` | `id`, `target_type`, `target_id`, `content`, `created_by`, `created_at`, `deleted_at` | `IX target_type, target_id, created_at` | 주문·상품 등 운영 메모 |

재고 차감 정책: **주문 생성 시 예약, 결제 승인 시 확정 차감**. 예약 트랜잭션: `inventory_stock`의 `PESSIMISTIC_WRITE` 잠금, `on_hand_quantity - reserved_quantity >= 요청수량` 확인, `reserved_quantity` 증가. 결제 실패·만료·취소 시 예약 해제. 결제 승인 시 `on_hand_quantity`·`reserved_quantity` 감소와 각각의 `inventory_movement` 기록 단일 트랜잭션 처리

현재 상태·이력 분리: `purchase_order`, `payment`, `inventory_stock`은 현재 상태와 `@Version` 기반 동시성 제어용 갱신 테이블. `order_status_history`, `payment_transaction`, `inventory_movement`, `admin_action_log`는 발생 사실 보존용 append-only 이력 테이블. 정정·취소·환불은 기존 이력 수정 대신 보정 사실 신규 기록

## 핵심 DDL 패턴

아래 SQL: Flyway 구현용 대표 패턴. 실제 도입: `db/migration` 경로의 도메인별 버전 파일 분리

```sql
CREATE TABLE product_sku (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id BINARY(16) NOT NULL,
    product_id BIGINT NOT NULL,
    sku_code VARCHAR(64) NOT NULL,
    option_signature VARCHAR(512) NOT NULL DEFAULT '',
    name VARCHAR(200) NOT NULL,
    sale_price BIGINT NOT NULL,
    list_price BIGINT NULL,
    cost_price BIGINT NULL,
    weight_gram INT NULL,
    sales_status VARCHAR(30) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT UQ_product_sku_public_id UNIQUE (public_id),
    CONSTRAINT UQ_product_sku_code UNIQUE (sku_code),
    CONSTRAINT UQ_product_sku_option_signature UNIQUE (product_id, option_signature),
    CONSTRAINT CK_product_sku_sale_price CHECK (sale_price >= 0),
    CONSTRAINT CK_product_sku_list_price CHECK (list_price IS NULL OR list_price >= 0),
    CONSTRAINT FK_product_sku_product FOREIGN KEY (product_id) REFERENCES product(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX IX_product_sku_product_sales_status ON product_sku(product_id, sales_status);

CREATE TABLE inventory_stock (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku_id BIGINT NOT NULL,
    on_hand_quantity INT NOT NULL DEFAULT 0,
    reserved_quantity INT NOT NULL DEFAULT 0,
    safety_stock_quantity INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT UQ_inventory_stock_sku UNIQUE (sku_id),
    CONSTRAINT CK_inventory_stock_non_negative CHECK (
        on_hand_quantity >= 0 AND reserved_quantity >= 0 AND safety_stock_quantity >= 0
    ),
    CONSTRAINT CK_inventory_stock_reservation CHECK (reserved_quantity <= on_hand_quantity),
    CONSTRAINT FK_inventory_stock_sku FOREIGN KEY (sku_id) REFERENCES product_sku(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

## 무결성·삭제·보존 정책

- FK 기본 정책: `ON DELETE NO ACTION`, 업무 데이터 연쇄 삭제 방지
- 상품·브랜드·카테고리: `deleted_at` 소프트 삭제, 주문 SKU·주문 항목 물리 삭제 금지
- 계정 탈퇴: `account.status = WITHDRAWN` 및 PII 익명화. 주문·결제 법정 보존 데이터의 보존 정책 확정 전 제거 금지
- `product_image.file_key`, PG 거래 식별자, 전화번호·주소 포함 JSON: 로그 원문 기록 금지
- 관리자 변경: `updated_at` 외 `admin_action_log` before/after JSON 별도 기록

## Flyway 도입 순서

엄지마켓 전용 write datasource 단일 Flyway 이력 적용 순서

1. `V2__create_auth_account_tables.sql`: account, role, permission, 연결 테이블, address, refresh token
2. `V3__create_catalog_tables.sql`: category, brand, product, image, option, SKU
3. `V4__create_cart_tables.sql`: cart, cart item
4. `V5__create_order_payment_tables.sql`: purchase order, order item, payment, bank transfer payment, cancellation/refund
5. `V6__create_inventory_operation_tables.sql`: stock, reservation, movement, operation log
6. `R__seed_system_roles_and_permissions.sql`: 시스템 role/permission upsert seed

운영 반영 마이그레이션 수정 금지. 컬럼 변경: 후속 버전 추가. 대량 데이터 backfill·NOT NULL/unique 제약: 최소 두 단계 배포

## 구현 전 확정이 필요한 정책

- 계좌이체 방식: 무통장, 가상계좌, 외부 은행 API 중 MVP 적용 범위
- PG사 및 부분 취소·부분 환불 범위
- 배송과 매장 픽업의 MVP 포함 여부, 배송비·도서산간 정책
- 자체 로그인, 소셜 로그인, 휴대폰 인증 중 최초 인증 수단
- 상품 옵션 조합의 최대 수와 SKU 코드 발급 규칙
- 주문/결제/개인정보의 법정 보존 기간 및 개인정보 익명화 기준

정책 확정 전 구현 범위: Catalog, Cart, 주문 생성, 재고 예약. PG·배송: 확장 테이블 추가 방식 연결
