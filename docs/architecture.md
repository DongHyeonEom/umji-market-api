# Architecture

## Overview

엄지철물마켓 통합 백엔드 API. Flutter 앱의 WebView 셸 역할, React 프론트의 사용자·관리자 UI 제공, 단일 API 프로젝트의 계정 role/permission 기반 기능 접근 제어

## Key Decision

사용자 API·관리자 API의 별도 서비스 미분리

이유:

- WebView 기반 단일 서비스 도메인 UI 분기
- 초기 MVP 서비스 분리의 운영 복잡도
- 관리자 기능 서버 권한 검사 보호
- 기능 확장 시 서비스 분리 전 패키지·모듈 경계 우선 검토

## Runtime Shape

```text
Flutter App
  -> WebView
    -> React Frontend
      -> umji-market-api
        -> Database / External APIs
```

## Database Access

공용 데이터베이스 연결 구성. `spring.datasource.read`와 `spring.datasource.write`만 유지하며, `@Transactional(readOnly = true)` 호출은 read 연결로 라우팅

접속 URL과 계정 정보는 애플리케이션 설정 파일에 저장하지 않고 실행 환경에서 `DB_READ_URL`, `DB_WRITE_URL`, `DB_USER_NAME`, `DB_USER_PASSWORD`로 주입

## Package Direction

패키지 루트: `com.buyeong.umji.api`

권장 패키지 방향:

```text
com.buyeong.umji.api.auth
com.buyeong.umji.api.account
com.buyeong.umji.api.catalog
com.buyeong.umji.api.cart
com.buyeong.umji.api.order
com.buyeong.umji.api.payment
com.buyeong.umji.api.inventory
com.buyeong.umji.api.operation
com.buyeong.umji.api.notification
com.buyeong.umji.api.file
com.buyeong.umji.api.common
```

## Layer Direction

도메인별 필요 계층

```text
controller
model
dto
service
persistence
mapper
enums
exception
```

규칙:

- Controller: HTTP 관심사
- Model: API request/response
- DTO: service 계층 전달 객체
- Entity: persistence 계층 외부 노출 금지
- Mapper: Entity, DTO, Model 변환

## Role Based UI/API

React의 role/permission 기반 사용자·관리자 화면 노출

서버 보장 사항

- 관리자 role/permission 부재 시 관리자 API 실패
- UI 은닉 기능의 서버 권한 검사 생략 금지
- 감사 필요 관리자 mutation의 audit log 고려

## Future Split Criteria

초기 단일 API. 아래 조건 누적 시 모듈 또는 서비스 분리 검토

- 특정 도메인의 독립적 고속 배포 주기
- Kafka consumer·batch의 API와 다른 확장 요구
- 관리자 운영 기능과 사용자 트래픽의 상이한 보안·성능 요구
- 공통 코드 대비 도메인 독립성 중요도 증가
