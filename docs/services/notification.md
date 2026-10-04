# 알림

## 구현 상태

주문·취소·입금·배송 상태 변경을 업무 트랜잭션과 함께 기록하는 notification outbox 구현 완료.<br>
outbox batch claim·lease·worker 재시도 처리는 구현됨. 실제 발송 adapter가 아직 없어 scheduler는 비활성 상태.<br>
인증 활성 계정의 Android FCM·iOS APNs token 등록·갱신·해제 API와 소유 계정 검사는 구현됨.<br>
FCM/APNs outbound adapter와 활성 기기 fanout은 구현됨. delivery 이력 및 운영자 실패 조회·재처리 API는 미구현임.<br>
SMS 본인 확인 코드 발송도 미구현임.<br>

## 발송 정책

모바일 앱의 정보성 push 채널은 Android FCM, iOS APNs로 통일.<br>
Flutter 앱이 OS push token을 획득·갱신하고, 인증된 앱이 알림 API에 token 등록·해제를 요청.<br>
token API는 활성 계정 인증을 요구하며 token 원문은 응답이나 로그에 포함하지 않음. 전송 adapter가 사용할 token 원문과 전역 중복 검사용 SHA-256 hash를 보관.<br>
token은 `ANDROID_FCM` 또는 `IOS_APNS` platform과 연결. Android FCM token은 1~4096자, APNs device token은 64자리 16진수로 제한. 동일 token 재등록은 멱등 갱신이며 비활성 token을 다시 활성화.<br>
동일 token을 다른 활성 계정이 등록하면 연결 계정을 현재 인증 계정으로 옮겨 한 token에 한 계정만 연결. 계정은 공개 token ID로 자기 token만 해제 가능.<br>
DB에는 전송용 token 원문과 전역 중복 식별용 SHA-256 hash를 저장. API 응답에는 공개 ID·platform·등록 시각만 포함하고 token 원문은 로그에 남기지 않음.<br>
datasource-proxy가 SQL parameter를 기록하지 않도록 기기 token 테이블을 포함하는 query log를 제외.<br>
알림 기능은 통합 API 안에서 독립 notification application 및 adapter 경계로 구현하며, 초기에는 별도 배포 서비스로 분리하지 않음.<br>
초기에는 메시지 broker를 도입하지 않으며, 비동기 worker가 DB outbox를 직접 조회해 FCM/APNs outbound adapter를 호출.<br>
주문·결제·배송 API는 상태 변경과 outbox 기록까지만 담당하고, DB commit 후 worker 전송 완료를 기다리지 않고 응답.<br>
클라이언트에는 기기 token 등록·해제 API만 제공하고 임의 메시지 발송 API는 노출하지 않음.<br>
웹 브라우저 push는 현재 범위에 포함하지 않음.<br>
서비스 알림은 주문·입금·배송 진행에 필요한 정보성 알림으로 한정하며, 광고·쿠폰·재구매 권유는 발송하지 않음.<br>
마케팅 알림은 정보성 알림과 동의를 분리하고, 명시적인 선택 동의가 확인된 경우에만 별도 발송.<br>
OS push permission과 마케팅 수신 동의는 별개로 처리. 마케팅 push는 OS permission과 명시적 마케팅 동의가 모두 활성인 경우에만 발송.<br>
현재 계정 동의 이력은 개인정보 처리 동의 중심이며 알림 수신 동의 조회·철회 기능은 없음. 알림 구현 시 마케팅 동의의 획득·철회 이력을 별도 관리해야 함.<br>

| 업무 이벤트 | 정보성 알림 조건 |
| --- | --- |
| 주문 생성 | 주문 생성이 성공적으로 완료된 경우 활성 기기로 주문 접수 사실 발송 |
| 입금 상태 변경 | 입금 대기에서 부분 확인·전액 확인·일반 이슈 검토 필요로 전이되거나, 환불 대기·환불 완료로 전이된 경우 발송 |
| 배송 준비 | 주문 배송 상태가 `PREPARING`으로 전이된 경우 발송 |
| 송장 등록 | 배송 상태가 `IN_TRANSIT`으로 전이되고 택배사·송장번호가 저장된 경우 발송 |
| 배송 완료 | 배송 상태가 `DELIVERED`로 처음 전이된 경우 발송 |
| 취소 | 취소가 확정되어 주문이 취소 상태로 전이된 경우 결과 발송. 취소 요청 접수는 요청 정책이 확정된 뒤 별도 이벤트로 취급 |

