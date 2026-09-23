# 관리자 운영

관리자 endpoint는 `/api/operation/**` 아래에 있습니다. `REQUIRED` 모드는 인증된 요청을 요구하고, 로컬 전용 `BYPASS`는 인증 검사를 모두 생략합니다. 관리자 role/permission의 endpoint별 인가 및 변경 감사 로그는 아직 구현되지 않았으므로 권한 검사가 완료된 것으로 간주하지 않습니다.

## 계정

- `POST /api/operation/accounts`
- `GET /api/operation/accounts?status=&page=&size=`
- `GET /api/operation/accounts/{id}`
- `PATCH /api/operation/accounts/{id}/status`
- `PUT /api/operation/accounts/{id}/business-profile`
- `POST /api/operation/accounts/{id}/consents`
- `POST /api/operation/accounts/{id}/approve`

계정은 동의·프로필 상태에 따라 대기 상태를 거쳐 승인됩니다. 개인정보 동의 이력을 확인한 뒤 승인하고 계정 token version을 갱신합니다.

## 카탈로그

관리자 카테고리·브랜드·상품 및 이미지·옵션·SKU endpoint는 [catalog.md](catalog.md)에 정리했습니다.

## 구조

계정과 카탈로그는 각각 `operation/account`, `operation/catalog`에 둡니다. HTTP 모델은 web adapter에서 application 명령·응답 모델로 바꾸고, persistence adapter에서만 JPA Entity를 사용합니다.
