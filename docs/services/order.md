# 주문

## 주문·입금·배송 정책 흐름

입금 확인과 물건 발송은 독립된 운영 작업.<br>
주문은 활성 구매자 그룹 소속 계정만 생성 가능. 미소속 계정은 [계정 문서](account.md)의 개인·사업자 그룹 등록을 먼저 완료.<br>
입금 상태 변경만으로 발송 상태가 바뀌지 않으며, 배송 준비 전환도 입금 확인을 선행 조건으로 요구하지 않음.<br>
아래 흐름은 확정된 정책과 이번 구현 범위.<br>
입금 확정은 결제 상태와 주문 결제 상태만 변경하며, 배송 상태는 별도 관리.<br>
재고는 주문 시 예약하고, 운영자가 등록한 휴무일과 주말을 제외한 평일 15:00(KST)에 배송 준비 상태로 전환하며 확정.<br>
`READY_TO_SHIP` 상태에서 사용자는 주문 전체를 즉시 취소 가능. `PREPARING` 이후 송장 등록 전에는 취소 요청을 생성하고 운영자가 배송 담당자 확인 후 승인 또는 거절.<br>
배송 준비 후 취소 승인 시 확정된 재고를 복구. 송장 등록으로 배송 중 상태가 된 주문은 취소 불가.<br>
별도 배송 조회 중계 서버가 없는 현재 구조에서는 고객이 주문 목록을 조회할 때 본인 구매자 그룹의 배송완료 전 송장을 택배사 조회 페이지에서 확인.<br>
택배사 조회 결과를 배송 상태에 반영한 뒤 주문 목록을 반환. 고객이 송장 조회 링크를 누르면 택배사 공식 페이지를 WebView로 표시.<br>
매시간 주기 조회는 향후 배송 조회 중계 서버를 신설할 때 적용할 범위. 현재 API 서버에서는 스케줄 조회 미실행.<br>
조회 결과는 동일한 멱등 상태 갱신 흐름으로 저장. 배송 완료 후에도 택배사 공식 조회 화면과 송장번호 제공.<br>
대신택배·천일택배는 공식 조회 페이지 HTML에서 상태를 추출. 경동택배는 신규 공식 화면의 JSON 조회 경로에서 상태를 추출.<br>
택배사 응답 형식 변경·접근 차단·조회 실패·미등록 운송장은 배송 상태를 변경하지 않고 운영자 수동 보정 가능.<br>

