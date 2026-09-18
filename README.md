# Umji Market API

엄지철물마켓 Kotlin/Spring Boot 통합 API

## 회원 인증 흐름

비밀번호·카카오 로그인 없이 휴대폰 번호 OTP로 기존 회원을 식별하고, 개인정보 동의와 계정 상태에 따라 서비스 진입을 결정함

```text
휴대폰 인증 -> 기존 회원 조회 -> 동의 또는 업체 정보 입력 -> ACTIVE 확인 -> 토큰 발급
```

Access Token은 15분, 기기별 Refresh Token은 30일로 유지함. Refresh Token 갱신마다 계정 상태·권한을 DB에서 재검증하여 정지·탈퇴·권한 변경을 반영함

전체 회원 흐름: `E:\buyeong_dev\umji-market\SERVICE_FLOW.md`

## 실행

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
./gradlew test
./gradlew check
```

## 로컬 설정

`src/main/resources/application-local.yml.sample`을 참고해 Git 비추적 `application-local.yml`을 구성함

```text
DB_READ_URL
DB_WRITE_URL
DB_USER_NAME
DB_USER_PASSWORD
JWT_ISSUER
JWT_AUDIENCE
JWT_KEY_ID
JWT_RSA_PUBLIC_KEY_PATH
JWT_RSA_PRIVATE_KEY_PATH
```

JWT PEM 파일은 `E:\buyeong_dev\umji-market\secrets\jwt`에 두고 경로만 설정으로 전달함

상태 확인 endpoint: `GET /actuator/health`
