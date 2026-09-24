# 관리자 운영

관리자 endpoint는 `/api/operation/**` 아래에 있음. `REQUIRED` 모드에서는 Access Token의 permission claim으로 endpoint별 인가를 적용함. 인증 정보가 없거나 유효하지 않은 요청은 `401`, 인증됐지만 권한이 없는 요청은 `403`으로 거절함. 로컬 전용 `BYPASS`는 권한 검사를 포함한 보안 검사를 생략함. 운영 환경에서 `BYPASS` 사용 금지.

## 권한 매트릭스

| Role | Permission |
| --- | --- |
| `ADMIN`, `SUPER_ADMIN` | 전체 permission |
| `PRODUCT_MANAGER` | `PRODUCT_READ`, `PRODUCT_WRITE` |
| `ORDER_MANAGER` | `ORDER_READ`, `ORDER_WRITE` |
| `INVENTORY_MANAGER` | `INVENTORY_READ`, `INVENTORY_WRITE` |

| Endpoint | Required permission |
| --- | --- |
| `/api/operation/accounts/**` | `ADMIN_ACCOUNT_MANAGE` |
| 카탈로그 조회 endpoint | `PRODUCT_READ` |
| 카탈로그 생성·수정 endpoint | `PRODUCT_WRITE` |
| 재고 조회·변동 조회 endpoint | `INVENTORY_READ` |
| 재고 조정 endpoint | `INVENTORY_WRITE` |

Role-permission 기본 매핑은 Flyway V10에서 적용함. 계정 role 부여·회수 API는 미구현 상태이며 `account_role` 관리 정책은 별도 작업 필요. Access Token에 발급 당시 permission을 담음. 매 요청 시 token version을 DB와 대조하므로 token version 변경 시 기존 Access Token은 즉시 인증 실패하고, 새 토큰부터 변경된 permission이 반영됨. 계정 정지는 상태 검사로 즉시 인증 실패 처리됨. 인증 불가 응답은 `401`, 권한 부족 응답은 `403`임.

### 초기 관리자 role bootstrap

초기 최고 관리자 계정 지정은 운영 DB 접근 권한을 가진 담당자가 수행. `phone_normalized`는 평문 전화번호 대신 정규화된 번호를 사용하며, 계정 상태가 `ACTIVE`인지 먼저 확인.

```sql
SET @admin_account_id = (
    SELECT id
    FROM account
    WHERE phone_normalized = '<정규화된 관리자 전화번호>'
      AND status = 'ACTIVE'
);

START TRANSACTION;

INSERT INTO account_role (account_id, role_id, granted_by)
SELECT @admin_account_id, id, NULL
FROM role
WHERE code = 'SUPER_ADMIN'
  AND @admin_account_id IS NOT NULL
ON DUPLICATE KEY UPDATE granted_at = CURRENT_TIMESTAMP(3);

SELECT a.id, a.name, a.status, r.code
FROM account_role ar
JOIN account a ON a.id = ar.account_id
JOIN role r ON r.id = ar.role_id
WHERE a.id = @admin_account_id;

COMMIT;
```

결과에 대상 계정과 `SUPER_ADMIN`이 한 건씩 표시되는지 확인. 결과가 비어 있거나 예상과 다르면 `COMMIT` 전 `ROLLBACK` 수행. 일반 운영 role의 부여·회수는 별도 운영 절차 또는 관리자 role 관리 API 도입 전까지 승인된 DB 작업으로 처리.

## 계정

- `POST /api/operation/accounts`
- `GET /api/operation/accounts?status=&page=&size=`
- `GET /api/operation/accounts/{id}`
- `PATCH /api/operation/accounts/{id}/status`
- `PUT /api/operation/accounts/{id}/business-profile`
- `POST /api/operation/accounts/{id}/consents`
- `POST /api/operation/accounts/{id}/approve`

계정은 동의·프로필 상태에 따라 대기 상태를 거쳐 승인됨. 개인정보 동의 이력을 확인한 뒤 승인하고 계정 token version을 갱신함.

## 카탈로그

관리자 카테고리·브랜드·상품 및 이미지·옵션·SKU endpoint는 [catalog.md](catalog.md)에 정리했음.

## 구조

계정과 카탈로그는 각각 `operation/account`, `operation/catalog`에 배치. HTTP 모델은 web adapter에서 application 명령·응답 모델로 바꾸고, persistence adapter에서만 JPA Entity를 사용함. Role 관리와 운영 변경 감사 로그는 미구현.