```mermaid
flowchart TD
    subgraph CHECKOUT[사용자 주문·계좌 안내]
        C01[인증된 사용자] --> C01A{활성 구매자 그룹 소속인가}
        C01A -- 아니오 --> C01B[개인·사업자 그룹 등록 흐름으로 이동]
        C01A -- 예 --> C02[Organization 기본 발행 설정·사업자 세금계산서 정보 조회]
        C02 --> C03[그룹 공용 배송지 선택 및 발행 여부 토글]
        C03 --> C04{발행 요청 여부}
        C04 -- 아니오 --> C05[일반 계좌 안내]
        C04 -- 예 --> C06{사업자 그룹 정보 완성}
        C06 -- 아니오 --> C07[주문 전 발행 정보 입력 안내]
        C06 -- 예 --> C08[세금계산서 계좌 안내]
        C05 --> C09{기본값 변경 확인}
        C08 --> C09
        C09 -- 확인 --> C10[주문과 함께 배송지·선택값·기본값 변경 요청]
        C09 -- 유지 --> C11[주문과 함께 배송지·선택값 전달]
        C10 --> C12[서버에서 그룹 범위·프로필 완성 여부 재검사]
        C11 --> C12
        C12 --> C13{주문·판매 Organization 사업자 프로필·세금계산서 정보 유효}
        C13 -- 아니오 --> C14[주문 거부·장바구니 유지]
        C13 -- 예 --> UNITCHECK{판매 수량 × 입수 수량이 정수 범위 안인가}
        UNITCHECK -- 아니오 --> C14[요청 거부·장바구니 유지]
        UNITCHECK -- 예 --> SPLIT[장바구니 항목을 판매 Organization별로 분리]
        SPLIT --> C15[판매자별 주문에 구매자·주문자·배송지·가격·수량·결제 snapshot 저장]
        C15 --> C16{세금계산서 발행 요청}
        C16 -- 예 --> C17[purchase_order_tax_invoice에 판매자 공급자·구매자 snapshot 저장]
        C16 -- 아니오 --> C18[세금계산서 하위 행 생성 생략]
        C17 --> C19[결제 대기·배송 대기·송장 등록 대기 저장]
        C18 --> C19
        C19 --> C20[판매 수량 × snapshot 입수 수량으로 기준 SKU 재고 환산]
        C20 --> C21[해당 판매 Organization의 SKU 재고 예약]
        C21 --> C22[모든 주문 저장 후 장바구니 비우기 및 주문별 이벤트 outbox 기록]
    end

    subgraph SHIPPING[배송 운영]
        M --> N[배송 상태 READY_TO_SHIP 및 재고 예약]
        N --> CUTOFF{주말·등록 휴무일이 아닌 평일 15시 도달}
        CUTOFF -- 아니오 --> N
        CUTOFF -- 예 --> CONFIRM[예약 재고 확정 및 PREPARING 전환]
        CONFIRM --> P[배송 담당자가 출고 준비]
        P --> Q[배송 담당자가 택배사·송장번호 입력]
        Q --> AUTHSHIP[SHIPMENT_WRITE 권한 검사]
        AUTHSHIP --> R{주문 및 배송 정보 유효}
        R -- 아니오 --> ERR[요청 거부]
        R -- 예 --> DUP{이미 IN_TRANSIT이며 같은 송장 정보}
        DUP -- 예 --> U[변경 없이 기존 배송 정보 반환]
        DUP -- 아니오 --> T[배송 정보 저장 및 IN_TRANSIT 반영]
        T --> TAXINVOICE{세금계산서 발행 요청 주문}
        TAXINVOICE -- 예 --> TAXREADY[purchase_order_tax_invoice의 발행 상태·작성·제공일자를 주문일자로 갱신]
        TAXINVOICE -- 아니오 --> U[사용자 주문 화면에 배송중·배송 조회 정보 제공]
        TAXREADY --> U
        U --> LIST[고객이 배송 목록 조회]
    end

    subgraph TRACKING_SYNC[배송 목록 응답 전 상태 확인]
        LIST --> TRACKSYNC[고객 그룹의 미완료 송장 목록 조회]
        TRACKSYNC --> TRACKER[택배사별 배송 조회 integration]
        TRACKER --> CARRIER{택배사}
        CARRIER -- 대신택배 --> DAESIN[대신 공식 조회 페이지]
        CARRIER -- 천일택배 --> CHUNIL[천일 공식 조회 페이지]
        CARRIER -- 경동택배 --> KDEXP[경동 공식 JSON 조회 경로]
        DAESIN --> PARSE[배송 상태 추출]
        CHUNIL --> PARSE
        KDEXP --> PARSE
        PARSE --> TRACKRESULT{조회 결과 상태}
        TRACKRESULT -- 배송중 --> TRACKSAVE[최신 배송 상태 저장]
        TRACKRESULT -- 배송완료 --> DONE[DELIVERED 상태 저장]
        TRACKRESULT -- 연동 불가/조회 실패 --> TRACKREVIEW[상태 유지 및 오류 로그]
        TRACKSAVE --> D1[최신 배송 상태·송장·공식 조회 링크를 목록에 제공]
        DONE --> D1
        TRACKREVIEW --> D1
        D1 --> WEBVIEW[고객이 링크 선택 시 공식 조회 페이지를 WebView로 표시]
        U --> OPFIX[SHIPMENT_WRITE 배송 관리자가 배송완료 보정 가능]
        OPFIX --> DONE
    end

    subgraph CANCELLATION[취소 및 환불]
        N --> C1{사용자 취소}
        C1 -- 평일 15시 전 또는 배송 준비 전 --> C2[전체 주문 즉시 취소 및 예약 재고 해제]
        C1 -- PREPARING, 송장 등록 전 --> C3[취소 요청 생성]
        C3 --> C4[ORDER_WRITE 운영자가 배송 담당자 확인]
        C4 --> C5{운영자 결정}
        C5 -- 승인 --> C6[주문 취소 및 확정 재고 복구]
        C5 -- 거절 --> C7[취소 요청 거절 이력 저장]
        T --> C8[배송 중·배송 완료 주문 취소 불가]
        C2 --> C9{입금 확인 여부}
        C6 --> C9
        C9 -- 미입금 --> C10[환불 불필요]
        C9 -- 입금 일부 또는 전액 확인 --> C11[결제 상태 REFUND_PENDING]
        C11 --> C12[관리자가 실제 계좌 환불]
        C12 --> C13[관리자가 REFUNDED 상태로 변경]
    end

    subgraph HOLIDAYS[공휴일 관리]
        H1[ORDER_WRITE 운영자 공휴일 등록/삭제] --> H2[(공휴일 날짜 저장)]
        H2 --> CUTOFF
    end

    subgraph PAYMENT[입금 확인]
        M --> PAY1[사용자가 주문에 저장된 계좌로 송금]
        PAY1 --> PAY2[운영자가 실제 입금 내역 대조]
        PAY2 --> PAY3{입금 결과}
        PAY3 -- 전액 확인 --> PAY4[결제 상태 PAYMENT_CONFIRMED]
        PAY3 -- 부분 확인 --> PAY5[결제 상태 PARTIAL_PAYMENT_REVIEW_REQUIRED]
        PAY3 -- 일반 이슈 --> PAY6[운영자가 PAYMENT_ISSUE_REVIEW_REQUIRED로 변경]
        PAY4 --> PAY7[사용자 주문 화면에 입금 확인 표시]
        PAY5 --> PAY8[사용자 주문 화면에 부분 입금 검토 표시]
        PAY6 --> PAY9[사용자 주문 화면에 일반 입금 이슈 표시]
        PAY9 --> PAY10[운영자가 시스템 밖에서 확인 및 후속 처리]
        PAY10 --> PAY11{운영자 확인 결과}
        PAY11 -- 입금 대기 유지 --> PAY12[WAITING_FOR_DEPOSIT로 변경]
        PAY11 -- 부분 입금 확인 --> PAY13[PARTIAL_PAYMENT_REVIEW_REQUIRED로 변경]
        PAY11 -- 전액 입금 확인 --> PAY14[PAYMENT_CONFIRMED로 변경]
        PAY12 --> PAY15[사용자 입금 대기 상태 갱신]
        PAY13 --> PAY16[사용자 부분 입금 상태 갱신]
        PAY14 --> PAY17[사용자 입금 완료 상태 갱신]
    end
```

