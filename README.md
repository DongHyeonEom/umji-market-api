# 엄지마켓 API

Flutter 앱의 React WebView가 사용하는 Kotlin/Spring Boot API입니다.

## 아키텍처

도메인별 헥사고날 아키텍처와 클린 아키텍처를 적용합니다. Controller는 입력 adapter, application UseCase는 업무 흐름, JPA 구현은 출력 adapter입니다. 상세 규칙과 Mermaid 흐름도는 [docs/architecture.md](docs/architecture.md)를 참고하세요.

## 로컬 실행

1. `src/main/resources/application-local.yml.sample`을 복사해 `application-local.yml`을 만듭니다.
2. MySQL 접속 및 JWT 환경 변수를 설정합니다.
3. JWT PEM 파일 경로를 설정합니다.
4. 실행합니다.

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

필수 변수: `DB_READ_URL`, `DB_WRITE_URL`, `DB_USER_NAME`, `DB_USER_PASSWORD`, `JWT_ISSUER`, `JWT_AUDIENCE`, `JWT_KEY_ID`, `JWT_RSA_PUBLIC_KEY_PATH`, `JWT_RSA_PRIVATE_KEY_PATH`.

로컬 프로필은 인증을 우회하는 `BYPASS` 설정을 사용합니다. 운영 환경에는 적용하지 마세요. 기본/운영 인증 모드는 `REQUIRED`입니다.

## 유용한 명령

```bash
./gradlew test
./gradlew check
```

상태 확인: `GET /actuator/health`. 로컬 설정과 키는 저장소에 커밋하지 않습니다.
