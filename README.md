# Umji Market API

엄지철물마켓 Kotlin/Spring Boot 통합 API

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
