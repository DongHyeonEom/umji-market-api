# 데이터베이스

## 기준과 출처

현재 스키마와 시스템 role·permission seed는 MySQL 8.0 이상과 Flyway V2–V50으로 관리함.<br>
실제 DDL과 제약의 단일 기준은 `src/main/resources/db/migration`임.<br>
이 문서는 공통 규칙과 현재 테이블 구성을 요약하며, 상세 관계는 [database-erd.md](database-erd.md)를 참고.<br>

## 공통 규칙

- 내부 PK/FK는 `BIGINT AUTO_INCREMENT`, API 공개 식별자는 UUID `BINARY(16)`을 사용함.<br>
- 금액은 KRW 최소 단위 `BIGINT`, 시각은 UTC `DATETIME(3)`로 저장하고 애플리케이션에서 `Instant`로 처리.<br>
- 상태는 문자열 코드로 저장함.<br>
  Kotlin enum을 사용하면 이름 기반 매핑을 사용하고 ordinal 저장을 금지함.<br>
- 신규/변경 스키마는 Flyway로만 적용함.<br>
  이미 적용된 version migration은 수정하지 않음.<br>
- 주문 상태 이력과 재고 이동은 append-only로 기록함.<br>
  현재 상태 테이블은 업무 상태 전이를 위해 갱신할 수 있음.<br>
- FK 기본 정책은 연쇄 삭제를 피함.<br>
  과거 주문에서 참조하는 상품/SKU를 물리 삭제하지 않음.<br>
- JPA Entity와 Spring Data Repository는 해당 도메인의 persistence 계층에 배치.<br>
  HTTP 응답에는 JPA Entity를 직접 노출하지 않음.<br>

## 현재 테이블과 마이그레이션

