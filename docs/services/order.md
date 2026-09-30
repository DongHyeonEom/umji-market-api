# 주문

## 주문·입금·배송 정책 흐름

입금 확인과 물건 발송은 독립된 운영 작업.<br>
입금 상태 변경만으로 발송 상태가 바뀌지 않으며, 배송 준비 전환도 입금 확인을 선행 조건으로 요구하지 않음.<br>
아래 흐름은 확정된 정책과 이번 구현 범위.<br>
입금 확정은 결제 상태와 주문 결제 상태만 변경하며, 배송 상태는 별도 관리.<br>
재고는 주문 시 예약하고, 운영자가 등록한 휴무일과 주말을 제외한 평일 15:00(KST)에 배송 준비 상태로 전환하며 확정.<br>
`READY_TO_SHIP` 상태에서 사용자는 주문 전체를 즉시 취소 가능. `PREPARING` 이후 송장 등록 전에는 취소 요청을 생성하고 운영자가 배송 담당자 확인 후 승인 또는 거절.<br>
배송 준비 후 취소 승인 시 확정된 재고를 복구. 송장 등록으로 배송 중 상태가 된 주문은 취소 불가.<br>
배송 중 주문의 공식 배송 페이지를 1시간마다 조회하고, 고객의 배송 조회 요청 시에도 최신 상태를 확인.<br>
조회 결과는 동일한 멱등 상태 갱신 흐름으로 저장. 배송 완료 후에도 택배사 공식 조회 화면과 송장번호 제공.<br>
대신택배·천일택배는 공식 조회 페이지 HTML의 상태를 adapter로 추출. 공식 서버 API가 확인되기 전까지 HTML 변경·접근 차단을 고려해 미확인 응답에서는 배송 상태를 유지하고 운영자 수동 상태 보정 유지.<br>
경동택배는 조회 페이지 링크만 제공 중이며 서버 측 응답·상태 adapter는 미확정.<br>

