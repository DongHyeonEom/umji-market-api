# 계정

## 구현 상태

사용자 프로필은 인증 subject에서 확인한 활성 계정 기준으로 조회.<br>
배송지는 계정이 아니라 활성 구매자 그룹 소유. 그룹 구성원은 공용 배송지를 모두 조회·추가·수정·삭제·기본값 지정 가능.<br>
첫 공용 배송지는 자동 기본 배송지로 지정. 기본 배송지를 삭제하면 남은 주소 중 가장 오래된 주소를 기본값으로 승격.<br>
주문 생성 시 현재 그룹 공용 주소를 선택하고 수령 정보를 주문에 snapshot으로 저장. 이후 주소 변경·삭제는 기존 주문 정보에 영향을 주지 않음.<br>
기존 계정별 배송지는 V20에서 현재 계정의 구매자 그룹 공용 주소로 이관. 한 그룹에 여러 기본 배송지가 있으면 기존 기본 주소 중 하나만 승격.<br>
대표자는 전화번호 초대와 가입 요청 처리를 수행. 대표자 지정·변경은 운영자 권한으로 제한.<br>
최초 가입 계정은 그룹 onboarding 조회에서 현재 그룹과 전화번호가 일치하는 대기 초대를 확인하고, 초대 수락 후에만 그룹에 연결.<br>
그룹이 없는 계정은 개인 그룹을 만들거나 휴대폰 번호로 그룹을 찾아 가입 요청 가능. 그룹 이동 전 주문의 귀속은 유지.<br>
공급받는자 세금계산서 정보는 `buyer_group_business_profile`을 단일 원본으로 사용하며, 사업자등록번호·상호·성명·사업자주소·업태·종목은 필수. 이메일은 선택이며 그룹 구성원이 공유.<br>
활성 그룹 구성원은 세금계산서 정보를 조회할 수 있고, 대표자와 `ADMIN_ACCOUNT_MANAGE` 운영자만 수정 가능. 개인 그룹은 세금계산서 정보를 등록하거나 발행 요청할 수 없음.<br>
공급받는자 정보 완성 기준은 활성 `BUSINESS` 그룹과 사업자등록번호·상호·성명·사업자주소·업태·종목 입력. 이메일은 선택 항목.<br>
주문별 발행 선택과 계정별 기본 발행 선택은 기존 계약을 유지. 발행을 요청한 주문에는 주문 시점의 그룹 세금계산서 정보를 snapshot하고, 이후 프로필 변경은 기존 주문을 변경하지 않음.<br>

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
    ACTION -- 세금계산서 정보 조회 --> GROUP
    ACTION -- 세금계산서 정보 수정 --> GROUP
    GROUP --> SCOPE[그룹 소유 공용 배송지로 범위 제한]
    SCOPE --> VALID{요청·대상 배송지 유효}
    VALID -- 아니오 --> ERROR[400 또는 그룹 범위 404]
    VALID -- 예 --> LOCK[그룹 행 잠금]
    LOCK --> CHANGE{변경 유형}
    CHANGE -- 추가·수정 --> SAVE[주소 저장 및 기본값 단일화]
    CHANGE -- 기본값 지정 --> DEFAULT[그룹 주소 기본값 교체]
    CHANGE -- 삭제 --> DELETE[주소 삭제 및 필요 시 기본 주소 승격]
    CHANGE -- 세금계산서 조회 --> INVOICEVIEW[그룹 정보와 발행 가능 여부 반환]
    CHANGE -- 세금계산서 수정 --> INVOICEAUTH{대표자 또는 권한 운영자}
    INVOICEAUTH -- 아니오 --> DENY[403 거부]
    INVOICEAUTH -- 예 --> INVOICESAVE[사업자 그룹 정보 저장]
    SAVE --> RESPONSE[프로필 또는 그룹 배송지 응답]
    DEFAULT --> RESPONSE
    DELETE --> RESPONSE
    INVOICEVIEW --> RESPONSE
    INVOICESAVE --> RESPONSE
