# 인증 서비스

## 책임

휴대폰 OTP, 개인정보 동의, 기존 회원 활성화, Access·Refresh Token 발급과 폐기 담당. 카카오 로그인과 비밀번호 로그인은 사용하지 않음

## Controller

```text
POST /api/auth/phone/challenges
POST /api/auth/phone/challenges/{challengeId}/verify
GET  /api/auth/activation
POST /api/auth/consents
POST /api/auth/token/refresh
POST /api/auth/logout
```

## 핵심 규칙

- OTP 원문 대신 해시와 만료 시각 저장, 전화번호·IP별 발송·검증 제한 적용
- 인증 후 기존 회원의 동의·활성화 상태에 따라 다음 화면 결정
- Refresh Token은 기기별 저장·회전, 갱신마다 계정 상태·권한·tokenVersion 재검증
- 정지·탈퇴·tokenVersion 불일치 시 해당 계정 세션 전체 폐기

전체 사용자 흐름: `E:\buyeong_dev\umji-market\SERVICE_FLOW.md`
