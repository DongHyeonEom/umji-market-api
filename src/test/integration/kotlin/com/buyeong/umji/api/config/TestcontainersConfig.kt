package com.buyeong.umji.api.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.utility.DockerImageName

/**
 * Testcontainers 설정.
 *
 * Docker 컨테이너를 사용하여 실제 데이터베이스와 통합 테스트를 수행합니다.
 *
 * 사용 예시:
 * ```kotlin
 * @SpringBootTest
 * @Import(TestcontainersConfig::class)
 * @Testcontainers
 * class MyDatabaseIntegrationTest {
 *     // 테스트 코드
 * }
 * ```
 *
 * 주의사항:
 * - Docker가 설치되어 있어야 합니다
 * - 테스트 실행 시 컨테이너 시작으로 인한 초기 지연이 있습니다
 * - CI/CD 환경에서는 Docker-in-Docker 또는 Docker socket 마운트가 필요합니다
 */
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfig {
    companion object {
        private const val MYSQL_IMAGE = "mysql:8.0"
    }

    /**
     * MySQL 테스트 컨테이너.
     *
     * 기본 설정:
     * - 이미지: mysql:8.0
     * - 데이터베이스: test
     * - 사용자: test
     * - 비밀번호: test
     *
     * @ServiceConnection으로 Spring Boot가 자동으로 데이터소스를 구성합니다.
     */
    @Bean
    @ServiceConnection
    fun mysqlContainer(): MySQLContainer<*> =
        MySQLContainer(DockerImageName.parse(MYSQL_IMAGE))
            .withDatabaseName("test")
            .withUsername("test")
            .withPassword("test")
}