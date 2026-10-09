package com.buyeong.umji.api.account.integration

import org.assertj.core.api.Assertions.assertThat
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.MigrationVersion
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.sql.DriverManager
import java.util.UUID

@SpringBootTest
@ActiveProfiles("local")
class LegacyBusinessProfileCleanupMigrationMySqlIntegrationTest {
    @Autowired
    @Qualifier("defaultWriteDataSourceProperties")
    private lateinit var writeDataSourceProperties: DataSourceProperties

    @Test
    fun `v50 removes legacy business profile table`() {
        val url = requireNotNull(writeDataSourceProperties.url)
        val username = requireNotNull(writeDataSourceProperties.username)
        val password = requireNotNull(writeDataSourceProperties.password)
        val urlWithoutQuery = url.substringBefore('?')
        val serverUrl = urlWithoutQuery.substringBeforeLast('/')
        val query = url.substringAfter('?', "")
        val testDatabase = "legacy_profile_cleanup_${UUID.randomUUID().toString().filter { it != '-' }}"
        val testUrl = "$serverUrl/$testDatabase" + if (query.isEmpty()) "" else "?$query"

        DriverManager.getConnection(url, username, password).use { connection ->
            connection.createStatement().use { it.execute("CREATE DATABASE `$testDatabase` CHARACTER SET utf8mb4") }
        }
        try {
            DriverManager.getConnection(testUrl, username, password).use { connection ->
                connection.createStatement().use { statement ->
                    statement.execute("CREATE TABLE account (id BIGINT PRIMARY KEY) ENGINE=InnoDB")
                    statement.execute(
                        """CREATE TABLE business_profile (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            account_id BIGINT NOT NULL,
                            CONSTRAINT FK_business_profile_account FOREIGN KEY (account_id) REFERENCES account(id)
                        ) ENGINE=InnoDB""",
                    )
                }
            }

            Flyway.configure()
                .dataSource(testUrl, username, password)
                .locations("classpath:db/migration")
                .repeatableSqlMigrationPrefix("disabled__")
                .baselineOnMigrate(true)
                .baselineVersion(MigrationVersion.fromVersion("49"))
                .target(MigrationVersion.fromVersion("50"))
                .load()
                .migrate()

            DriverManager.getConnection(testUrl, username, password).use { connection ->
                connection.prepareStatement(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = ? AND table_name = 'business_profile'",
                ).use { statement ->
                    statement.setString(1, testDatabase)
                    statement.executeQuery().use { result ->
                        assertThat(result.next()).isTrue()
                        assertThat(result.getInt(1)).isZero()
                    }
                }
                connection.prepareStatement(
                    "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '50' AND success = 1",
                ).use { statement ->
                    statement.executeQuery().use { result ->
                        assertThat(result.next()).isTrue()
                        assertThat(result.getInt(1)).isEqualTo(1)
                    }
                }
            }
        } finally {
            DriverManager.getConnection(url, username, password).use { connection ->
                connection.createStatement().use { it.execute("DROP DATABASE IF EXISTS `$testDatabase`") }
            }
        }
    }
}
