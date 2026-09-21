# Umji Market API

엄지철물마켓 Kotlin/Spring Boot 통합 API

## 회원 인증 흐름

비밀번호·카카오 로그인 없이 휴대폰 번호로 계정을 식별함. `ACTIVE` 계정은 휴대폰 번호만으로 로그인하고, 비활성 계정과 신규 번호의 휴대폰 본인 인증·개인정보 입력은 후속 구현함

```text
휴대폰 번호 입력 -> ACTIVE 계정 확인 -> Access·Refresh Token 발급
```

Access Token은 15분, 기기별 Refresh Token은 명시적 폐기·계정 상태 변경 전까지 영속 유지함. Refresh Token 갱신마다 계정 상태와 `tokenVersion`을 DB에서 재검증함

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
