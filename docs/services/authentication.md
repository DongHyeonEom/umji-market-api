# 인증

## 구현 상태

휴대폰 번호 로그인과 Access/Refresh Token 발급·갱신·폐기를 구현했습니다. 비밀번호·소셜 로그인을 제공하지 않습니다. OTP 검증, 신규/비활성 계정 활성화, 동의 API는 아직 없습니다.

## Endpoint

- `POST /api/auth/login`
- `POST /api/auth/token/refresh`
- `POST /api/auth/tokens/revoke`

로그인 번호를 정규화해 계정을 조회합니다. `ACTIVE` 계정은 토큰을 발급하고, 비활성 또는 미등록 계정은 `PHONE_VERIFICATION_REQUIRED` 결과를 받습니다. 정지·탈퇴 계정은 로그인할 수 없습니다.

## 토큰 정책

Access Token은 짧게 만료됩니다. Refresh Token은 기기별로 저장하고 회전·폐기하며, DB에는 hash를 저장합니다. 갱신 시 계정 상태와 token version을 확인합니다. 만료·폐기 기준의 상세 구현은 설정 및 토큰 모델을 기준으로 합니다.

## 보안 모드

기본 모드는 `REQUIRED`이며, 로컬 프로필은 `BYPASS`를 사용합니다. BYPASS는 모든 요청의 인증을 우회하므로 운영 환경에서 사용하지 않습니다.
