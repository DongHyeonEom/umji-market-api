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

## 핵심 규칙

- `ADMIN_ACCOUNT_MANAGE` 등 필요한 permission을 endpoint별로 검사
- 서면 동의는 문서 버전, 증빙 참조, 담당자, 처리 시각 보관
- 운영 mutation은 before·after·처리자·traceId 감사 로그 보관
- 계정 정지·탈퇴 처리 시 Refresh Token 폐기와 tokenVersion 증가 수행
