# 계정

## 구현 상태

사용자 프로필은 인증 subject에서 확인한 활성 계정 기준으로 조회.<br>
배송지는 계정이 아니라 활성 구매자 그룹 소유. 그룹 구성원은 공용 배송지를 모두 조회·추가·수정·삭제·기본값 지정 가능.<br>
첫 공용 배송지는 자동 기본 배송지로 지정. 기본 배송지를 삭제하면 남은 주소 중 가장 오래된 주소를 기본값으로 승격.<br>
주문 생성 시 현재 그룹 공용 주소를 선택하고 수령 정보를 주문에 snapshot으로 저장. 이후 주소 변경·삭제는 기존 주문 정보에 영향을 주지 않음.<br>
기존 계정별 배송지는 V20에서 현재 계정의 구매자 그룹 공용 주소로 이관. 한 그룹에 여러 기본 배송지가 있으면 기존 기본 주소 중 하나만 승격.<br>
구성원 초대·그룹별 권한 차등과 세금계산서 정보 공유 정책은 별도 미구현 범위.<br>

## 사용자 계정·공용 배송지 흐름

```mermaid
flowchart TD
    REQUEST[사용자 프로필 또는 배송지 요청] --> AUTH[Access Token 인증]
    AUTH --> SUBJECT[JWT subject에서 계정 공개 UUID 확인]
    SUBJECT --> ACCOUNT[계정 존재·ACTIVE 상태 검증]
    ACCOUNT --> ACTION{요청 종류}
    ACTION -- 프로필 조회 --> PROFILE[본인 계정 프로필 조회]
    ACTION -- 배송지 목록 --> GROUP[활성 구매자 그룹 확인]
    ACTION -- 배송지 변경 --> GROUP
    GROUP --> SCOPE[그룹 소유 공용 배송지로 범위 제한]
    SCOPE --> VALID{요청·대상 배송지 유효}
    VALID -- 아니오 --> ERROR[400 또는 그룹 범위 404]
    VALID -- 예 --> LOCK[그룹 행 잠금]
    LOCK --> CHANGE{변경 유형}
    CHANGE -- 추가·수정 --> SAVE[주소 저장 및 기본값 단일화]
    CHANGE -- 기본값 지정 --> DEFAULT[그룹 주소 기본값 교체]
    CHANGE -- 삭제 --> DELETE[주소 삭제 및 필요 시 기본 주소 승격]
    SAVE --> RESPONSE[프로필 또는 그룹 배송지 응답]
    DEFAULT --> RESPONSE
    DELETE --> RESPONSE
```

### Endpoint

- `GET /api/account/profile`
- `GET /api/account/addresses`
- `POST /api/account/addresses`
- `PUT /api/account/addresses/{addressId}`
- `PUT /api/account/addresses/{addressId}/default`
- `DELETE /api/account/addresses/{addressId}`

모든 endpoint는 Access Token의 subject가 가리키는 활성 계정을 사용. 계정 ID를 요청에서 받지 않음.<br>
배송지 목록·수정 범위는 인증 계정이 속한 활성 구매자 그룹으로 제한.<br>
주문 생성은 `shippingAddressId`를 필수 입력으로 받고 주문 요청의 구매자 그룹 배송지인지 확인.<br>

운영자 계정 생성·동의·프로필·승인 및 role 변경 흐름은 [operation.md](operation.md)를 기준으로 함.<br>

## 사업자와 구매자 계정 확장 정책

한 사업자에 여러 직원 계정을 연결하고 각 직원이 생성한 주문도 사업자 주문으로 귀속하는 정책.<br>
사람 계정과 구매자 그룹을 분리하고, 그룹 구성원 관계로 여러 계정을 한 구매 주체에 연결.<br>
사업자에 연결된 모든 구성원은 다른 구성원이 생성한 주문을 포함해 사업자의 전체 주문 내역을 조회 가능.<br>
주문에는 구매자 그룹과 실제 주문한 계정을 함께 기록해 주문 주체를 보존.<br>
소매는 1인 계정당 개인 구매자 그룹 하나를 기본으로 사용. 개인 구매자 그룹도 도매 채널을 이용할 수 있어 구매 유형과 채널은 별도 판정.<br>
V18–V19에서 구매자 그룹·구성원 관계와 계정당 단일 그룹 제약을 추가. 주문 저장·그룹 전체 주문 목록·상세 조회도 연동됨.<br>
운영자는 계정 관리 권한으로 계정을 활성 사업자 그룹에 명시적으로 연결. 사업자번호만으로 그룹을 자동 병합하지 않음.<br>
V18 기존 데이터는 계정별 그룹으로 시작하고, 연결 이전에 생성된 주문은 기존 그룹에 유지.<br>
구성원 초대, 그룹 프로필 별도 관리 및 세금계산서 정보 관리 권한은 미구현.<br>

