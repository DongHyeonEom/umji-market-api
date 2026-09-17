# Umji Market API

엄지철물마켓 통합 Kotlin/Spring Boot API 프로젝트

전체 프로젝트 방향성: `E:\buyeong_dev\umji-market\AGENTS.md`

## Role

Flutter WebView/React 프론트 호출용 통합 백엔드 API. 계정 role/permission 기반 사용자·관리자 UI 분기 및 관리자성 기능 서버 권한 검사

## Local Commands

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
./gradlew test
./gradlew check
```

## Runtime Environment Variables

Key Vault를 사용하지 않으므로 DB 자격 증명은 실행 환경 변수로 제공 필요

```text
DB_READ_URL
DB_WRITE_URL
DB_USER_NAME
DB_USER_PASSWORD
TLS_KEY_STORE_PASSWORD
```

## Package Root

```text
com.buyeong.umji.api
```

## Current Baseline

스켈레톤 유지·교체·제거 기준: [`docs/skeleton-cleanup-audit.md`](docs/skeleton-cleanup-audit.md)

상태 확인 endpoint: `GET /actuator/health`
