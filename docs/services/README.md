# 서비스 문서

이 문서는 도메인별 상세 문서의 위치를 안내하는 목차.<br>
전체 제품 흐름은 작업 공간의 [SERVICE_FLOW.md](../../../SERVICE_FLOW.md)를 기준으로 함.<br>
각 도메인 문서는 현재 API 동작, 구현 흐름, 명시적 미구현 범위를 관리함.<br>

## 도메인 문서

| 도메인·업무 | 문서 | 주요 범위 |
| --- | --- | --- |
| 인증·세션 | [authentication.md](authentication.md) | 로그인, token, 계정 상태, 세션 |
| 계정·그룹 | [account.md](account.md) | 프로필, 구매자 그룹, onboarding, 공용 배송지 |
| 카탈로그·판매자 상품 | [catalog.md](catalog.md) | 상품 탐색, 판매 Organization 카탈로그 |
| 장바구니 | [cart.md](cart.md) | 상품·수량 조회 및 변경 |
| 재고 | [inventory.md](inventory.md) | 조정, 예약, 확정, 복구 |
| 주문·배송 | [order.md](order.md) | 주문, 취소, 출고, 송장, 배송 조회 |
| 결제 | [payment.md](payment.md) | 계좌 안내, 입금·환불 상태 |
| 운영자 관리 | [operation.md](operation.md) | 계정·권한·카탈로그·감사·영업 운영 |
| 화면 접근 권한 | [operation.md](operation.md) | audience·role·그룹 기반 화면 접근 context |
| 파일 | [file.md](file.md) | 상품 이미지·사업자 증빙 업로드와 접근 범위 |
| 알림 | [notification.md](notification.md) | outbox, 기기 token, FCM/APNs 전달 |

서비스 기능을 변경할 때 해당 도메인 문서의 실제 흐름과 수용 기준을 먼저 갱신.<br>
화면별 반응형 사용 흐름과 채널별 제품 정책은 `SERVICE_FLOW.md`에서 관리하며 이 문서에 복사하지 않음.<br>