| 버전 | 테이블 및 변경 |
| --- | --- |
| V2 | `account`, `role`, `permission`, `account_role`, `role_permission`, `refresh_token`, `account_address` |
| V3 | `category`, `brand`, `product`, `product_sku` |
| V4 | 계정 로그인 필드 nullable 변경, `business_profile`, `consent_history` |
| V5 | `product_image`, `product_option`, `product_option_value`, `product_sku_option_value` |
| V6 | 정규화 전화번호 unique, refresh token 변경 |
| V7 | `inventory_stock`, `inventory_movement`, `stock_reservation` |
| V8 | `cart`, `cart_item` |
| V9 | `purchase_order`, `order_number_sequence`, `order_item`, `order_status_history` |
| V10 | 시스템 role-permission 기본 매핑 |
| V11 | `operation_audit_log`, `ADMIN_AUDIT_READ` permission 및 `SUPER_ADMIN` role mapping |
| V12 | `order_payment`, `order_payment_status_history`; 기존 주문의 초기 결제 상태 생성 |
| V13 | 계정별 세금계산서 발행 기본값과 주문별 발행 여부·입금 계좌 스냅샷 |
| V14 | `order_shipment`; 기존 주문의 배송 준비 상태 backfill |
| V15 | 결제 이슈 검토 상태 허용 |
| V16 | 공휴일 일정, 주문 취소 이력, 환불 상태 허용 |
| V17 | 배송 상태 `DELIVERED` 허용 |
| V18 | `buyer_group`, `buyer_group_member`, `buyer_group_business_profile`; 주문 구매자 그룹 귀속 및 기존 데이터 backfill |
| V19 | 계정당 그룹 한 곳으로 제한, 그룹 UUID 저장 형식 정규화, 주문 그룹 귀속 필수화 |
| V20 | 계정별 배송지를 그룹 공용 배송지로 이관, 주문 배송지 snapshot 컬럼 추가 |
| V21 | 그룹 대표자, 활성 구성원 재가입 이력, 전화번호 초대 및 가입 요청 테이블 추가 |
| V22 | 복수 구성원 그룹의 초기 대표자를 가장 먼저 생성된 계정으로 고정 |
| V23 | `SHIPPING_MANAGER` role 및 배송 전용 `SHIPMENT_WRITE` permission seed, 운영자 role permission mapping |
| V24 | `notification_outbox` 알림 이벤트 outbox 테이블 |
| V25 | outbox worker의 `PROCESSING` 상태와 만료 lease 추가 |
| V26 | 활성 계정별 FCM·APNs 기기 token 저장 |
| V27 | 관리자 계정의 암호화 TOTP secret 및 등록 상태 저장 |
| V28 | Refresh Token 세션의 2차 인증 완료 상태 저장 |
| V29 | MFA 적용 전 상위 관리자 token version 갱신 및 기존 refresh session 폐기 |
| V30 | 웹 로그인 실패 횟수 제한용 전화번호·원격 주소 hash와 15분 window 저장 |
| V31 | 그룹 세금계산서 업태·종목·이메일, 주문별 공급자·공급받는자 snapshot 및 송장 등록 후 발행 준비 일자 추가 |
| V32 | 사용자 신규 사업자 그룹 등록 시 국세청 상태 확인 완료 시각 저장 |
| V33 | 운영자 사전등록 사업자 그룹 상태조회 및 대표자 확인 상태 저장 |
| V34 | 기존 사업자 상태확인 시각을 대표자 정보 확인 시각으로 backfill |
| V35 | `sales_channel`, 채널별 `category`, `channel_product_listing`, `sales_offer` 추가 및 기존 상품·SKU·장바구니·주문 `WHOLESALE` backfill; 주문 채널·오퍼 참조 추가 |
| V36 | `sales_offer.units_per_sale` 및 `order_item.units_per_sale` 추가, 기존 데이터는 1로 backfill |
| V37 | `SALES_MANAGER` role 및 그룹 생성·조회, 본인 인센티브 조회 permission 추가. `ADMIN`·`SUPER_ADMIN`에는 영업 permission 전체 연결 |
| V38 | 구매자 그룹·구성원·초대·주소·사업자 프로필·주문 FK를 Organization 명칭으로 전환. `organization_capability` 추가 및 기존 조직의 `BUYER` capability backfill. account 사업자 정보 중 기존 공통 프로필에서 비어 있는 값 이관 |
| V39 | `organization_profile`을 `organization_business_profile`로 명칭 변경 |
| V40 | 세금계산서 기본 발행 설정을 `account`에서 `organization`으로 이동. 대표자 설정을 우선 이관하고 대표자가 없는 경우 활성 구성원 중 가장 작은 계정 ID의 설정을 사용 |
| V41 | 주문별 세금계산서 발행 상태와 양측 snapshot을 선택형 `purchase_order_tax_invoice`로 분리. 기존 요청 주문 데이터 이관 후 `purchase_order`의 세금계산서 전용 컬럼 제거 |
| V42 | 판매 오퍼·재고 원장에 Organization 범위 추가 및 장바구니 항목의 오퍼별 선택 지원. 외래 키 유지용 인덱스를 보존하고 판매 오퍼·재고의 Organization 복합 유일성 적용. 기존 오퍼·재고는 소유자를 추정할 수 없어 nullable 레거시 행으로 보존 |
| V43 | 브랜드·상품 소유 Organization 연결 및 상품별 SKU 코드 유일성으로 전환. 기존 브랜드·상품은 소유자를 추정하지 않고 nullable 레거시로 보존 |
| V44 | 파일 분류·소유 계정·불투명 저장 key·MIME·크기·업로드 상태 metadata를 보관하는 `file_asset` 추가. 원본 파일은 저장하지 않음 |
| V45 | `file_asset`에 공통 낙관적 잠금 버전과 수정 시각 추가 |
| V46 | audience별 화면·permission mapping 및 구매자 구성원 역할 permission 추가 |
| V47 | 구매 Organization별 영업 담당자·선택 인센티브율의 유효기간 배정 이력과 배정 permission 추가 |
| V48 | 주문별 인센티브 기준·요율 snapshot, 정산 상태·append-only 이벤트 원장 및 관리자 정산 permission 추가 |
| V49 | 주문 출처·생성 관리자 기록, 홈택스 수기 세금계산서 발행 필드와 append-only 이벤트 추가 |
| V50 | Organization 프로필로 이관 완료된 레거시 `business_profile` 제거 |

시스템 role·permission seed는 `R__seed_system_roles_and_permissions.sql`에 있음.<br>

## 도메인 데이터 규칙

