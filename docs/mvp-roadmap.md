# MVP Roadmap

## Goal

스켈레톤의 엄지철물마켓 통합 API 전환 및 상품 탐색부터 주문 요청까지 MVP 흐름 구축

## Phase 1: Skeleton Cleanup

- 기존 예제 도메인 파악
- 유지할 공통 인프라와 제거할 예제 코드 구분
- 패키지 구조를 `com.buyeong.umji.api` 기준으로 재정렬
- application 설정에서 스켈레톤 흔적 정리
- 기본 health check 또는 root API 확인

## Phase 2: Auth Foundation

- Account, Role, Permission 방향 확정
- 인증 방식 후보 결정
- 관리자성 API 권한 검사 방식 결정
- 공통 security config 정리

## Phase 3: Catalog MVP

- Product, Category, ProductImage 기본 모델 정의
- 상품 목록 API
- 상품 상세 API
- 관리자성 상품 등록/수정 API 후보 설계

## Phase 4: Cart And Order MVP

- 장바구니 담기/수정/삭제
- 주문 요청
- 주문 내역 조회
- 주문 상태 모델 정의

## Phase 5: Operation MVP

- 관리자 상품 관리
- 관리자 주문 상태 변경
- 재고 수량 관리
- 관리자 감사 로그 후보 적용

## Immediate Next Step

`docs/architecture.md` 기준 실제 패키지 구조 확정 후 기존 스켈레톤 예제 코드 유지·삭제 목록 작성
