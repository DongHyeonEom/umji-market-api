# 인증

## 구현 상태

휴대폰 번호 로그인과 Access/Refresh Token 발급·갱신·폐기를 구현했음.<br>
비밀번호·소셜 로그인을 제공하지 않음.<br>
SMS 휴대폰 본인 확인과 고객 셀프 가입·활성화 API는 아직 없음.<br>
운영자 계정 관리 API의 동의 이력 기록과 승인을 통한 활성화는 제공됨.<br>
ACTIVE 계정의 일반 로그인마다 코드를 요구하지 않음.<br>
운영자가 기존 회원의 오프라인 서면 동의를 기록하고 계정을 승인해 `ACTIVE`로 전환하면 사용자는 휴대폰 번호만으로 로그인 가능.<br>
신규 앱 신청 등 그 외 신규·비활성 계정 활성화를 위한 SMS 본인 확인은 아직 미구현.<br>

## 알고리즘 흐름

```mermaid
flowchart TD
    A[휴대폰 로그인 요청] --> B[전화번호 정규화]
    B --> C{계정 조회}
    C -- 없음 --> V[PHONE_VERIFICATION_REQUIRED]
    C -- 찾음 --> D{계정 상태 ACTIVE}
    D -- 아니오 --> V
    D -- 예 --> E[로그인 시각 기록]
    E --> F[Access Token 발급]
    F --> G[Refresh Token 난수 생성 및 hash 저장]
    G --> H[인증 응답]

    I[Refresh 요청] --> J[토큰 hash로 행 잠금 조회]
    J --> K{토큰·계정·만료·기기 유효}
    K -- 아니오 --> X[요청 거부]
    K -- 예 --> L[기존 Refresh Token 폐기]
    L --> F

    M[로그아웃 요청] --> N[토큰 hash로 세션 조회]
    N --> O{미폐기 세션 존재}
    O -- 예 --> P[세션 폐기]
    O -- 아니오 --> Q[변경 없이 종료]
```

Access Token 유효성 검사 시 계정 상태와 token version을 현재 저장값과 대조.<br>
토큰 해시는 SHA-256으로 저장하며 원문은 저장하지 않음.<br>

## Endpoint

- `POST /api/auth/login`
- `POST /api/auth/token/refresh`
- `POST /api/auth/tokens/revoke`

로그인 번호를 정규화해 계정을 조회함.<br>
`ACTIVE` 계정은 토큰을 발급하고, 비활성 또는 미등록 계정은 `PHONE_VERIFICATION_REQUIRED` 결과를 수신.<br>
정지·탈퇴 계정은 로그인할 수 없음.<br>

## 토큰 정책

Access Token 유효기간은 1시간.<br>
Refresh Token은 기기별로 발급하며 DB에는 hash와 만료 시각을 저장함.<br>
Refresh Token은 발급·갱신 시점부터 1년간 사용되지 않으면 만료됨.<br>
갱신할 때 기존 토큰을 폐기하고 새 토큰을 발급해 만료 시각을 1년 뒤로 연장함.<br>
최대 세션 수명 제한은 없으므로 유효한 세션을 계속 사용하는 동안 앱 로그인 상태가 유지됨.<br>
로그아웃은 해당 Refresh Token을 폐기함.<br>
정지 계정은 새 로그인과 Refresh Token 갱신이 거부됨.<br>

Access Token 검증 시 계정 상태와 token version을 DB의 현재 값과 대조함.<br>
계정 정지 또는 token version 변경 시 기존 Access Token은 즉시 인증 실패 처리되며, 정지 계정의 Refresh Token 갱신도 거부됨.<br>
인증할 수 없는 토큰은 `401`, 인증은 유효하나 endpoint 권한이 부족한 요청은 `403`을 반환함.<br>

## 보안 모드

기본 모드는 `REQUIRED`이며, 로컬 프로필은 `BYPASS`를 사용함.<br>
BYPASS는 모든 요청의 인증을 우회하므로 운영 환경에서 사용하지 않음.<br>
