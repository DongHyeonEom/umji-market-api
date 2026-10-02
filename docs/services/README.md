# 서비스 문서

이 문서는 사용자와 운영자 업무 흐름을 나누어 해당 도메인 문서의 기준을 찾기 위한 목차.<br>
전체 채널·계정·그룹·화면 권한 흐름은 작업 공간의 [SERVICE_FLOW.md](../../../SERVICE_FLOW.md)를 기준으로 함.<br>
도메인 문서는 현재 구현과 미구현 설계안을 명확히 구분하며, 계획 endpoint·table을 운영 중인 계약처럼 기록하지 않음.<br>
서비스 개발·수정은 해당 문서 Mermaid 흐름과 수용 기준을 먼저 갱신하고 코드가 그 흐름을 따르도록 진행.<br>

## 사용자 흐름

| 업무 | 문서 | 현재 범위 |
| --- | --- | --- |
| 로그인·세션 | [authentication.md](authentication.md) | 휴대폰 식별, Access/Refresh Token, 계정 상태 검사 |
| 개인 계정·구매자 그룹 | [account.md](account.md) | 프로필, 그룹 onboarding·대표자/구성원 관리, 그룹 공용 배송지 |
| 상품 탐색 | [catalog.md](catalog.md) | 공개 카테고리·상품 조회 |
| 장바구니 | [cart.md](cart.md) | SKU 추가·수정·삭제·조회 |
| 주문·배송 조회 | [order.md](order.md) | 그룹 귀속 주문·배송정보 조회·전체 주문 취소 |
| 계좌이체 안내 | [payment.md](payment.md) | 주문별 계좌 snapshot·입금 상태 확인 결과 |

## 운영자 흐름

| 업무 | 문서 | 현재 범위 |
| --- | --- | --- |
| 계정·role·그룹 운영 | [operation.md](operation.md) | 계정·동의·승인, role 관리, 그룹 연결 및 대표자 변경 |
| 카탈로그 운영 | [operation.md](operation.md) | 카테고리·브랜드·상품·SKU·이미지 metadata 관리 |
| 재고 운영 | [inventory.md](inventory.md) | 재고 조정·변동 조회, 주문 예약·해제·확정 |
| 주문·배송 운영 | [order.md](order.md), [operation.md](operation.md) | 입금·취소·환불 상태, 출고 준비·송장·배송완료 처리 |
| 운영 감사 | [operation.md](operation.md) | 변경 감사 이력 조회·보존 관리 |

## 미구현 서비스

| 업무 | 문서 | 상태 |
| --- | --- | --- |
| 파일 업로드·민감 증빙 조회 | [file.md](file.md) | 구현 전. 저장 장비·접근 정책 결정 필요 |
| 주문·결제·배송 알림 | [notification.md](notification.md) | 제공자·동의·재시도 정책 결정 필요 |

화면별 권한 설정, 운영 업무 role 및 영업 인센티브의 설계안·미구현 범위는 [operation.md](operation.md), 대표자·일반구성원 판정은 [account.md](account.md)를 기준으로 함.<br>
