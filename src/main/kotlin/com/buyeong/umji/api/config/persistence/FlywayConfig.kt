package com.buyeong.umji.api.config.persistence

import org.flywaydb.core.Flyway
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

/**
 * 엄지마켓 write 데이터소스용 Flyway 설정.
 *
 * 모든 도메인 마이그레이션은 `db/migration`에서 단일 이력으로 관리합니다.
 *
 * 마이그레이션 파일 명명 규칙:
 * - V{version}__{description}.sql (예: V1__create_table.sql)
 * - R__{description}.sql (반복 가능한 마이그레이션)
 *
 * 활성화 조건:
 * - spring.flyway.enabled=true 설정 필요
 *
 * @see <a href="https://flywaydb.org/documentation/concepts/migrations">Flyway Migrations</a>
 */
@Configuration
@ConditionalOnProperty(name = ["spring.flyway.enabled"], havingValue = "true")
class FlywayConfig {
    /**
     * 엄지마켓 write 데이터소스용 Flyway 마이그레이션.
     */
    @Bean(initMethod = "migrate")
    fun umjiMarketFlyway(
        @Qualifier("defaultWriteDataSource") dataSource: DataSource,
    ): Flyway =
        Flyway
            .configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .baselineVersion("0")
            .load()

    /**
     * 기본 Flyway 자동 마이그레이션 비활성화.
     * 명시적으로 구성한 write 데이터소스 Bean이 마이그레이션을 수행합니다.
     */
    @Bean
    fun flywayMigrationStrategy(): FlywayMigrationStrategy = FlywayMigrationStrategy { }
}