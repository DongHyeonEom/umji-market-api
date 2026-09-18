# 계정 서비스

## 책임

회원과 업체 프로필, 배송지, 계정 상태 조회·수정 담당. 인증·토큰 발급은 인증 서비스가 담당함

## Controller

```text
GET   /api/account/me
PATCH /api/account/me
GET   /api/account/addresses
POST  /api/account/addresses
PATCH /api/account/addresses/{addressId}
DELETE /api/account/addresses/{addressId}
```

## 핵심 규칙

- 본인 계정만 조회·수정 가능, 운영자 변경은 운영 서비스로 분리
- 사업자번호·주소·연락처는 역할 기반 접근 제어와 변경 감사 적용
- 신규 업체 프로필 입력 완료 시 `PENDING_REVIEW` 또는 정책상 `ACTIVE` 전환
- 주소 수정이 기존 주문의 배송지 스냅샷을 바꾸지 않음
