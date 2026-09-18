# API 서비스 문서

Controller와 Service는 아래 도메인 경계로 분리함. 각 문서는 API 책임, 주요 endpoint, 권한·상태 규칙을 기록함

| 서비스 | 문서 | 책임 |
| --- | --- | --- |
| 인증 | `authentication.md` | 휴대폰 인증, 동의·활성화, 세션·토큰 |
| 계정 | `account.md` | 회원·업체 프로필, 주소, 계정 상태 조회 |
| 카탈로그 | `catalog.md` | 카테고리, 브랜드, 상품, SKU 조회·관리 |
| 장바구니 | `cart.md` | 장바구니와 항목 |
| 주문 | `order.md` | 주문 생성, 상태, 취소·환불 요청 |
| 결제 | `payment.md` | 결제 요청·승인·콜백 |
| 재고 | `inventory.md` | 재고, 예약, 재고 이력 |
| 운영 | `operation.md` | 관리자 업무, 승인, 감사 |
| 알림 | `notification.md` | 알림 요청·발송 상태 |
| 파일 | `file.md` | 업로드 정책·파일 메타데이터 |
