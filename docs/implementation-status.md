# 구현 상태와 다음 순서

## 현재 기반

- Java 21, Kotlin, Spring Boot, Gradle Kotlin DSL 기반
- MySQL 8.0+, read/write datasource, Flyway 사용
- `com.buyeong.umji.api` 패키지 기준으로 스켈레톤 예제 코드·테스트 제거 완료
- Actuator 상태 확인: `GET /actuator/health`
- 공통 예외 처리, 추적 로그, OpenAPI, Testcontainers 기반 유지
- JWT RSA PEM은 프로젝트 외부 `E:\buyeong_dev\umji-market\secrets\jwt`에서 경로로 주입
- 인증 세부 정책 확정 전 로컬 환경은 `BYPASS` 모드 사용. 운영 환경은 `REQUIRED` 유지
- `V3__create_catalog_tables.sql`과 공개 카탈로그 조회 API 기반 추가
- `V4__add_operation_account_profile_tables.sql`과 운영 계정·업체 프로필·동의 이력 기반 추가
- `V5__add_product_images_and_options.sql`과 운영자 상품 수정, 이미지·옵션·SKU 관리 API 추가
- `V6__add_phone_login_and_persistent_sessions.sql`과 `ACTIVE` 계정의 휴대폰 번호 로그인, RS256 Access Token·영속 Refresh Token 갱신·폐기 API 추가

## 마이그레이션 주의사항

스켈레톤 예제용 `V1__create_skeleton_example_tables.sql`은 저장소에서 제거됨. 과거에 V1을 적용한 로컬 DB는 `flyway_schema_history`에 이력이 남아 현재 파일과 불일치할 수 있음

개발 DB는 예제 테이블과 Flyway 이력을 정리하거나 재생성 후 사용함. 운영 DB의 Flyway 이력은 임의로 삭제하지 않고 실제 스키마를 확인한 뒤 별도 마이그레이션으로 처리함

도메인 마이그레이션은 `V2` 인증·계정, `V3` 카탈로그, `V4` 운영 계정·업체 프로필, `V5` 상품 이미지·옵션 순서로 확장함. 장바구니·주문·결제·재고 도메인은 다음 신규 버전으로 추가함. 배포된 버전 파일은 수정하지 않음

## 다음 구현 순서

1. 휴대폰 본인 인증, 개인정보 입력·저장, 비활성 계정 활성화 흐름 구현
2. 장바구니와 주문 생성, 재고 예약 구현
3. 결제·주문 상태 전이와 운영 기능 확장

## 확정된 후속 인증 정책

- 로그인 식별자는 정규화한 휴대폰 번호이며, 비밀번호는 사용하지 않음
- `ACTIVE` 계정은 휴대폰 번호만으로 로그인함
- `ACTIVE`가 아닌 기존 계정과 신규 계정은 휴대폰 본인 인증 후 기존 정보 확인 또는 신규 정보 입력을 진행하고, 개인정보 최종 저장 시 `ACTIVE` 처리함
- Access Token은 짧게 유지하고, Refresh Token은 명시적 삭제·무효화 또는 계정 상태 변경 전까지 기기별로 영속 유지함
- 영속 Refresh Token 정책은 V6에 반영함

## 보류된 정책

- 인증번호 발송 제공자와 실패 시 대체 채널
- PG사와 부분 취소·환불 범위
- 배송·매장 픽업·배송비 정책
- 상품 옵션 조합 최대 수와 SKU 코드 발급 규칙
- 개인정보·주문·결제 보존 및 익명화 기준
