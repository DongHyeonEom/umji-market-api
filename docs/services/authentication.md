# 인증 서비스

## 책임

휴대폰 번호 단일 로그인, 계정별 휴대폰 본인 인증 여부 판단, 기존 계정 정보 초기화·확인, 신규 계정 정보 등록, Access·Refresh Token 발급·갱신을 담당함. 비밀번호와 소셜 로그인은 사용하지 않음

운영자가 `휴대폰 본인 인증 생략 가능` 값을 허용한 `ACTIVE` 계정은 휴대폰 번호만으로 로그인할 수 있음. 그 외 계정과 신규 휴대폰 번호는 휴대폰 본인 인증 성공 후에만 다음 단계로 진행함

구현 시 계정에 `phone_verification_required` 값을 둠. 기본값은 `true`이며, 운영자가 본인 확인을 마친 `ACTIVE` 계정에 한해 `false`로 변경할 수 있음. 이 값의 변경은 계정 상태 변경과 별도 감사 대상임

## 로그인 알고리즘

```text
휴대폰 번호 입력
  -> 정규화 후 계정 조회
       -> ACTIVE 계정 + 인증 생략 허용: 인증 없이 기존 계정 정보 초기화·확인
       -> 기존 계정 + 인증 생략 미허용: 휴대폰 본인 인증 후 기존 계정 정보 초기화·확인
       -> 계정 없음: 휴대폰 본인 인증 후 신규 계정 정보 입력
  -> 사용자 확인·보완 정보 최종 저장
  -> 이용 가능한 계정이면 Access Token과 Refresh Token 발급
```

`SUSPENDED`, `WITHDRAWN` 계정은 인증 생략 설정과 무관하게 로그인 및 토큰 갱신을 거부함. 운영자가 인증 생략 가능 값을 변경할 때는 처리자·변경 전후 값·시각·traceId를 감사 로그로 남김

## 현재 임시 정책

인증 세부 플로우 확정 전까지 로컬 개발 환경은 `umji.security.authentication.mode=BYPASS`로 모든 요청을 통과시킴. 기본값과 운영 환경은 `REQUIRED`이며, `BYPASS`를 운영·스테이징 설정에 넣지 않음

인증 재도입 시 휴대폰 OTP, 개인정보 동의·서면 동의 이력, 계정 활성화, Access·Refresh Token 흐름을 함께 구현함. 임시 BYPASS 모드에 계정 식별·권한 검사를 의존하지 않음

## Controller

```text
POST /api/auth/login
POST /api/auth/phone/challenges
POST /api/auth/phone/challenges/{challengeId}/verify
GET  /api/auth/activation
POST /api/auth/consents
POST /api/auth/token/refresh
POST /api/auth/tokens/revoke
```

## 핵심 규칙

- 로그인 식별자는 정규화한 휴대폰 번호이며 비밀번호를 사용하지 않음
- 인증 생략은 운영자가 신원을 확인한 `ACTIVE` 계정에만 허용함. 값이 없거나 해제되면 휴대폰 본인 인증이 필요함
- OTP 원문 대신 해시와 만료 시각을 저장하고, 전화번호·IP별 발송·검증 제한을 적용함
- 기존 계정은 서버 저장 정보를 초기값으로 제공하고, 사용자의 확인·보완 뒤 최종 저장함
- 신규 계정은 필요한 정보를 입력받아 저장하고, 자동 활성화 또는 운영 검토 상태를 적용함
- Access Token은 짧게 유지하고, Refresh Token은 명시적 삭제·무효화, 계정 상태 변경, `tokenVersion` 증가 전까지 기기별 영속 세션으로 유지함
- Refresh Token은 기기별 저장·회전하며, 원문은 클라이언트 보관·서버 SHA-256 hash 저장 방식임
- 갱신마다 계정 상태·권한·`tokenVersion`을 재검증하며, 정지·탈퇴·불일치 시 세션을 폐기함

전체 사용자 흐름: `E:\buyeong_dev\umji-market\SERVICE_FLOW.md`