- `brand`와 `product`는 소유 Organization을 가질 수 있으며, 신규 판매자 상품은 둘 다 같은 Organization에 속함. `product_sku`·이미지·옵션은 소속 상품을 통해 소유권을 상속.<br>
  V43 이전 브랜드·상품은 판매자 소유자를 추정하지 않고 `organization_id IS NULL`인 레거시 범위로 보존. 레거시 상품은 관리자 카탈로그 API에서 관리.<br>
  SKU 코드는 상품별로 유일하며 서로 다른 Organization 상품에서 같은 코드를 사용할 수 있음.<br>
  신규 `sales_offer`는 상품 소유 Organization·채널·SKU와 같은 Organization 범위에서 가격·판매 상태를 소유함. V42 이전에 생성된 Organization 오퍼는 소유자 미지정 레거시 상품을 계속 참조할 수 있음.<br>
  재고는 `inventory_stock`에서 Organization·SKU별로 분리하고 `(organization_id, sku_id)` 조합으로 유일성을 보장. `sales_offer`는 `(organization_id, sales_channel_id, product_sku_id)` 조합으로 유일성을 보장하며, 채널·SKU 외래 키에는 별도 인덱스를 유지. 한 Organization의 채널별 오퍼는 같은 재고를 공유.<br>
  V42 이전 오퍼·재고의 판매자 소유권은 migration에서 임의로 추정하지 않으며 `organization_id IS NULL`인 레거시 범위로 보존.<br>
  기존 상품·SKU 및 기존 주문은 WHOLESALE로 backfill.<br>
- SKU가 참조하는 옵션값은 같은 상품의 옵션에 속해야 함.<br>
- 장바구니 항목은 상품 소유 Organization·채널의 `sales_offer`를 참조하며 offer 가격을 표시. 장바구니 안에서 오퍼별 항목을 별도 보유.<br>
  주문은 단일 판매 채널로 생성하고 주문 항목의 상품명·SKU명·코드·가격 snapshot 및 offer 참조를 보관.<br>
- 주문 생성 시 재고를 예약하고, 평일 15:00 배송 준비 전환에서 확정함.<br>
  READY_TO_SHIP 취소는 예약을 해제하고, PREPARING 취소 승인 시 확정 재고를 복구함.<br>
  예약 식별자는 주문 항목과 재고 예약에 같은 UUID를 보관하며 두 행 사이 DB FK는 없음.<br>
- Refresh Token 원문은 저장하지 않고 hash를 저장함.<br>
  토큰 만료·폐기 의미는 인증 구현과 일치.<br>
- `account.password_hash`는 웹 전용 비밀번호 해시를 저장하며 앱 휴대폰 로그인과 분리됨.<br>
  관리자 TOTP secret은 애플리케이션 환경 변수 키로 AES-GCM 암호화해 저장하고, Refresh Token은 MFA 완료 상태를 보존함.<br>
- `web_login_attempt`은 전화번호·원격 주소 SHA-256 hash별 최근 실패 횟수만 저장함.<br>
  15분 내 동일 조합 5회 또는 전화번호 전체 합계 10회 실패 시 로그인 거부, 15분이 지난 행은 매시 정리.<br>
- 업체 동의 이력은 기존 행을 덮어쓰지 않고 새 이력으로 추가함.<br>
- `file_asset`은 파일 분류·소유 계정·원본 파일명·불투명 저장 key·MIME·크기·업로드 token hash·상태와 만료 시각을 저장. 원본 byte는 DB에 저장하지 않음.<br>
  공개 상품 이미지와 사업자 증빙은 분리된 로컬 파일 저장 경로를 사용하며 storage root는 `UMJI_FILE_STORAGE_ROOT`로 지정.<br>
- `ui_screen`은 화면 code·audience·route key·permission 결합 방식을 보관. `ui_screen_permission`은 화면별 조회 permission을, `organization_role_permission`은 미소속·대표자·일반구성원별 permission을 보관.<br>
  Access context는 활성 계정과 현재 role·구성원 관계로 허용 화면을 계산하며 화면 표시용 metadata를 반환. 업무 API 권한·소유권 검사를 대체하지 않음.<br>
- `organization_sales_assignment`는 구매 Organization의 담당 영업자·선택 인센티브율과 유효기간 이력을 보관. 담당자 또는 요율 변경은 기존 행 종료 후 새 행 추가로 보존하며, 현재 담당 변경은 Organization 행 잠금으로 직렬화.<br>
  `SALES_GROUP_ASSIGN`은 `ADMIN`·`SUPER_ADMIN`에게만 부여. 배정 endpoint는 활성 구매 Organization과 활성 `SALES_MANAGER` 계정을 요구.<br>
