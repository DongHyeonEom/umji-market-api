# 운영 서비스

## 책임

관리자 계정 활성화·정지, 서면 개인정보 동의 기록, 신규 업체 승인, 상품·주문·재고 운영 변경과 감사 담당

## Controller

```text
GET   /api/operation/accounts
PATCH /api/operation/accounts/{accountId}/status
POST  /api/operation/accounts/{accountId}/consents
POST  /api/operation/accounts/{accountId}/approve
PATCH /api/operation/orders/{orderId}/status
```

계정 운영 구현 범위:

```text
POST  /api/operation/accounts
GET   /api/operation/accounts
GET   /api/operation/accounts/{accountId}
PATCH /api/operation/accounts/{accountId}/status
PUT   /api/operation/accounts/{accountId}/business-profile
POST  /api/operation/accounts/{accountId}/consents
POST  /api/operation/accounts/{accountId}/approve
```

사전 등록 계정은 `PENDING_CONSENT`로 생성함. 개인정보 동의 기록 후 업체 프로필 유무에 따라 `PENDING_PROFILE` 또는 `PENDING_REVIEW`로 전환하며, 운영 승인 시 `ACTIVE`로 전환함

카탈로그 운영 구현 범위:

```text
POST  /api/operation/categories
POST  /api/operation/brands
POST  /api/operation/products
PATCH /api/operation/products/{productId}/status
GET   /api/operation/categories
GET   /api/operation/brands
GET   /api/operation/products
GET   /api/operation/products/{productId}
```

인증이 임시 `BYPASS` 모드인 동안에는 권한 검사를 적용하지 않음. 인증 재도입 시 위 endpoint에 `PRODUCT_WRITE` 권한 검사를 우선 연결함

운영 조회는 공개 조회와 별도로 노출 상태·판매 상태와 연결된 카테고리·브랜드·SKU 정보를 포함함. 공개 API는 노출·판매 가능한 데이터만 반환하고, 운영 API는 숨김·판매 중지 데이터를 포함함

## 핵심 규칙

- `ADMIN_ACCOUNT_MANAGE` 등 필요한 permission을 endpoint별로 검사
- 서면 동의는 문서 버전, 증빙 참조, 담당자, 처리 시각 보관
- 운영 mutation은 before·after·처리자·traceId 감사 로그 보관. 감사 로그 테이블과 현재 계정 연결은 인증 재도입 전 후속 작업
- 계정 정지·탈퇴 처리 시 Refresh Token 폐기와 tokenVersion 증가 수행
