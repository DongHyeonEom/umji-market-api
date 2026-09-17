# Auth And Permission

## Goal

단일 WebView 서비스의 role/permission 기반 사용자·관리자 UI 분기. 인증·권한 설계의 서비스 경계 역할

## Initial Model

초기 후보:

```text
Account
Role
Permission
AccountRole
RolePermission
```

Role 후보:

- CUSTOMER
- ADMIN
- PRODUCT_MANAGER
- ORDER_MANAGER
- INVENTORY_MANAGER
- SUPER_ADMIN

Permission 후보:

- PRODUCT_READ
- PRODUCT_WRITE
- ORDER_READ
- ORDER_WRITE
- INVENTORY_READ
- INVENTORY_WRITE
- ADMIN_ACCOUNT_MANAGE

## Server Rules

- 관리자성 API의 role 또는 permission 필수 검사
- 관리자 메뉴 은닉 여부와 무관한 서버 검사
- 권한 실패의 표준 에러 응답
- 중요 관리자 mutation의 감사 로그

## Token Direction

Access Token + Refresh Token 방식

- Access Token: RS256 서명 JWT, 10~15분 만료
- Refresh Token: 난수 원문 클라이언트 보관, 서버 SHA-256 hash 저장, 만료·폐기 관리
- JWT claim: `sub`, `iss`, `aud`, `roles`, `tokenVersion`, `iat`, `exp`
- 키 보관: 환경 변수 또는 secret manager, 소스·설정 파일 평문 보관 금지
- 검증: Spring Security OAuth2 Resource Server와 `JwtDecoder`
- 발급: Spring Security `JwtEncoder`, 단일 API 시작 후 인증 서비스 분리 가능 구조

WebView 환경의 refresh token 보관 방식: `HttpOnly`, `Secure` cookie 또는 Flutter native secure storage 중 프론트·앱 bridge 계약 확정 후 선택

## Open Questions

- 자체 로그인부터 시작할지 소셜/휴대폰 인증을 포함할지
- 관리자 계정은 일반 회원 계정과 같은 로그인 화면을 쓸지
- 관리자 2FA가 MVP에 필요한지
- 권한을 Role 중심으로 단순화할지 Permission까지 처음부터 둘지
