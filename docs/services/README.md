# 도메인 문서

각 문서는 구현된 endpoint, 동작, 알고리즘 흐름을 우선 기록함.
미구현 도메인은 계획된 endpoint를 구현 계약처럼 기재하지 않고 상태만 표시함.
서비스 구현·수정은 해당 문서의 Mermaid 흐름을 먼저 확정하고 흐름에 맞춰 진행함.
현재 구현 흐름도는 인증·카탈로그·장바구니·주문/결제·재고·운영 문서에 포함하며, 관리자 계정 흐름은 운영 문서에 통합함.
계정 사용자 API·파일·알림은 미구현이므로 현재 동작 흐름도는 없음.
구현 착수 전에 각 도메인 문서에 먼저 추가.

| 도메인 | 문서 | 구현 상태 |
| --- | --- | --- |
| 인증 | [authentication.md](authentication.md) | 휴대폰 로그인·토큰 |
| 계정 | [account.md](account.md) | 사용자 API 미구현 |
| 카탈로그 | [catalog.md](catalog.md) | 공개 조회·관리자 관리 |
| 장바구니 | [cart.md](cart.md) | 구현 |
| 주문 | [order.md](order.md) | 생성·조회 구현 |
| 재고 | [inventory.md](inventory.md) | 조정·예약 구현 |
| 운영 | [operation.md](operation.md) | 계정·카탈로그 운영 구현 |
| 결제 | [payment.md](payment.md) | 수동 계좌이체 입금 상태 확인 |
| 파일 | [file.md](file.md) | 미구현 |
| 알림 | [notification.md](notification.md) | 미구현 |
