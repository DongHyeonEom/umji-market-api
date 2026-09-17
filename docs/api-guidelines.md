# API Guidelines

## Prefix

기본 API prefix: `/api`

역할별 기능의 prefix 단독 보안 표현 금지. 서버 role/permission 필수 검사

예시:

```text
GET    /api/products
GET    /api/products/{productId}
POST   /api/cart/items
POST   /api/orders
POST   /api/operation/products
PATCH  /api/operation/orders/{orderId}/status
```

`operation`: 관리자성 운영 기능 후보 prefix. 최종 확정 전 문서·코드 병행 검토

## Response Shape

공통 응답 형식 미확정. 아래 초기 후보

```json
{
  "data": {},
  "traceId": "..."
}
```

에러 응답 후보:

```json
{
  "code": "ERROR_CODE",
  "message": "사용자에게 보여줄 메시지",
  "traceId": "...",
  "details": []
}
```

## Pagination

목록 API의 기본 pagination 고려

후보 파라미터:

```text
page
size
sort
```

무한 스크롤 사용자 화면의 cursor 방식 검토

## OpenAPI Tags

도메인·역할 식별 가능한 OpenAPI tag 작성

예시:

- Catalog
- Cart
- Order
- Account
- OperationProduct
- OperationOrder

## WebView Considerations

API 설계 시 고려 흐름

- 인증 만료
- 앱 뒤로가기
- 외부 브라우저 이동
- 결제 앱/PG 이동 후 복귀
- 파일 업로드
- 푸시 토큰 등록
