# 구현 현황

이 문서는 현재 저장소의 구현 상태만 요약함.<br>
변경 이력이나 과거 작업 순서는 기록하지 않음.<br>
endpoint 계약은 각 [도메인 문서](services/README.md), 아키텍처 규칙은 [architecture.md](architecture.md)를 기준으로 함.<br>

## 기반

- Kotlin, Java 21, Spring Boot, Gradle
- MySQL read/write datasource, Flyway, Spring Data JPA
- 헥사고날/클린 아키텍처의 application Port와 inbound/outbound adapter
- JWT RS256 인증, Access Token 및 영속 Refresh Token
- 공통 예외 응답, 요청 추적, Actuator, OpenAPI 설정

## 구현된 기능

| 도메인 | 현황 |
| --- | --- |
| 인증 | 휴대폰 로그인, 1시간 Access Token, 1년 rolling Refresh Token, 세션별 refresh/revoke, SMS 본인 확인과 계정 활성화 API는 미구현 |
| 공개 카탈로그 | 카테고리·상품 목록·상품 상세 조회 |
| 관리자 계정 | 계정 생성/조회, 프로필·동의 관리, 서면 동의 처리자 추적, 동의 확인 후 승인·상태 변경, 운영 role 부여·회수 |
| 관리자 카탈로그 | 카테고리·브랜드·상품 생성/수정, 이미지·옵션·SKU 관리 |
| 장바구니 | 조회, SKU 추가·수량 변경·삭제 |
| 사용자 계정 | 프로필 조회, 구매자 그룹 공용 배송지 CRUD·기본 배송지 관리 |
| 주문·결제·배송 | 주문 생성·조회·전체 취소, 공휴일 캘린더, 평일 15시 자동 배송 준비·재고 확정, 준비 이후 취소 요청·운영자 승인/거절, 수동 계좌이체 입금·환불 상태와 이력, 그룹 공용 배송지 선택과 주문 배송지 snapshot, 송장 등록·고객 주문 목록 조회 전 대신/천일택배 HTML 및 경동택배 JSON 미완료 송장 조회와 자동 배송완료·공식 WebView 링크·운영자 수동 보정 |
| 재고 | 운영 조회·조정·변동 조회, 주문 재고 예약·해제·확정 |
| 관리자 인가 | 계정·카탈로그·재고 endpoint별 permission 검사, role 관리, 운영 변경 감사 로그 |

## 아직 구현되지 않은 범위

- SMS 휴대폰 본인 확인 및 비활성/신규 계정 활성화 흐름
- 부분 주문 취소, 배송 상태 이력
- 파일 업로드 API, 알림 발송

미구현 endpoint나 흐름은 실제 구현처럼 문서화하지 않음.<br>
정책 결정이 필요한 항목은 해당 기능을 시작할 때 별도 설계함.<br>

## Flyway

현재 버전 마이그레이션은 V2부터 V20까지이며, 재실행 seed는 R 스크립트임.<br>
버전 파일은 적용 후 수정하지 않고 새 변경은 다음 버전 migration으로 추가함.<br>
정확한 버전별 테이블 목록은 [database.md](database.md)를 참고.<br>
