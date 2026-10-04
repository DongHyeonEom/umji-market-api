# 데이터베이스

## 기준과 출처

현재 스키마는 MySQL 8.0 이상과 Flyway V2–V25로 관리함.<br>
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
- JPA Entity와 Spring Data Repository는 persistence adapter 안에 배치.<br>
  애플리케이션 경계 밖으로 Entity를 전달하지 않음.<br>

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

시스템 role·permission seed는 `R__seed_system_roles_and_permissions.sql`에 있음.<br>

## 도메인 데이터 규칙

- `product`는 공통 상품 정보, `product_sku`는 가격·판매 상태를 소유함.<br>
  재고도 SKU 단위임.<br>
- SKU가 참조하는 옵션값은 같은 상품의 옵션에 속해야 함.<br>
- 장바구니는 현재 SKU를 참조하며 가격을 확정하지 않음.<br>
  주문 항목은 생성 당시 상품명·SKU명·코드·가격을 snapshot으로 보관함.<br>
- 주문 생성 시 재고를 예약하고, 평일 15:00 배송 준비 전환에서 확정함.<br>
  READY_TO_SHIP 취소는 예약을 해제하고, PREPARING 취소 승인 시 확정 재고를 복구함.<br>
  예약 식별자는 주문 항목과 재고 예약에 같은 UUID를 보관하며 두 행 사이 DB FK는 없음.<br>
- Refresh Token 원문은 저장하지 않고 hash를 저장함.<br>
  토큰 만료·폐기 의미는 인증 구현과 일치.<br>
- 업체 동의 이력은 기존 행을 덮어쓰지 않고 새 이력으로 추가함.<br>
- 구매 주문은 `buyer_group_id`로 그룹에 귀속하고, 기존 `purchase_order.account_id`는 실제 주문한 계정으로 유지함.<br>
  현재 각 기존 계정에 개인 또는 사업자 구매자 그룹 하나를 생성해 기존 주문·프로필을 backfill함.<br>
  V19에서 `buyer_group_id`를 필수화하며 신규 주문 생성 시 활성 계정의 그룹 ID를 저장해야 함.<br>
- `buyer_group.group_type`은 `BUSINESS` 또는 `INDIVIDUAL`이며, 사업자번호는 선택 정보임.<br>
  사업자 그룹 식별자나 그룹 병합 키로 사용하지 않음.<br>
- `buyer_group_member`는 구성원 소속 이력을 보존하며, 계정당 동시 활성 그룹 소속은 하나로 제한함.<br>
  V21의 generated `active_account_id` unique 제약으로 동시 활성 소속을 하나로 제한.<br>
  그룹 이동 시 이전 소속은 `LEFT`로 종료하고 새 활성 소속을 추가. 주문의 과거 그룹 귀속은 변경하지 않음.<br>
- `buyer_group.representative_account_id`는 현재 대표 계정이며 반드시 활성 구성원이어야 함.<br>
  대표자 지정·변경은 운영자 권한으로만 수행. 개인 그룹 최초 생성자는 대표자로 지정됨.<br>
- `buyer_group_invitation`은 대표자가 전화번호로 보낸 초대 이력이며, 초대 대상 계정이 수락해야 그룹 소속 변경.<br>
  초대 수락은 기존 그룹 대표자 계정에 대해 허용하지 않음.<br>
- `buyer_group_join_request`는 일반 구성원의 가입 요청 및 대표자의 처리 이력.<br>
  대표자만 그룹 가입 요청을 승인·거절할 수 있음.<br>
- `buyer_group_address`는 구매자 그룹 공용 배송지임.<br>
  그룹 구성원은 주소를 공동 조회·관리하고 기본 배송지는 그룹당 최대 하나로 유지함.<br>
  생성 계정은 이력 식별용이며 주소 접근 범위는 구매자 그룹 기준.<br>
- `purchase_order`의 배송지 snapshot은 주문 당시 수령인·연락처·주소를 보존함.<br>
  주소 원본과 외래 키를 두지 않아 그룹 주소 변경·삭제가 기존 주문에 영향을 주지 않음.<br>
- `business_profile`은 기존 계정 기능의 호환을 위해 유지하고, V18 시점의 사업자 프로필은 `buyer_group_business_profile`로 복사함.<br>

## 아직 없는 스키마

현재 migration에 PG 거래 상세, 부분 취소, 파일 메타데이터, push 기기 token, SMS 본인 확인 challenge 테이블은 없음.<br>
V24의 `notification_outbox`는 알림 이벤트 기록이며 push 전달 이력이나 기기 token 테이블은 아님.<br>
V25의 lease는 worker 장애 후 만료된 처리 행을 다시 claim하기 위한 값이며, 외부 push provider 호출과 DB 변경을 같은 트랜잭션으로 묶지 않음.<br>
해당 기능이 확정되면 정책과 테이블을 설계하고 새 Flyway migration으로 추가함.<br>
설계안이나 Mermaid 관계도만으로 실제 테이블이 생성된 것으로 보지 않음.<br>

택배사 배송 현황 자동 연동, 미입금 만료, 부분 취소·환불 및 감사 로그 외 데이터 보존 기간은 미확정 정책임.<br>
확정 전에는 구체적인 테이블 계약을 현재 스키마로 문서화하지 않음.<br>
