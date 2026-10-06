# 엄지마켓 API

Flutter 앱의 React WebView가 사용하는 Kotlin/Spring Boot API임.

## 아키텍처

도메인별 계층형 구조를 사용함.
요청 흐름은 Controller → 구체 도메인 Service → JpaEntityService → Spring Data Repository → JPA Entity임.
상세 규칙과 Mermaid 흐름도는 [docs/architecture.md](docs/architecture.md)를 참고.

## 로컬 실행

1. `src/main/resources/application-local.yml.sample`을 복사해 `application-local.yml`을 생성.
2. MySQL 접속 및 JWT 환경 변수를 설정함.
3. JWT PEM 파일 경로를 설정함.
4. 실행함.

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

필수 변수: `DB_READ_URL`, `DB_WRITE_URL`, `DB_USER_NAME`, `DB_USER_PASSWORD`, `JWT_ISSUER`, `JWT_AUDIENCE`, `JWT_KEY_ID`, `JWT_RSA_PUBLIC_KEY_PATH`, `JWT_RSA_PRIVATE_KEY_PATH`, `UMJI_BANK_STANDARD_NAME`, `UMJI_BANK_STANDARD_ACCOUNT_NUMBER`, `UMJI_BANK_STANDARD_ACCOUNT_HOLDER`, `UMJI_BANK_TAX_INVOICE_NAME`, `UMJI_BANK_TAX_INVOICE_ACCOUNT_NUMBER`, `UMJI_BANK_TAX_INVOICE_ACCOUNT_HOLDER`.

관리자 TOTP를 사용할 때 `UMJI_AUTH_TOTP_ENCRYPTION_KEY`에 32바이트 난수 키의 Base64 값을 secret store 또는 환경 변수로 주입. 키 생성 예: `openssl rand -base64 32`. 기존 계정의 TOTP secret 복호화를 위해 키를 안정적으로 보관하며 변경 시 등록된 관리자 TOTP 재등록 필요.

계좌 안내 변수는 세금계산서 미발행 계좌(`UMJI_BANK_STANDARD_*`)와 발행 계좌(`UMJI_BANK_TAX_INVOICE_*`)를 각각 설정함. 주문 생성 시 선택한 계좌 정보는 주문에 스냅샷으로 저장.

공급자 세금계산서 정보는 `application.yml`에서 `UMJI_TAX_INVOICE_SUPPLIER_BUSINESS_REGISTRATION_NUMBER`, `UMJI_TAX_INVOICE_SUPPLIER_BUSINESS_NAME`, `UMJI_TAX_INVOICE_SUPPLIER_NAME`, `UMJI_TAX_INVOICE_SUPPLIER_ADDRESS`, `UMJI_TAX_INVOICE_SUPPLIER_INDUSTRY`, `UMJI_TAX_INVOICE_SUPPLIER_ITEM`, `UMJI_TAX_INVOICE_SUPPLIER_EMAIL` 환경 변수로 주입. 일곱 값이 모두 설정되어야 발행 요청 주문 생성 가능. 실제 공급자 값은 저장소에 저장하지 않음.

사용자가 사업자 구매자 그룹을 새로 생성할 때 국세청 사업자등록 상태조회 API를 호출. 공공데이터포털에서 발급한 인증키를 `UMJI_BUSINESS_REGISTRATION_STATUS_SERVICE_KEY`로 주입. 운영자 사전등록 그룹은 백그라운드 상태 확인 후 계속사업자·휴업자인 경우 대표자가 사업자 정보를 확인·완성. 폐업·확인 오류·미확인 상태에서는 세금계산서 발행 정보 확정 불가. 배송지는 사업자등록 주소와 별도로 그룹 공용 배송지에 등록.

FCM 발송 사용 시 `UMJI_NOTIFICATION_FCM_ENABLED=true`, `UMJI_NOTIFICATION_FCM_PROJECT_ID`와 Google Application Default Credentials를 설정함. APNs 발송 사용 시 `UMJI_NOTIFICATION_APNS_ENABLED=true`, `UMJI_NOTIFICATION_APNS_TEAM_ID`, `UMJI_NOTIFICATION_APNS_KEY_ID`, `UMJI_NOTIFICATION_APNS_PRIVATE_KEY_PATH`, `UMJI_NOTIFICATION_APNS_TOPIC`, `UMJI_NOTIFICATION_APNS_ENVIRONMENT`를 설정함. provider 미사용이 기본값이며 자격 증명은 저장소 밖에서 주입.

로컬 프로필은 인증을 우회하는 `BYPASS` 설정을 사용함.
운영 환경 사용 금지.
기본/운영 인증 모드는 `REQUIRED`임.

## 유용한 명령

```bash
./gradlew test
./gradlew check
```

상태 확인: `GET /actuator/health`.
로컬 설정과 키는 저장소에 커밋하지 않음.
