# Skeleton Cleanup Audit

## 목적

엄지철물마켓 도메인 구현 전 기존 Spring Boot 스켈레톤의 유지·교체·제거 범위 확정

원본 스켈레톤 저장소 `E:\buyeong_dev\buyeong-spring-boot-internal-api-skeleton` 수정 금지. 본 저장소의 예제 코드는 후속 정리 PR에서 제거

## 확인 기준

- 기준 브랜치: `dev`
- 패키지 루트: `com.buyeong.umji.api`
- 실행 환경: Java 21, Kotlin, Spring Boot 3.4, Gradle Kotlin DSL
- DB 기준: MySQL 8.0+, read/write datasource, Flyway

## 유지 대상

| 영역 | 현재 위치 | 유지 이유 | 후속 조정 |
| --- | --- | --- | --- |
| 애플리케이션 진입점 | `UmjiMarketApiApplication.kt` | 서비스 패키지 루트와 Spring Boot 설정 기준 | 도메인 설정 등록 시 확장 |
| read/write 데이터소스 | `config/persistence` | 읽기 전용 트랜잭션 라우팅, 환경 변수 기반 DB 연결 | 운영 환경 연결 검증 |
| JPA·Flyway 설정 | `config/persistence` | 도메인 마이그레이션과 JPA 기반 영속성 기반 | V2부터 엄지마켓 전용 마이그레이션 추가 |
| 도메인 공통 엔티티 | `persistence/jpa/entity/backbone/Domain*.kt` | `Long` 내부 식별자, `UUID` 공개 식별자, 낙관적 잠금, UTC 시각 기준 충족 | catalog 구현 시 첫 사용 |
| 공통 예외 처리 | `exception`, `handler`, `model/ErrorResponseModel.kt` | API 오류 응답 기반 | 응답 형식 확정 후 `code`, `message`, `traceId`, `details`로 정렬 |
| 추적·로깅 | `filter/TraceIdFilter.kt`, `util/LoggerExtension.kt` | 요청 추적과 운영 로그 기반 | 응답 헤더 trace ID 노출 여부 결정 |
| 테스트 기반 | unit, integration, e2e source set 및 Testcontainers 설정 | 도메인별 테스트 추가 기반 | 예제 테스트 제거와 함께 도메인 테스트 교체 |
| OpenAPI·Actuator | `SwaggerConfig.kt`, Actuator 의존성 | API 문서화와 상태 확인 기반 | `/api` 계약과 태그 적용 |

## 교체 대상

| 영역 | 현재 위치 | 교체 방향 |
| --- | --- | --- |
| 공통 오류 코드 | `enums/ErrorCode.kt` | 엄지마켓 표준 오류 코드와 API 응답 형식으로 재정의 |
| 감사 사용자 조회 | `SimpleAuditorAware.kt` | 인증된 Account 공개 식별자 또는 내부 식별자 기준으로 교체 |
| 설정 이름 | `Default*Configuration.kt` | 멀티 모듈 또는 멀티 datasource 필요성 발생 전까지 유지, 도메인 명명 규칙 확정 후 정리 |
| 클라이언트 설정 | `config/client`, `service/biz/client` | 외부 PG·파일·알림 연동 확정 전 사용 보류, 실제 외부 연동 계약으로 교체 |

## 제거 완료 대상

| 영역 | 현재 위치 | 제거 조건 |
| --- | --- | --- |
| Staff API·서비스·DTO·모델·매퍼 | `controller/StaffRestController.kt`, `service/biz/StaffBizService.kt`, `service/mapping/StaffMappingService.kt`, `dto/StaffDto.kt`, `model/StaffModel.kt`, `mapper/StaffMapper.kt` | 제거 완료 |
| CDI 예제 영속성 | `persistence/**/cdi`, `persistence/jpa/entity/cdi` | 제거 완료 |
| Test·Example API/DTO/Mapper/Service | `dto/TestDto.kt`, `dto/example`, `enums/example`, `mapper/example`, `service/example`, `persistence/jpa/entity/cdi/TestJpaEntity.kt` | 제거 완료 |
| Staff·Example·CDI 테스트 | `src/test/**/staff`, `src/test/**/example`, `src/test/**/cdi` | 제거 완료 |
| 예제 Flyway DDL | `db/migration/V1__create_skeleton_example_tables.sql` | 제거 완료 |
| 스켈레톤 전용 enum·JDBC 변환기 | `StaffStatusEnum.kt`, `SchoolTypeEnum.kt`, `persistence/jdbc/**/cdi` | 제거 완료 |

## 확정 패키지 구조

```text
com.buyeong.umji.api
├── auth
├── account
├── catalog
├── cart
├── order
├── payment
├── inventory
├── operation
├── notification
├── file
└── common
    ├── config
    ├── exception
    ├── persistence
    ├── tracing
    └── web
```

도메인 내부 계층: `controller`, `model`, `dto`, `service`, `persistence`, `mapper`, `enums`, `exception`

기존 최상위 공통 패키지는 후속 도메인 구현과 함께 `common` 경계로 이동. 한 번의 대규모 이동 금지

## 마이그레이션 기준

- `V1__create_skeleton_example_tables.sql`: 예제 테이블만 포함, 엄지마켓 도메인에서 사용 금지
- `V2` 이후: `docs/database-design.md`의 인증·계정, 카탈로그, 장바구니, 주문·결제, 재고·운영 순서 적용
- 운영 반영 후 마이그레이션 수정 금지
- 예제 V1 처리: 개발 DB 사용 여부 확인 후 별도 cleanup PR에서 결정

## 확인 결과

- 현재 브랜치 `dev`와 `origin/dev` 동기화 상태 확인
- 원격 `origin` 확인: `https://github.com/DongHyeonEom/umji-market-api.git`
- Actuator health endpoint 설정 확인: `GET /actuator/health`
- `spring.datasource.read`, `spring.datasource.write` 환경 변수 주입 구조 확인
- 도메인 공통 엔티티의 `Long` 식별자, `UUID` 공개 식별자, `Instant` 시각 정책 확인

## 후속 PR 순서

1. `feature/auth-foundation`: Account, Role, Permission 및 인증 방식 확정
2. `feature/catalog-domain-foundation`: Category, Brand, Product, SKU 모델과 공개 상품 조회 API 구현

공유 운영 DB에 V1 예제 테이블이 존재하면 별도 운영 마이그레이션으로 삭제 여부 검토