```

### Endpoint

- `GET /api/account/profile`
- `GET /api/account/addresses`
- `POST /api/account/addresses`
- `PUT /api/account/addresses/{addressId}`
- `PUT /api/account/addresses/{addressId}/default`
- `DELETE /api/account/addresses/{addressId}`
- `GET /api/account/groups/current/tax-invoice-profile`
- `PUT /api/account/groups/current/tax-invoice-profile` (활성 그룹 대표자 전용)
- `GET /api/account/groups/onboarding`
- `GET /api/account/groups/current`
- `POST /api/account/groups/individual`
- `GET /api/account/groups/search?phone=`
- `POST /api/account/groups/invitations`
- `GET /api/account/groups/invitations`
- `POST /api/account/groups/invitations/{invitationId}/response`
- `POST /api/account/groups/join-requests`
- `GET /api/account/groups/join-requests`
- `POST /api/account/groups/join-requests/{requestId}/response`
- `PUT /api/operation/buyer-groups/{groupId}/representative`

모든 endpoint는 Access Token의 subject가 가리키는 활성 계정을 사용. 계정 ID를 요청에서 받지 않음.<br>
배송지 목록·수정 범위는 인증 계정이 속한 활성 구매자 그룹으로 제한.<br>
주문 생성은 `shippingAddressId`를 필수 입력으로 받고 주문 요청의 구매자 그룹 배송지인지 확인.<br>
세금계산서 정보 조회·수정은 인증 계정의 현재 활성 그룹을 사용하며 그룹 ID를 사용자 요청에서 받지 않음. 조회는 활성 구성원, 수정은 대표자만 허용.<br>
운영자 수정 endpoint는 [operation.md](operation.md)의 `ADMIN_ACCOUNT_MANAGE` 권한을 요구.<br>

운영자 계정 생성·동의·프로필·승인 및 role 변경 흐름은 [operation.md](operation.md)를 기준으로 함.<br>

## 구매자 그룹과 계정 관계

구매자 그룹은 `BUSINESS` 또는 `INDIVIDUAL` 유형이며 사업자번호 보유 여부와 별개로 주문·공용 배송지의 공유 경계를 형성.<br>
그룹은 여러 계정을 구성원으로 가질 수 있고, 계정은 한 번에 하나의 활성 그룹에만 소속.<br>
도매·소매는 그룹 유형이 아니라 판매 채널 정책으로 판정. 사업자번호 없는 업체와 개인 대량구매자도 그룹을 이용 가능.<br>
운영자는 계정을 사업자 그룹에 명시적으로 연결하며 사업자번호만으로 그룹을 자동 병합하지 않음.<br>
구성원 초대·가입 요청·역할과 그룹 프로필 관리 흐름은 이 문서의 단일 기준. 주문 소유권, 실제 주문자와 주문 조회 범위는 [주문 문서](order.md)를 기준으로 함.<br>
현재 DB 테이블과 관계는 [database-erd.md](../database-erd.md)를 기준으로 함.<br>

## 대표자·일반구성원 화면 권한 설계

현재 대표자는 `buyer_group.representative_account_id`와 활성 `buyer_group_member` 소속으로 식별. 활성 구성원 중 해당 계정만 대표자이며 나머지는 일반구성원. 대표 여부를 계정 role에 복제하지 않아 대표자 변경 시 역할 데이터가 어긋나는 것을 방지.<br>

목표 설계에서는 `buyer_group_role_permission`이 `REPRESENTATIVE`·`MEMBER`별 permission을 설정하고, `ui_screen_permission`이 각 사용자 화면을 보기 위한 permission을 지정. 그룹 onboarding/current 조회가 현재 그룹 역할, 허용 screen code, capability를 반환해 React가 화면·버튼을 표시.<br>
그룹 미소속 계정은 `UNASSIGNED` 역할로 onboarding 화면만 접근. `REPRESENTATIVE`와 `MEMBER`는 그룹 주문·공용 주소·거래 이력을 공유하며, 초대·가입 요청 관리 화면과 action은 대표자만 허용.<br>

| 사용자 역할 | 노출 screen code 예시 | permission code 예시 |
| --- | --- | --- |
| `UNASSIGNED` | `BUYER_GROUP_ONBOARDING` | `BUYER_GROUP_ONBOARDING_READ`, `BUYER_GROUP_CREATE`, `BUYER_GROUP_SEARCH`, `BUYER_GROUP_JOIN_REQUEST_CREATE` |
| `REPRESENTATIVE` | 주문·공용 주소·구성원 관리 화면 | `BUYER_GROUP_ORDER_READ`, `BUYER_GROUP_ORDER_CREATE`, `BUYER_GROUP_ADDRESS_MANAGE`, `BUYER_GROUP_INVITE`, `BUYER_GROUP_JOIN_REQUEST_MANAGE` |
| `MEMBER` | 주문·공용 주소 화면 | `BUYER_GROUP_ORDER_READ`, `BUYER_GROUP_ORDER_CREATE`, `BUYER_GROUP_ADDRESS_MANAGE`, `BUYER_GROUP_SEARCH`, `BUYER_GROUP_JOIN_REQUEST_CREATE` |

가입 요청 승인·거절과 전화번호 초대 등 대표자 전용 API는 현재처럼 서버가 그룹 대표 계정을 다시 검증. 화면 목록이나 클라이언트가 전달한 role 값을 신뢰하지 않음.<br>
사용자가 그룹 소속을 바꾸거나 운영자가 대표자를 변경한 경우 다음 access context 조회부터 새 그룹·역할을 기준으로 권한을 계산. 기존 주문의 귀속 그룹과 주문자 정보는 변경하지 않음.<br>

화면 permission mapping table 및 API 계약은 [operation.md](operation.md)의 운영자·사용자 화면 권한 설계를 단일 기준으로 함.<br>

## 그룹 구성원 가입 흐름

구현 흐름.<br>
대표자는 일반 구성원 초대와 가입 요청 처리를 담당. 대표자 변경은 운영자만 수행.<br>
운영자는 사업자 프로필을 포함한 계정을 생성해 사업자 그룹을 초기화하고, 기존 계정 연결 기능으로 초기 구성원을 지정. 최초 계정은 초기 대표자로 연결되며 운영자가 대표자를 지정·변경 가능.<br>
그룹 검색은 입력한 휴대폰 번호와 정확히 일치하는 일반 계정 번호 또는 사업자 대표 계정 번호로 수행. 결과에는 그룹 식별에 필요한 이름·유형만 노출.<br>
일반 구성원은 검색 결과 그룹에 가입 요청 가능. 대표자가 승인하면 기존 개인 그룹 소속을 종료하고 대상 그룹에 이동. 기존 주문의 그룹 귀속은 변경하지 않음.<br>
대표자의 사전 등록 초대는 계정 onboarding 조회에서 확인. 여러 그룹의 미처리 초대가 있으면 사용자가 대상을 선택하고, 수락 전에 자동으로 그룹을 이동하지 않음.<br>
그룹이 없는 최초 계정은 개인 그룹 생성 또는 대표자 번호 검색 후 가입 요청을 선택. 개인 그룹 생성자는 최초 대표자로 지정.<br>

```mermaid
flowchart TD
    START[인증 계정의 그룹 상태 확인] --> EXISTS{활성 그룹이 있는가}
    EXISTS -- 예 --> INVITE[휴대폰 번호 대상 미처리 초대 확인]
    INVITE --> ONE{미처리 초대 존재}
    ONE -- 예 --> CHOOSE[초대별 대상 그룹·번호 확인 후 사용자 선택]
    CHOOSE --> ACCEPT{초대 수락}
    ACCEPT -- 예 --> MOVEINV[기존 소속 종료 후 초대 그룹 연결]
    ACCEPT -- 아니오 --> KEEPINV[기존 소속 유지]
    ONE -- 아니오 --> CHOOSEOPT[현재 그룹 유지 또는 그룹 변경 요청]
    EXISTS -- 아니오 --> OPTIONS{그룹 연결 방식}
    OPTIONS -- 개인 그룹 생성 --> CREATE[개인 그룹 생성 및 생성자를 대표자로 지정]
    OPTIONS -- 대표자 번호 검색 --> SEARCH[일반·사업자 대표 휴대폰 번호 정확히 검색]
    SEARCH --> REQUEST[대상 그룹 가입 요청]
    REQUEST --> REP{그룹 대표자 결정}
    REP -- 승인 --> MOVE[기존 소속 종료 후 대상 그룹 연결]
    REP -- 거절 --> KEEP[기존 소속 유지 또는 그룹 미지정 유지]
    OP[운영자 계정·사업자 프로필 등록 및 구성원 그룹 연결] --> INITIAL[최초 계정을 대표자로 한 그룹 초기화]
    ADMIN[운영자 대표자 변경] --> ROLE[대표자 계정 변경]
    REPCHG[대표자 변경] --> ROLE{운영자 권한인가}
    ROLE -- 예 --> CHANGE[활성 구성원 중 새 대표자 지정]
    ROLE -- 아니오 --> DENY[변경 거부]
```
