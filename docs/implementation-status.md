# 구현 현황

이 문서는 현재 저장소의 구현 상태만 요약함.<br>
변경 이력이나 과거 작업 순서는 기록하지 않음.<br>
endpoint 계약은 각 [도메인 문서](services/README.md), 아키텍처 규칙은 [architecture.md](architecture.md)를 기준으로 함.<br>

## 기반

- Kotlin, Java 21, Spring Boot, Gradle
- MySQL read/write datasource, Flyway, Spring Data JPA
- 계층형 아키텍처: 도메인 코드를 `controller`·`service`·`model`·`integration` 패키지로 구성. Controller에서 구체 도메인 Service로 진입하고, 업무 Service에서 `JpaEntityService` 또는 외부 연동 구체 구현 호출. 서비스용 Port와 위임 wrapper 제거
- JWT RS256 인증, Access Token 및 영속 Refresh Token
- 공통 예외 응답, 요청 추적, Actuator, OpenAPI 설정
- Swagger controller tag·endpoint summary·request/response 모델 설명 및 필드별 형식·예시 metadata

## 구현된 기능

| 도메인 | 현황 |
| --- | --- |
| 인증 | 휴대폰 로그인, 사용자 웹 전용 비밀번호(Argon2), 웹 로그인, 상위 관리자 TOTP 등록·검증·운영자 초기화, MFA 미완료 운영 권한 차단, 실패 시도 제한, 1시간 Access Token, 1년 rolling Refresh Token 및 세션별 refresh/revoke. SMS 본인 확인·신규 계정 활성화 API는 미구현 |
| 공개 카탈로그 | 채널별 카테고리·상품 목록·상세 조회, Organization 소유 상품·SKU의 판매 오퍼 표시 |
| 관리자 계정 | 계정 생성/조회, 프로필·동의 관리, 서면 동의 처리자 추적, 동의 확인 후 승인·상태 변경, 상품·주문·재고·배송·영업 운영 role 부여·회수. 영업 그룹·인센티브 API는 미구현 |
| 카탈로그 | 운영자의 레거시 소유자 미지정 상품 관리, 판매자의 Organization 소유 브랜드·상품·SKU 및 채널 오퍼 관리 |
| 장바구니 | 조회, 오퍼 기준 상품 추가·수량 변경·삭제. 도매 수량은 박스 수 |
| 사용자 계정 | 프로필·그룹 공용 배송지, 그룹 onboarding, 전화번호 검색 초대·가입 요청, 대표자 승인 및 운영자 대표자 지정 |
| 주문·결제·배송 | 판매 Organization별 주문 분리와 판매자 사업자 프로필 기반 세금계산서 공급자 snapshot, 주문 생성·조회·전체 취소, 공휴일 캘린더, 평일 15시 자동 배송 준비·재고 확정, 준비 이후 취소 요청·운영자 승인/거절, 수동 계좌이체 입금·환불 상태와 이력, 그룹 공용 배송지 선택과 주문 배송지 snapshot, 송장 등록·고객 주문 목록 조회 전 대신/천일택배 HTML 및 경동택배 JSON 미완료 송장 조회와 자동 배송완료·공식 WebView 링크·운영자 수동 보정 |
| 재고 | 운영자 전체 및 판매 Organization별 SKU 조회·조정·변동 조회, 오퍼 소유 Organization 재고에 대한 주문 예약·해제·확정·복구. 박스 수량은 기준 SKU 단위로 환산 |
| 파일 | 운영자 권한별 상품 이미지·사업자 증빙 업로드, MIME·파일 크기·token 검증, DB metadata와 로컬 파일 원본 분리 저장, 상품 연결 이미지 공개 응답 및 사업자 증빙 소유 계정 범위 확인 |
| 화면 접근 권한 | ADMIN·BUYER audience별 화면 permission mapping, 운영 role과 활성 Organization 대표자·구성원 역할에 따른 access context 조회. 화면 metadata는 표시 편의용이며 업무 API는 개별 인가 수행 |
| 관리자 인가 | 계정·카탈로그·재고·배송 endpoint별 permission 검사, 제한된 영업 permission의 `SALES_MANAGER` role 부여·회수, 운영 변경 감사 로그 |
| 알림 | 주문·취소·입금·배송 상태 변경 이벤트의 DB outbox 기록, batch claim·lease·제한 재시도 worker, 활성 계정의 FCM·APNs 기기 token API, 주문자 활성 기기 fanout 및 FCM/APNs outbound adapter. delivery 이력과 운영자 실패 조회·재처리 API는 미구현 |

## 아직 구현되지 않은 범위

- SMS 휴대폰 본인 확인 및 비활성/신규 계정 활성화 흐름
- 부분 주문 취소, 배송 상태 이력
- 알림 delivery 이력과 운영자 실패 조회·재처리 API

미구현 endpoint나 흐름은 실제 구현처럼 문서화하지 않음.<br>
정책 결정이 필요한 항목은 해당 기능을 시작할 때 별도 설계함.<br>

## Flyway

현재 버전 마이그레이션은 V2부터 V46까지이며, 재실행 seed는 R 스크립트임.<br>
버전 파일은 적용 후 수정하지 않고 새 변경은 다음 버전 migration으로 추가함.<br>
정확한 버전별 테이블 목록은 [database.md](database.md)를 참고.<br>