입금 및 배송 상태는 각 운영 작업에서 별도로 갱신.<br>
주말과 운영자 등록 휴무일을 제외한 평일 15:00(KST)에 `READY_TO_SHIP` 주문을 `PREPARING`으로 변경하고 재고 예약을 확정.<br>
송장 등록 시 배송 정보와 `IN_TRANSIT`을 저장. 같은 송장 정보의 중복 등록은 상태 변경 없이 기존 배송 정보를 반환.<br>
배송 처리에는 입금 확인을 요구하지 않음.<br>
대신택배·경동택배·천일택배를 우선 지원 대상으로 하며, 택배사 코드별 공식 배송 조회 화면을 주문 응답의 WebView 링크로 제공.<br>
별도 배송 조회 중계 서버가 없는 현재 구조에서는 `GET /api/orders`가 고객의 `IN_TRANSIT` 송장을 먼저 조회하고, 상태 반영 후 주문 목록 반환.<br>
고객이 주문 목록의 송장 조회 링크를 선택하면 택배사 공식 조회 페이지를 WebView로 표시. 상태 조회는 목록 진입 시 서버가 수행하고, 링크는 택배사 상세 조회 화면 제공에 사용.<br>
택배사 조회 결과가 배송완료이면 `DELIVERED`로 전환. 중복 완료 결과는 멱등 처리하고, 연동 실패는 기존 상태를 유지.<br>
`SHIPMENT_WRITE` 권한 운영자는 배송 완료 상태를 수동 보정 가능. 결제·취소·휴무일 관리는 기존 `ORDER_WRITE` 권한을 유지. 배송 상태 전이 이력 및 외부 연동 실패 재처리 정책은 별도 범위.<br>
입금 만료는 두지 않으며, 미입금 주문도 운영자가 별도 입금 상태로 관리.<br>
주문 생성·결제 확인의 현재 구현과 target 변경사항은 각각 아래 동작 설명과 [payment.md](payment.md)를 기준으로 함.<br>
주문에는 요청 계정과 구매자 그룹이 함께 기록되므로 영업 인센티브는 그룹·주문 시점 담당 배정과 선택 요율을 기준으로 제안. 요율 미설정 주문은 미지급 상태로 snapshot하고, 설정 주문은 상품 판매액을 기준으로 계산하는 방향. 배송비·세금 포함 여부와 인센티브 확정 시점은 미확정이며 세부 설계는 [operation.md](operation.md)를 기준으로 함.<br>