```mermaid
flowchart TD
    subgraph CHECKOUT[사용자 주문·계좌 안내]
        A[인증된 사용자] --> B[계정 기본 세금계산서 설정 조회]
        B --> C[발행 여부 토글 및 주문별 선택]
        C --> D[선택한 발행 여부에 맞는 계좌 표시]
        D --> E{기본값 변경 확인}
        E -- 확인 --> F[주문과 함께 선택값·기본값 변경 요청]
        E -- 유지 --> G[주문과 함께 선택값 전달]
        F --> H[장바구니·SKU 판매 상태 검증]
        G --> H
        H --> I{주문 가능}
        I -- 아니오 --> J[주문 거부]
        I -- 예 --> K[주문·상품·가격·발행 여부·계좌 snapshot 저장]
        K --> L[결제 대기·배송 대기 상태 저장]
        L --> M[재고 예약 및 장바구니 비우기]
    end

    subgraph SHIPPING[배송 운영]
        M --> N[배송 상태 READY_TO_SHIP 및 재고 예약]
        N --> CUTOFF{주말·등록 휴무일이 아닌 평일 15시 도달}
        CUTOFF -- 아니오 --> N
        CUTOFF -- 예 --> CONFIRM[예약 재고 확정 및 PREPARING 전환]
        CONFIRM --> P[배송 담당자가 출고 준비]
        P --> Q[배송 담당자가 택배사·송장번호 입력]
        Q --> R{주문 및 배송 정보 유효}
        R -- 아니오 --> ERR[요청 거부]
        R -- 예 --> DUP{이미 IN_TRANSIT이며 같은 송장 정보}
        DUP -- 예 --> U[변경 없이 기존 배송 정보 반환]
        DUP -- 아니오 --> T[배송 정보 저장 및 IN_TRANSIT 반영]
        T --> U[사용자 주문 화면에 배송중·배송 조회 정보 제공]
        U --> TRACKSYNC
    end

    subgraph TRACKING_SYNC[배송 상태 자동 조회]
        TRACKSYNC{추적 조회 실행}
        TRACKSYNC -- 매시간 스케줄러 --> TRACKER[지원 택배사 HTML adapter]
        TRACKSYNC -- 고객 배송 조회 요청 --> TRACKER
        TRACKER --> CARRIER{택배사}
        CARRIER -- 대신택배 --> DAESIN[대신 공식 조회 페이지]
        CARRIER -- 천일택배 --> CHUNIL[천일 공식 조회 페이지]
        CARRIER -- 경동택배 미지원 --> UNAVAILABLE[수동 보정 가능]
        DAESIN --> PARSE[배송 상태 추출]
        CHUNIL --> PARSE
        UNAVAILABLE --> TRACKREVIEW
        PARSE --> TRACKRESULT{조회 결과 상태}
        TRACKRESULT -- 배송중 --> TRACKSAVE[최신 배송 상태 저장]
        TRACKRESULT -- 배송완료 --> DONE[DELIVERED 상태 저장]
        TRACKRESULT -- 연동 불가/조회 실패 --> TRACKREVIEW[상태 유지 및 오류 로그]
        TRACKSAVE --> U
        DONE --> D1[주문 조회에 배송완료·송장·공식 조회 링크 제공]
        U --> OPFIX[ORDER_WRITE 운영자가 배송완료 보정 가능]
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
배송 중 상태는 스케줄러의 주기 조회를 기본으로 하고 사용자 요청에 따른 즉시 조회도 허용. 두 경로는 동일한 상태 갱신 규칙을 사용.<br>
택배사 API 또는 중계 API의 조회 결과가 배송완료이면 `DELIVERED`로 전환. 중복 완료 결과는 멱등 처리하고, 연동 실패는 기존 상태를 유지.<br>
`ORDER_WRITE` 운영자는 배송 완료 상태를 수동 보정 가능. 배송 상태 전이 이력 및 외부 연동 실패 재처리 정책은 별도 범위.<br>
입금 만료는 두지 않으며, 미입금 주문도 운영자가 별도 입금 상태로 관리.<br>
주문 생성·결제 확인의 현재 구현과 target 변경사항은 각각 아래 동작 설명과 [payment.md](payment.md)를 기준으로 함.<br>

## Endpoint

- `POST /api/orders`
- `GET /api/orders?page=&size=`
- `GET /api/orders/{orderId}`
- `POST /api/orders/{orderId}/shipment/refresh`
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

현재 인증 계정의 장바구니를 기준으로 주문을 생성함.<br>
주문 항목에는 상품명, SKU명·코드, 가격, 수량을 스냅샷으로 저장함.<br>
같은 유스케이스 흐름에서 재고를 예약하고 장바구니 항목을 초기화.<br>
초기 상태는 `PENDING_PAYMENT`임.<br>
초기 결제수단은 `BANK_TRANSFER`, 결제 상태는 `WAITING_FOR_DEPOSIT`이며 사용자 주문 목록·상세에 결제 상태가 포함됨.<br>
배송 상태는 `READY_TO_SHIP`으로 초기화.<br>
계정별 세금계산서 발행 기본값은 초기 미발행이며, `GET /api/orders/checkout-options`에서 기본값과 발행·미발행 계좌 정보를 조회.<br>
주문 생성 요청은 `taxInvoiceRequested` 값을 받아 주문에 저장하며, `updateDefaultTaxInvoicePreference=true`가 함께 전달된 경우 선택값을 계정 기본값에도 반영.<br>
주문 상세에는 주문 당시 세금계산서 발행 선택과 안내 계좌 정보가 포함됨.<br>

주문 조회는 토큰 subject의 계정으로 제한하며 최신 취소 요청 상태도 포함함.<br>
주문 취소는 전체 주문 단위. `READY_TO_SHIP`에서 주문자는 즉시 취소 가능하고 예약 재고 해제. `PREPARING` 이후 송장 등록 전에는 취소 요청 상태(`PENDING`)를 주문 조회에 제공하며, 운영자 승인(`APPROVED`) 시 주문 취소·확정 재고 복구, 거절(`REJECTED`) 시 주문·재고 유지.<br>
운영자 공휴일 등록은 평일 15:00 배송 준비·재고 확정 배치에서 제외할 날짜를 관리.<br>
입금이 확인된 취소 주문의 실제 계좌 환불은 운영자가 수행하고 시스템에서 환불 상태를 직접 갱신.<br>
application UseCase가 장바구니·재고·주문 저장 Port를 조정함.<br>

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
`ORDER_WRITE` 운영자가 발송 처리를 시작하고, 배송 담당자가 택배사와 송장번호를 등록하면 `IN_TRANSIT`으로 변경.<br>
고객은 주문 목록·상세 조회에서 배송 상태와 송장 정보를 확인함.<br>
대신택배·경동택배·천일택배의 공식 조회 URL과 송장번호를 사용자 주문 응답에서 제공.<br>
확인한 조회 URL은 대신택배 `https://www.ds3211.co.kr/freight/internalFreightSearch.ht?billno=`, 경동택배 `https://kdexp.com/newDeliverySearch.kd?barcode=`, 천일택배 `https://www.chunil.co.kr/HTrace/HTrace.jsp?transNo=`.<br>
대신택배·천일택배는 공식 조회 페이지를 매시간 조회하고, 고객의 `POST /api/orders/{orderId}/shipment/refresh` 요청 시에도 즉시 확인.<br>
두 택배사 조회 페이지 HTML에서 배송 상태를 추출하는 outbound adapter 적용. 정식 서버 API 계약은 확인되지 않음. HTML 구조 변경 또는 자동화 접근 차단 시 배송 상태를 유지하고 운영자 보정 가능.<br>
경동택배 조회 링크는 제공하나 현재 실행 환경에서 조회 요청은 `403`을 반환해 상태 응답 확인 미완료. 실제 추적번호를 사용한 결과 확인 후 adapter 범위 확정.<br>
스케줄 주기는 `shipment.tracking.poll-cron`으로 바꾸며 기본은 매시간 정각(KST). 고객 요청 조회와 배치 조회는 같은 멱등 상태 갱신 유스케이스 사용.<br>
`POST /api/operation/orders/{orderId}/shipment/delivered`는 `ORDER_WRITE` 운영자의 수동 배송완료 보정 endpoint.<br>
운영 배송 API는 `ORDER_WRITE` 권한을 요구.<br>
