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

## 마이그레이션 주의사항

스켈레톤 예제용 `V1__create_skeleton_example_tables.sql`은 저장소에서 제거됨. 과거에 V1을 적용한 로컬 DB는 `flyway_schema_history`에 이력이 남아 현재 파일과 불일치할 수 있음

개발 DB는 예제 테이블과 Flyway 이력을 정리하거나 재생성 후 사용함. 운영 DB의 Flyway 이력은 임의로 삭제하지 않고 실제 스키마를 확인한 뒤 별도 마이그레이션으로 처리함

도메인 마이그레이션은 `V2` 인증·계정, `V3` 카탈로그, `V4` 장바구니, `V5` 주문·결제, `V6` 재고·운영 순서로 확장함. 배포된 버전 파일은 수정하지 않음

## 다음 구현 순서

1. 카탈로그 관리자 등록·수정, 상품 이미지·옵션 관리 구현
2. 인증 정책 확정 후 Account, Role, Permission과 로그인·토큰 발급 구현
3. 장바구니와 주문 생성, 재고 예약 구현
4. 결제·주문 상태 전이와 운영 기능 확장

## 보류된 정책

- 최초 인증 수단: 자체 로그인, 소셜 로그인, 휴대폰 인증
- PG사와 부분 취소·환불 범위
- 배송·매장 픽업·배송비 정책
- 상품 옵션 조합 최대 수와 SKU 코드 발급 규칙
- 개인정보·주문·결제 보존 및 익명화 기준