## Endpoint

- `POST /api/orders` (응답의 `orders` 배열에 판매 Organization별 주문 포함)
- `GET /api/orders?page=&size=`
- `GET /api/orders/{orderId}`
- `PUT /api/operation/orders/{orderId}/shipment/tracking`
- `POST /api/operation/orders/{orderId}/shipment/delivered`
- `POST /api/orders/{orderId}/cancellation`
- `GET /api/operation/order-cancellations?page=&size=`
- `PATCH /api/operation/order-cancellations/{orderId}`
- `GET /api/operation/shipping-holidays`
- `POST /api/operation/shipping-holidays`
- `DELETE /api/operation/shipping-holidays/{date}`

## 주문 조회 흐름

```mermaid
flowchart TD
    A[주문 목록 또는 상세 요청] --> B[현재 인증 계정 식별]
    B --> C{요청 종류}
    C -- 목록 --> D[현재 계정 기준 페이지 조회]
    D --> E[목록 또는 빈 페이지 응답]
    C -- 상세 --> F[현재 계정과 주문 ID로 상세 조회]
    F --> G{소유 계정의 주문 존재}
    G -- 아니오 --> H[미발견 오류]
    G -- 예 --> I[주문·항목·결제·계좌 snapshot 응답]
```

## 주문 생성

현재 인증 계정의 장바구니와 활성 구매자 그룹 공용 배송지를 기준으로 주문을 생성함.<br>
요청에는 그룹에 속한 배송지 공개 UUID가 필수. 다른 그룹 배송지이거나 삭제된 배송지는 주문을 거부.<br>
수령인명·연락처·우편번호·기본 주소·상세 주소를 주문 시점 snapshot으로 저장. 이후 그룹 배송지의 수정·삭제와 무관하게 주문에 남음.<br>
그룹 주문을 공유하는 구성원은 해당 주문의 배송지 snapshot을 조회할 수 있음.<br>
`POST /api/orders` request의 `shippingAddressId`는 필수 UUID 필드.<br>
주문 항목에는 상품명, SKU명·코드, 판매가, 판매 수량, 판매 단위 입수 수량을 snapshot으로 저장함. WHOLESALE 수량은 박스 수이며 재고 예약 수량은 박스 수와 snapshot 입수 수량의 곱.<br>
장바구니 상품을 판매 Organization별로 묶어 주문을 분리 생성. 한 주문은 하나의 판매 Organization 상품만 포함.<br>
각 주문 생성 시 해당 판매자의 SKU 재고를 예약하고 모든 주문 저장이 성공한 뒤 장바구니 항목을 초기화.<br>
판매자별 주문 저장·재고 예약·장바구니 초기화·주문 생성 outbox 기록은 하나의 주문 생성 트랜잭션에 포함. 어느 판매자의 재고 예약이나 outbox 기록이 실패하면 전체 주문 생성이 롤백되어 장바구니와 기존 재고 상태가 유지.<br>
`POST /api/orders`는 판매자별 주문 응답을 `orders` 배열에 담아 반환하며, 각 주문 응답의 `sellerOrganizationId`로 판매자를 식별.<br>
초기 상태는 `PENDING_PAYMENT`임.<br>
초기 결제수단은 `BANK_TRANSFER`, 결제 상태는 `WAITING_FOR_DEPOSIT`이며 사용자 주문 목록·상세에 결제 상태가 포함됨.<br>
배송 상태는 `READY_TO_SHIP`으로 초기화.<br>
구매 Organization별 세금계산서 발행 기본값은 초기 미발행이며, `GET /api/orders/checkout-options`에서 기본값과 발행·미발행 계좌 정보를 조회. `taxInvoiceAvailable`은 장바구니의 모든 판매자 공급자 정보와 구매자 정보를 기준으로 계산.<br>
주문 생성 요청은 `taxInvoiceRequested` 값을 주문에 저장하며, 대표자가 `updateDefaultTaxInvoicePreference=true`를 전달하면 선택값을 Organization 기본값에 반영. Organization 구성원은 같은 기본값을 공유.<br>
판매자 오퍼가 연결된 주문의 공급자 정보(사업자등록번호·상호·성명·사업장주소·업태·종목·이메일)는 해당 SELLER Organization의 확인된 `organization_business_profile`에서 사용. 개인 판매 Organization이나 필수 정보가 미완성인 판매자는 세금계산서 발행 대상이 될 수 없음.<br>
V42 이전 판매자 미지정 레거시 오퍼 주문은 기존 환경 설정의 공급자 정보를 사용. 공급받는자 필수 정보(사업자등록번호·상호·성명·사업자주소·업태·종목)는 활성 `BUSINESS` 구매 Organization에서 관리하며 이메일은 선택.<br>
발행 요청은 각 판매자 공급자 정보와 인증 계정의 활성 `BUSINESS` 구매 Organization 공급받는자 필수 정보가 모두 완성된 경우에만 허용.<br>
발행 요청 주문에는 공급자·공급받는자·주문 항목 및 항목별 `lineAmount`를 snapshot. 송장 등록으로 배송이 시작되면 작성일자와 제공일자를 모두 `ordered_at`의 KST 날짜로 기록하고 발행 대기 정보를 노출.<br>
세금계산서 발행을 요청한 주문만 `purchase_order_tax_invoice`에 행을 생성해 상태와 공급자·공급받는자 정보를 주문 시점 snapshot으로 보관. 일반 주문은 해당 행을 생성하지 않으며 API의 `taxInvoiceRequested`는 하위 행 존재 여부로 반환.<br>
세금계산서 snapshot은 사용자 그룹 주문 상세와 권한 있는 운영 조회에서 확인하며, 실제 전자세금계산서 전송은 외부 발행 연동 도입 범위.<br>
그룹 정보가 완성되지 않은 상태에서 발행을 요청하면 주문 생성과 기본 설정 변경을 모두 거부하고 장바구니를 유지.<br>

