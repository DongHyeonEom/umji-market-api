# 인증

## 구현 상태

휴대폰 번호 로그인과 Access/Refresh Token 발급·갱신·폐기를 구현했음.\
비밀번호·소셜 로그인을 제공하지 않음.\
OTP 검증, 신규/비활성 계정 활성화, 동의 API는 아직 없음.

## Endpoint

- `POST /api/auth/login`
- `POST /api/auth/token/refresh`
- `POST /api/auth/tokens/revoke`

로그인 번호를 정규화해 계정을 조회함.\
`ACTIVE` 계정은 토큰을 발급하고, 비활성 또는 미등록 계정은 `PHONE_VERIFICATION_REQUIRED` 결과를 수신.\
정지·탈퇴 계정은 로그인할 수 없음.

## 토큰 정책

Access Token 유효기간은 1시간.\
Refresh Token은 기기별로 발급하며 DB에는 hash와 만료 시각을 저장함.\
Refresh Token은 발급·갱신 시점부터 1년간 사용되지 않으면 만료됨.\
갱신할 때 기존 토큰을 폐기하고 새 토큰을 발급해 만료 시각을 1년 뒤로 연장함.\
최대 세션 수명 제한은 없으므로 유효한 세션을 계속 사용하는 동안 앱 로그인 상태가 유지됨.\
로그아웃은 해당 Refresh Token을 폐기함.\
정지 계정은 새 로그인과 Refresh Token 갱신이 거부됨.

Access Token 검증 시 계정 상태와 token version을 DB의 현재 값과 대조함.\
계정 정지 또는 token version 변경 시 기존 Access Token은 즉시 인증 실패 처리되며, 정지 계정의 Refresh Token 갱신도 거부됨.\
인증할 수 없는 토큰은 `401`, 인증은 유효하나 endpoint 권한이 부족한 요청은 `403`을 반환함.

## 보안 모드

기본 모드는 `REQUIRED`이며, 로컬 프로필은 `BYPASS`를 사용함.\
BYPASS는 모든 요청의 인증을 우회하므로 운영 환경에서 사용하지 않음.