중복 요청·재처리로 같은 주문 이벤트가 반복되어도 같은 알림을 중복 발송하지 않도록 이벤트별 멱등 key를 사용.<br>
잠금 화면에 표시될 수 있는 push에는 주문번호·금액·사업자번호·주소·입금 계좌·상세 주문 내역을 넣지 않고, 알림 종류와 안전한 앱 이동 정보만 포함.<br>
알림 대상은 주문을 생성한 계정에 연결된 활성 기기로 제한. 구매자 그룹의 다른 구성원에게 주문 알림을 확장하지 않음.<br>
알림 title/body는 이벤트별 일반 문구로 고정하고 주문 ID는 앱 내부 이동용 data field로만 전달.<br>
알림 전달 기록은 주문·입금·배송 상태 변경과 같은 DB 트랜잭션에서 outbox에 저장해 상태 변경 commit 후 발송되도록 연계.<br>
업무 API는 DB commit 후 알림 전송 완료를 기다리지 않고 응답. 배송 관리자는 배송 상태·송장 정보 저장이 끝나면 작업을 이어갈 수 있음.<br>
worker의 push 전송은 비동기이며, provider 전송 결과는 별도 알림 전달 상태로 추적. push provider의 접수 성공은 단말 표시·열람을 보장하지 않음.<br>
FCM/APNs 장애는 주문·입금·배송 상태 변경을 rollback하지 않으며, outbox worker가 실패 메시지를 재시도.<br>
일시 실패는 최초 시도 후 1분·5분·15분 간격으로 최대 3회 재시도. 최초 시도를 포함해 최대 4회 전송.<br>
네트워크·timeout·provider throttling·provider 5xx는 일시 실패로 분류. 요청 형식 오류와 유효하지 않은 token은 영구 실패로 분류하고 재시도하지 않음.<br>
유효하지 않은 token은 해당 기기 token을 비활성화. 기타 영구 실패와 재시도 소진은 `FAILED`로 남기고 provider·실패 분류·응답 코드·시각을 운영 확인에 필요한 범위로 기록.<br>
자동 재시도 소진 뒤 추가 발송은 자동 수행하지 않음. 운영자가 실패 원인을 확인하고 수정한 뒤 명시적 재처리 기능을 통해 재시도 상태로 되돌리는 복구 정책으로 처리.<br>
외부 provider 접수 후 결과 저장 전에 worker가 중단되면 중복 접수 가능성이 있는 at-least-once 전달로 취급. 업무 상태 변경은 알림 전송 실패나 재처리로 rollback하지 않음.<br>
FCM/APNs 자격 증명은 secret 설정으로 주입.<br>

## 예정 흐름

아래 흐름은 현재 구현과 미구현 범위를 함께 나타냄. delivery 이력 및 운영자 조회·재처리는 미구현 상태.<br>

```mermaid
flowchart TD
    subgraph TOKEN[기기 token 관리]
        ClientToken["인증된 활성 계정"] --> TokenRequest["platform·OS token 등록/갱신"]
        TokenRequest --> TokenAuth{"인증 subject의 활성 계정 확인"}
        TokenAuth -->|실패| TokenDeny["401 또는 403 응답"]
        TokenAuth -->|성공| TokenValidate{"platform·길이 검증"}
        TokenValidate -->|실패| TokenInvalid["요청 거부"]
        TokenValidate -->|통과| TokenHash["SHA-256 계산·token hash 기준 upsert"]
        TokenHash --> TokenOwner["token을 인증 계정에 연결·활성화"]
        TokenOwner --> TokenResult["공개 token ID·platform·등록 시각 반환"]
        ClientToken --> TokenDelete["공개 token ID로 해제 요청"]
        TokenDelete --> TokenOwnerCheck{"계정 소유 token인가?"}
        TokenOwnerCheck -->|아니오| TokenDeny
        TokenOwnerCheck -->|예| TokenInactive["token 비활성화"]
    end

    Event["주문·입금·배송 상태 변경"] --> Eligible{"정의된 알림 이벤트인가?"}
    Eligible -->|아니오·마케팅 이벤트| Ignore["현재 미지원 이벤트 생략"]
    Eligible -->|정보성 이벤트| Build["최소 개인정보로 메시지 구성"]
    Build --> Idempotency{"이벤트별 멱등 key가 이미 처리됐는가?"}
    Idempotency -->|예| Ignore
    Idempotency -->|아니오| Queue["상태 변경과 같은 트랜잭션에 outbox 저장"]
    Queue -->|commit 성공| Response["업무 API 응답 반환"]
    Queue --> Worker["DB outbox polling worker"]
    Worker --> Recipients["주문 생성 계정의 활성 token 조회"]
    Recipients --> Fanout["이벤트별 일반 문구 생성·기기별 fanout"]
    Fanout --> Provider{"기기 platform"}
    Provider -->|ANDROID_FCM| FCM["Firebase Admin SDK FCM adapter"]
    Provider -->|IOS_APNS| APNS["APNs HTTP/2 token-auth adapter"]
    FCM -->|성공 또는 실패| Result["전달 결과 기록"]
    APNS -->|성공 또는 실패| Result
    Result -->|성공| Sent["발송 완료 기록"]
    Result -->|일시 실패·재시도 잔여| Retry["1분·5분·15분 간격으로 예약"]
    Retry --> Worker
    Result -->|영구 실패| Failed["FAILED 기록·유효하지 않은 token 비활성화"]
    Result -->|재시도 소진| Failed
    Failed --> Inspect["운영자가 실패 원인 확인·수정"]
    Inspect --> Replay["명시적 재처리 기능으로 재시도"]
    Replay --> Worker
```

발송 조건·정보성/마케팅 동의·outbox·재시도 정책은 위 기준을 따름.<br>
기기 token API는 `POST /api/notifications/device-tokens`에서 등록·갱신, `DELETE /api/notifications/device-tokens/{tokenId}`에서 인증 계정 소유 token 비활성화 제공.<br>
FCM은 Firebase Admin SDK와 Application Default Credentials, APNs는 HTTP/2·TLS 1.2 이상과 ES256 token-based authentication 사용.<br>
FCM project/service account와 APNs team ID·key ID·private key·bundle ID·환경 endpoint는 secret 또는 환경 설정으로 주입.<br>
운영자 실패 조회·명시적 재처리 endpoint는 worker 구현 task의 범위에서 권한과 감사 이력을 확정.<br>
별도 배포 서비스 분리는 발송량·장애 격리 요구가 발생할 때 검토하며, outbox 경계는 추후 분리를 지원하도록 유지.<br>
RabbitMQ 등 broker 도입은 초기 범위에서 제외. worker 처리량이나 독립 확장 요구가 생기면 outbox relay가 broker에 발행하고 전송 worker가 소비하는 구조로 확장.<br>