## 사업자 주문 귀속·조회 정책

인증 계정의 활성 구매자 그룹이 주문 소유자이며, 요청을 실행한 계정은 실제 주문자로 별도 기록.<br>
활성 그룹 구성원은 그룹의 주문 목록·상세를 조회 가능. 주문 응답의 실제 주문자 이름은 첫 글자만 노출하고 나머지는 `*`로 치환하며, 휴대폰 번호는 끝 네 자리만 노출.<br>
그룹 구성, 유형, 계정 소속 및 구성원 관리 정책은 [계정 문서](account.md)를 기준으로 함.<br>

```mermaid
flowchart TD
    USER[인증된 사용자] --> GROUP[인증 계정의 구매자 그룹 조회]
    GROUP --> MEMBER{활성 그룹 구성원인가}
    MEMBER -- 아니오 --> DENY[그룹 주문 작업 거부]
    MEMBER -- 예 --> ACTION{작업 종류}
    ACTION -- 주문 생성 --> CART[인증 계정 장바구니 검증]
    CART --> ORDER[구매자 그룹 ID와 주문자 계정 ID로 주문 생성]
    ACTION -- 주문 목록·상세 조회 --> SCOPE[활성 구성원 그룹의 주문 범위 확인]
    SCOPE --> ALL[그룹 전체 주문과 주문자 표시 정보 반환]
    ALL --> MASK[주문자 이름 및 휴대폰 끝자리 마스킹]
    ACTION -- 취소·변경 --> MUTATE[별도 주문 변경 권한·상태 정책 검사]
    GROUPPOLICY[계정 문서의 그룹 소속 정책] --> GROUP
```

주문 소유 범위는 인증 계정의 현재 활성 그룹으로 판정. 주문 생성 계정 ID는 문의·감사 이력으로 보존.<br>
그룹 주문 조회 권한은 취소·수정 권한을 자동 부여하지 않으며, 변경 작업은 별도 권한·상태 검증을 유지.<br>
주문 취소는 전체 주문 단위. `READY_TO_SHIP`에서 주문자는 즉시 취소 가능하고 예약 재고 해제. `PREPARING` 이후 송장 등록 전에는 취소 요청 상태(`PENDING`)를 주문 조회에 제공하며, 운영자 승인(`APPROVED`) 시 주문 취소·확정 재고 복구, 거절(`REJECTED`) 시 주문·재고 유지.<br>
운영자 공휴일 등록은 평일 15:00 배송 준비·재고 확정 배치에서 제외할 날짜를 관리.<br>
입금이 확인된 취소 주문의 실제 계좌 환불은 운영자가 수행하고 시스템에서 환불 상태를 직접 갱신.<br>
`OrderService`가 장바구니·재고·주문 저장 기능을 조정하고 트랜잭션을 관리하며 주문 snapshot 저장·조회는 `OrderCheckoutJpaEntityService`에서 처리함.<br>
취소 정책은 `OrderCancellationService → OrderCancellationJpaEntityService`, 공휴일 관리는 `ShippingHolidayService → ShippingHolidayJpaEntityService`, 배송 상태 변경은 `ShipmentService → OrderShipmentJpaEntityService`에서 처리함.<br>
운영 결제 상태 변경은 `PaymentService → OrderPaymentJpaEntityService` 흐름으로 처리.<br>

