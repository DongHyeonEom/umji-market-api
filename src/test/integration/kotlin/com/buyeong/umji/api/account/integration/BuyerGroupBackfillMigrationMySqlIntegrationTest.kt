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
class BuyerGroupBackfillMigrationMySqlIntegrationTest {
    @Autowired
    @Qualifier("defaultWriteDataSourceProperties")
    private lateinit var writeDataSourceProperties: DataSourceProperties

    @Test
    fun `flyway v18 backfills individual and business groups and existing order ownership`() {
        val url = requireNotNull(writeDataSourceProperties.url)
        val username = requireNotNull(writeDataSourceProperties.username)
        val password = requireNotNull(writeDataSourceProperties.password)
        val urlWithoutQuery = url.substringBefore('?')
        val serverUrl = urlWithoutQuery.substringBeforeLast('/')
        val query = url.substringAfter('?', "")
        val testDatabase = "buyer_group_migration_test_${UUID.randomUUID().toString().filter { it != '-' }}"
        val testUrl = "$serverUrl/$testDatabase" + if (query.isEmpty()) "" else "?$query"

        DriverManager.getConnection(url, username, password).use { connection ->
            connection.createStatement().use { it.execute("CREATE DATABASE `$testDatabase` CHARACTER SET utf8mb4") }
        }
        try {
            DriverManager.getConnection(testUrl, username, password).use { connection ->
                connection.createStatement().use { statement ->
                    statement.execute(
                        """CREATE TABLE account (
                            id BIGINT PRIMARY KEY,
                            name VARCHAR(200) NOT NULL
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""",
                    )
                    statement.execute(
                        """CREATE TABLE business_profile (
                            id BIGINT PRIMARY KEY,
                            account_id BIGINT NOT NULL,
                            business_name VARCHAR(200) NOT NULL,
                            business_registration_number VARCHAR(30) NULL,
                            representative_name VARCHAR(100) NULL,
                            business_phone VARCHAR(30) NULL,
                            postal_code VARCHAR(20) NULL,
                            address1 VARCHAR(255) NULL,
                            address2 VARCHAR(255) NULL,
                            status VARCHAR(30) NOT NULL,
                            created_at DATETIME(3) NOT NULL,
                            updated_at DATETIME(3) NOT NULL
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""",
                    )
                    statement.execute(
                        """CREATE TABLE purchase_order (
                            id BIGINT PRIMARY KEY,
                            account_id BIGINT NOT NULL,
                            ordered_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                            CONSTRAINT FK_purchase_order_account FOREIGN KEY (account_id) REFERENCES account(id)
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""",
                    )
                    statement.execute("INSERT INTO account (id, name) VALUES (1, 'Individual Buyer'), (2, 'Business Buyer')")
                    statement.execute(
                        """INSERT INTO business_profile (
                            id, account_id, business_name, business_registration_number, status, created_at, updated_at
                        ) VALUES (1, 2, 'Business Group', NULL, 'ACTIVE', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3))""",
                    )
                    statement.execute("INSERT INTO purchase_order (id, account_id) VALUES (101, 1), (102, 2)")
                }
            }

            Flyway.configure()
                .dataSource(testUrl, username, password)
                .locations("classpath:db/migration")
                .repeatableSqlMigrationPrefix("disabled__")
                .baselineOnMigrate(true)
                .baselineVersion(MigrationVersion.fromVersion("17"))
                .target(MigrationVersion.fromVersion("18"))
                .load()
                .migrate()

            DriverManager.getConnection(testUrl, username, password).use { connection ->
                connection.createStatement().use { statement ->
                    statement.executeQuery(
                        """SELECT COUNT(*) FROM purchase_order o
                            JOIN buyer_group_member member ON member.account_id = o.account_id
                                AND member.buyer_group_id = o.buyer_group_id AND member.status = 'ACTIVE'""",
                    ).use { result ->
                        assertThat(result.next()).isTrue()
                        assertThat(result.getInt(1)).isEqualTo(2)
                    }
                    statement.executeQuery(
                        """SELECT COUNT(*) FROM buyer_group
                            WHERE (group_type = 'INDIVIDUAL' AND display_name = 'Individual Buyer')
                               OR (group_type = 'BUSINESS' AND display_name = 'Business Group')""",
                    ).use { result ->
                        assertThat(result.next()).isTrue()
                        assertThat(result.getInt(1)).isEqualTo(2)
                    }
                    statement.executeQuery(
                        """SELECT business_registration_number FROM buyer_group_business_profile
                            WHERE business_name = 'Business Group'""",
                    ).use { result ->
                        assertThat(result.next()).isTrue()
                        assertThat(result.getString(1)).isNull()
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