```mermaid
flowchart TD
    CREATE[운영자 계정 생성] --> PROFILE{사업자 프로필 입력}
    PROFILE -- 있음 --> BUSINESS[BUSINESS 구매자 그룹 생성]
    PROFILE -- 없음 --> INDIVIDUAL[INDIVIDUAL 구매자 그룹 생성]
    BUSINESS --> MEMBER[계정당 단일 그룹 구성원 연결]
    INDIVIDUAL --> MEMBER
    ADMIN[ADMIN_ACCOUNT_MANAGE 계정-그룹 지정] --> GROUP{활성 BUSINESS 그룹인가}
    GROUP -- 아니오 --> REJECT[그룹 지정 거부]
    GROUP -- 예 --> MEMBER
    MEMBER --> ORDER[구성원 누구나 사업자 명의로 주문 생성]
    ORDER --> CONTEXT[구매자 그룹 ID와 주문자 계정 ID 함께 저장]
    MEMBER --> QUERY[사업자 주문 목록 요청]
    CONTEXT --> ALL[사업자에 연결된 모든 구성원에게 조회 허용]
    QUERY --> ALL
    RETAIL[개인 구매 사용자 계정] --> ONE[계정당 개인 구매자 그룹 하나]
    ONE --> CONTEXT
```

사업자 정보와 그룹 단위 배송·세금계산서 정보는 개인 연락처·인증 정보와 분리하는 방향.<br>
장바구니는 계정별로 유지하고 주문 생성 시 인증 계정이 속한 구매자 그룹으로 주문 귀속.<br>

## 구매자 그룹 DB 설계안

구매 주체는 법적 사업자번호가 아니라 주문·조회 권한의 경계인 `buyer_group`으로 모델링.<br>
한 구매자 그룹에 여러 계정이 소속되고 한 계정은 한 그룹에만 소속.<br>
사업자번호가 없는 업체는 `BUSINESS` 그룹과 선택적 사업자 프로필로 관리하고, 사업자번호는 nullable 속성으로 보관.<br>
개인 대량구매자는 `INDIVIDUAL` 그룹으로 관리. 도매·소매는 구매자 유형이 아니라 별도 판매 채널로 결정.<br>
소매 구매자는 개인 계정 하나에 개인 그룹 하나를 연결하고, 개인 도매 구매자는 같은 개인 그룹에서 도매 채널을 이용.<br>
구매자 그룹·구성원·사업자 프로필 테이블과 기존 주문의 그룹 귀속은 V18 migration에 반영됨. 그룹 초대·구성원 관리 및 그룹 단위 주문 조회 기능은 미구현.<br>

```mermaid
erDiagram
    ACCOUNT ||--o| BUYER_GROUP_MEMBER : joins
    BUYER_GROUP ||--o{ BUYER_GROUP_MEMBER : has
    BUYER_GROUP ||--o| BUYER_GROUP_BUSINESS_PROFILE : optionally_describes
    BUYER_GROUP ||--o{ PURCHASE_ORDER : owns
    ACCOUNT ||--o{ PURCHASE_ORDER : placed_by
    SALES_CHANNEL ||--o{ STOREFRONT_LISTING : organizes
    STOREFRONT_LISTING ||--o{ SALES_OFFER : publishes
    PRODUCT_SKU ||--o{ SALES_OFFER : offered_as
```

| 테이블 | 주요 데이터 | 기준 |
| --- | --- | --- |
| `buyer_group` | `id`, `public_id`, `group_type`, `display_name`, `status` | 주문 귀속·공유 조회 단위. `BUSINESS` 또는 `INDIVIDUAL` |
| `buyer_group_member` | `buyer_group_id`, `account_id`, `status`, `joined_at` | 개인 계정과 구매 그룹 연결. 사업자 그룹은 구성원 다수 허용 |
| `buyer_group_business_profile` | `buyer_group_id`, 업체명, 사업자번호 nullable, 프로필 상태, 대표자·연락처 | 사업자 추가 정보. 개인 그룹에는 행이 없어도 됨 |
| `purchase_order` | `buyer_group_id`, `placed_by_account_id` | 사업자/개인 그룹 소유권과 실제 주문자 구분 |
| `storefront_listing` | `sales_channel`, 채널 카테고리·전시 상태 | 도매/소매별 전시를 공용 상품과 분리 |
| `sales_offer` | `storefront_listing_id`, `product_sku_id`, 판매 단위·가격·상태 | 채널별 판매 조건을 구매자 유형 및 공용 SKU와 분리 |

사업자번호는 그룹 식별자나 필수 FK로 사용하지 않으며 unique 제약도 없음. 사업자번호 검증 상태는 현재 별도 저장하지 않음.<br>
구성원 주문 목록은 인증 계정의 유효한 그룹 소속을 확인한 뒤 `buyer_group_id`로 조회. 전체 전화번호는 공유하지 않고 주문자 계정의 이름과 마스킹된 휴대폰 끝자리만 표시.<br>
상품 채널 접근 정책은 사업자번호 보유 여부가 아닌 별도 판매 권한·오퍼 정책으로 판정. 구매자 그룹 유형과 판매 채널을 직접 연결하지 않음.<br>
기존 `business_profile.account_id` 일대일 구조는 그룹 프로필로 이전 필요. 기존 주문은 주문 계정의 그룹으로 backfill하되 검증되지 않은 사업자번호만으로 그룹 자동 병합 금지.<br>