## 입금 확인 운영 흐름

고객에게 세금계산서 발행 여부에 맞는 환경설정 계좌를 안내하고 계좌이체를 받는 방식을 사용함.<br>
가상계좌 발급이나 고객 계좌 연동을 전제로 하지 않음.<br>
운영자가 주문을 실제 계좌 입금 내역과 대조한 뒤 수동으로 입금 확인 처리.<br>
부분 입금으로 표시하면 사용자 주문 조회에 `PARTIAL_PAYMENT_REVIEW_REQUIRED` 상태를 보여주고, 운영자가 전화로 후속 처리.<br>
전액 확인 완료 시 주문 상태를 `PAID`로 변경.<br>
재고 예약은 평일 15:00 배송 준비 전환 시 확정.<br>
계좌 내역 자동 조회·자동 매칭은 현재 범위에 포함하지 않음.<br>

운영자 입금 목록·상태 변경 API와 상태 이력은 구현됨.<br>

## 배송 및 송장 조회 흐름

배송 상태는 `READY_TO_SHIP`, `PREPARING`, `IN_TRANSIT`, `DELIVERED`로 결제 상태와 분리해 관리함.<br>
평일 15시 배송 준비 배치가 재고를 확정하고 `PREPARING`으로 변경. 배송 관리자가 택배사와 송장번호를 등록하면 `IN_TRANSIT`으로 변경.<br>
고객은 주문 목록·상세 조회에서 배송 상태와 송장 정보를 확인함.<br>
대신택배·경동택배·천일택배의 공식 조회 URL과 송장번호를 사용자 주문 응답에서 제공.<br>
확인한 조회 URL은 대신택배 `https://www.ds3211.co.kr/freight/internalFreightSearch.ht?billno=`, 경동택배 `https://kdexp.com/service/delivery/etc/delivery.do?barcode=`, 천일택배 `https://www.chunil.co.kr/HTrace/HTrace.jsp?transNo=`.<br>
세 택배사는 고객이 주문 목록을 조회할 때 본인 구매자 그룹 주문 중 배송완료 전 송장에 대해서만 상태를 확인.<br>
대신택배·천일택배는 공식 조회 HTML에서, 경동택배는 신규 공식 화면의 JSON 조회 경로에서 배송 상태를 추출. 응답 형식 변경·접근 차단·조회 실패 시 상태를 유지하고 운영자 보정 가능.<br>
경동택배 서버 상태 조회는 신규 공식 화면의 JSON 조회 경로 `/service/delivery/new/ajax_basic.do?barcode={운송장번호}`를 사용. `result=suc` 응답의 `data.scanList`는 오래된 이력부터 최신 이력 순이며 마지막 `scanTypeNm`이 정확히 `배송완료`이면 완료로 전환하고, 진행 이력이 있으면 배송중으로 유지.<br>
임의 번호와 다른 택배사 예시 번호는 경동택배 경로에서 `result=fail`을 반환하므로 유효 송장 실조회 결과로 간주하지 않음. 조회 불가·실패는 기존 배송 상태 유지.<br>
목록 조회는 택배사별 상태를 먼저 갱신한 뒤 최신 상태와 공식 조회 링크를 반환. 고객이 링크를 선택하면 WebView에서 택배사 공식 조회 화면 표시.<br>
`PUT /api/operation/orders/{orderId}/shipment/tracking` 및 `POST /api/operation/orders/{orderId}/shipment/delivered`는 `SHIPMENT_WRITE` 권한 운영자만 호출 가능.<br>
입금·취소·휴무일 운영 API는 `ORDER_WRITE` 권한을 요구. 송장·배송 상태 변경은 `SHIPMENT_WRITE` 권한을 요구.<br>