- `sales_commission`은 주문 시점의 구매 Organization·담당 영업자·요율·세금 제외 상품 순판매액·계산 인센티브 snapshot을 주문당 한 건 저장. 배송완료와 전액 입금 후 월말 정산 시 `PAYABLE`, 지급 시 `PAID`, 취소·환불 시 `REVERSED`로 상태 전이.<br>
  `sales_commission_event`는 snapshot·월 정산 발생·reversal·지급 이벤트를 append-only로 기록하며 idempotency key로 재처리를 방지. 부분 환불도 주문 전체 인센티브 금액을 취소.<br>
  정산 대상 월은 한국 시간 기준 자격 충족 시각의 월. `SALES_COMMISSION_READ_ALL` 및 `SALES_COMMISSION_SETTLE`은 `ADMIN`·`SUPER_ADMIN`에만 부여.<br>
- 구매 주문은 `organization_id`로 구매 Organization에 귀속하고, 기존 `purchase_order.account_id`는 실제 주문한 계정으로 유지함.<br>
  현재 각 기존 계정에 개인 또는 사업자 구매자 그룹 하나를 생성해 기존 주문·프로필을 backfill함.<br>
  V19에서 `organization_id`를 필수화하며 신규 주문 생성 시 활성 계정의 그룹 ID를 저장해야 함.<br>
- `organization.organization_type`은 `BUSINESS` 또는 `INDIVIDUAL`이며, 사업자번호는 선택 정보임.<br>
  사업자 그룹 식별자나 그룹 병합 키로 사용하지 않음.<br>
- `organization_capability`는 `BUYER`, `SELLER`, `OPERATOR` 중 Organization이 수행하는 역할을 저장. Organization은 capability를 복수로 보유 가능하며 V38에서 기존 Organization 전체에 `BUYER`를 backfill.<br>
- `organization_member`는 구성원 소속 이력을 보존하며, 계정당 동시 활성 그룹 소속은 하나로 제한함.<br>
  V21의 generated `active_account_id` unique 제약으로 동시 활성 소속을 하나로 제한.<br>
  그룹 이동 시 이전 소속은 `LEFT`로 종료하고 새 활성 소속을 추가. 주문의 과거 그룹 귀속은 변경하지 않음.<br>
- `organization.default_tax_invoice_requested`는 구매 Organization의 기본 세금계산서 발행 여부이며 구성원이 공유함.<br>
  대표자만 주문 흐름에서 기본값을 갱신할 수 있고, 주문별 선택은 `purchase_order.tax_invoice_requested`에 별도 저장함.<br>
- `organization.representative_account_id`는 현재 대표 계정이며 반드시 활성 구성원이어야 함.<br>
  대표자 지정·변경은 운영자 권한으로만 수행. 개인 그룹 최초 생성자는 대표자로 지정됨.<br>
- `organization_invitation`은 대표자가 전화번호로 보낸 초대 이력이며, 초대 대상 계정이 수락해야 그룹 소속 변경.<br>
  초대 수락은 기존 그룹 대표자 계정에 대해 허용하지 않음.<br>
- `organization_join_request`는 일반 구성원의 가입 요청 및 대표자의 처리 이력.<br>
  대표자만 그룹 가입 요청을 승인·거절할 수 있음.<br>
- `organization_address`는 구매자 그룹 공용 배송지임.<br>
  그룹 구성원은 주소를 공동 조회·관리하고 기본 배송지는 그룹당 최대 하나로 유지함.<br>
  생성 계정은 이력 식별용이며 주소 접근 범위는 구매자 그룹 기준.<br>
- `purchase_order`의 배송지 snapshot은 주문 당시 수령인·연락처·주소를 보존함.<br>
  주소 원본과 외래 키를 두지 않아 그룹 주소 변경·삭제가 기존 주문에 영향을 주지 않음.<br>
- `purchase_order.order_source`는 `CUSTOMER` 또는 `ADMIN_PHONE`이며 `created_by_account_id`는 실제 요청을 수행한 계정을 기록. 기존 주문은 주문 계정으로 생성자를 backfill.<br>
- `purchase_order_tax_invoice`는 세금계산서를 요청한 주문에만 생성하며 발행 상태·일자와 공급자·공급받는자 정보를 주문 시점 snapshot으로 보관함.<br>
  수기 발행은 `MANUALLY_ISSUED` 상태와 승인번호·발행일·공급가액·세액·합계·처리자 정보를 저장. `purchase_order_tax_invoice_event`는 수기 발행 이벤트를 append-only로 보존하고 승인번호 unique 제약으로 중복 등록을 방지.<br>
  판매자 오퍼가 연결된 주문의 공급자는 SELLER Organization 프로필이며 주문은 판매자별로 분리. 소유자 미지정 레거시 오퍼만 기존 환경 설정 공급자를 사용.<br>
  행의 존재가 발행 요청 여부이며, 일반 주문에는 세금계산서 전용 행이 없음.<br>
- `sales_offer.units_per_sale`은 판매 단위당 기준 SKU 수량이며 양수. RETAIL은 1, WHOLESALE은 박스 입수 수량으로 사용.<br>
- 판매자는 `SELLER` capability의 BUSINESS Organization과 완성·확인된 ACTIVE `organization_business_profile`이 있어야 판매 오퍼를 ON_SALE로 등록하고 주문을 받을 수 있음. 사업자 프로필은 법적 정보이고 카탈로그 소유 FK는 Organization을 직접 참조.<br>
- `inventory_movement`와 `stock_reservation`은 재고 원장과 같은 `organization_id + sku_id` 범위를 저장. 주문 예약·해제·확정·복구는 주문 항목의 판매 오퍼 Organization에 귀속.<br>
- `order_item.quantity`와 `unit_price`는 판매 단위 기준이며 `units_per_sale`은 주문 시점 snapshot. 재고 예약 수량은 두 수량의 곱.<br>
- 운영자·판매자·구매자의 현재 사업자 정보 원본은 `organization_business_profile`임. V38에서 레거시 `business_profile` 데이터를 Organization 프로필로 이관하고 V50에서 원본 테이블 제거.<br>
  V31부터 그룹 발행 프로필에 업태·종목·선택 이메일을 보관함. 주문은 발행 요청 당시 공급자·공급받는자 정보를 복사하며 기존 그룹 정보 변경의 영향을 받지 않음.<br>
- 세금계산서 품목은 주문 항목의 상품명·SKU 코드·수량·`line_amount`를 사용. 품목 공급가액과 합계는 주문 당시 확정 금액이며 별도로 재산출하지 않음.<br>
- 사업자등록 주소는 `organization_business_profile`의 사업자등록 프로필에 저장. 배송지는 `organization_address`에서 별도 관리하며 두 주소는 자동 동기화하지 않음.<br>
- 사용자가 사업자 그룹을 최초 생성할 때 국세청 사업자등록 상태조회에서 폐업 상태가 아님을 확인하고 `business_registration_verified_at`을 기록. 운영자가 초기 사업자 프로필을 입력한 그룹에는 해당 시각이 없으며 신규 생성 검증을 다시 요구하지 않음.<br>
- 주문 생성 시 발행 정보는 `WAITING_FOR_SHIPMENT` 상태로 snapshot. 배송 관리자의 최초 송장 등록 시 `ordered_at`의 KST 날짜를 작성일자와 제공일자로 함께 저장하고 `READY_FOR_ISSUANCE`로 전환.<br>

## 아직 없는 스키마

현재 migration에 PG 거래 상세, 부분 취소, SMS 본인 확인 challenge 테이블은 없음.<br>
V24의 `notification_outbox`는 알림 이벤트 기록이며 push 전달 이력이나 기기 token 테이블은 아님.<br>
`notification_device_token`은 token 원문과 전역 중복 식별용 SHA-256 hash를 저장하며 API 응답·로그에서 token 원문을 제외.<br>
V25의 lease는 worker 장애 후 만료된 처리 행을 다시 claim하기 위한 값이며, 외부 push provider 호출과 DB 변경을 같은 트랜잭션으로 묶지 않음.<br>
해당 기능이 확정되면 정책과 테이블을 설계하고 새 Flyway migration으로 추가함.<br>
설계안이나 Mermaid 관계도만으로 실제 테이블이 생성된 것으로 보지 않음.<br>

택배사 배송 현황 자동 연동, 미입금 만료, 부분 취소·환불 및 감사 로그 외 데이터 보존 기간은 미확정 정책임.<br>
확정 전에는 구체적인 테이블 계약을 현재 스키마로 문서화하지 않음.<br>
