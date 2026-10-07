package com.buyeong.umji.api.catalog.integration

import java.sql.DriverManager
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.MigrationVersion
import org.junit.jupiter.api.Test
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@Testcontainers(disabledWithoutDocker = true)
class CatalogOwnershipMigrationMySqlIntegrationTest {
    @Test
    fun `flyway v43 preserves legacy catalog rows and scopes catalog uniqueness`() {
        val mysql = mysqlContainer
        DriverManager.getConnection(mysql.jdbcUrl, mysql.username, mysql.password).use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("CREATE TABLE organization (id BIGINT PRIMARY KEY) ENGINE=InnoDB")
                statement.execute(
                    """CREATE TABLE brand (
                        id BIGINT PRIMARY KEY,
                        public_id BINARY(16) NOT NULL,
                        name VARCHAR(200) NOT NULL,
                        deleted_at DATETIME(3) NULL,
                        CONSTRAINT UQ_brand_name UNIQUE (name)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""",
                )
                statement.execute(
                    """CREATE TABLE product (
                        id BIGINT PRIMARY KEY,
                        public_id BINARY(16) NOT NULL,
                        category_id BIGINT NOT NULL,
                        deleted_at DATETIME(3) NULL
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""",
                )
                statement.execute(
                    """CREATE TABLE product_sku (
                        id BIGINT PRIMARY KEY,
                        product_id BIGINT NOT NULL,
                        sku_code VARCHAR(100) NOT NULL,
                        CONSTRAINT UQ_product_sku_code UNIQUE (sku_code)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""",
                )
                statement.execute("INSERT INTO organization (id) VALUES (1), (2)")
                statement.execute("INSERT INTO brand (id, public_id, name) VALUES (10, UNHEX(REPLACE(UUID(), '-', '')), 'Legacy brand')")
                statement.execute(
                    "INSERT INTO product (id, public_id, category_id) VALUES (20, UNHEX(REPLACE(UUID(), '-', '')), 1)",
                )
                statement.execute("INSERT INTO product_sku (id, product_id, sku_code) VALUES (30, 20, 'LEGACY-SKU')")
            }
        }

        Flyway.configure()
            .dataSource(mysql.jdbcUrl, mysql.username, mysql.password)
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .baselineVersion(MigrationVersion.fromVersion("42"))
            .target(MigrationVersion.fromVersion("43"))
            .load()
            .migrate()

        DriverManager.getConnection(mysql.jdbcUrl, mysql.username, mysql.password).use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT organization_id FROM brand WHERE id = 10").use { result ->
                    assertThat(result.next()).isTrue()
                    assertThat(result.getObject(1)).isNull()
                }
                statement.executeQuery("SELECT organization_id FROM product WHERE id = 20").use { result ->
                    assertThat(result.next()).isTrue()
                    assertThat(result.getObject(1)).isNull()
                }
                statement.executeQuery("SELECT sku_code FROM product_sku WHERE id = 30 AND product_id = 20").use { result ->
                    assertThat(result.next()).isTrue()
                    assertThat(result.getString(1)).isEqualTo("LEGACY-SKU")
                }
                statement.execute("INSERT INTO brand (id, public_id, organization_id, name) VALUES (11, UNHEX(REPLACE(UUID(), '-', '')), 1, 'Shared name')")
                statement.execute("INSERT INTO brand (id, public_id, organization_id, name) VALUES (12, UNHEX(REPLACE(UUID(), '-', '')), 2, 'Shared name')")
                statement.execute("INSERT INTO brand (id, public_id, organization_id, name) VALUES (13, UNHEX(REPLACE(UUID(), '-', '')), NULL, 'Legacy brand')")
                statement.execute("INSERT INTO product (id, public_id, organization_id, category_id) VALUES (21, UNHEX(REPLACE(UUID(), '-', '')), 1, 1), (22, UNHEX(REPLACE(UUID(), '-', '')), 2, 1)")
                statement.execute("INSERT INTO product_sku (id, product_id, sku_code) VALUES (31, 21, 'SHARED-SKU'), (32, 22, 'SHARED-SKU')")

                assertThatThrownBy {
                    statement.execute("INSERT INTO brand (id, public_id, organization_id, name) VALUES (14, UNHEX(REPLACE(UUID(), '-', '')), 1, 'Shared name')")
                }.isInstanceOf(Exception::class.java)
                assertThatThrownBy {
                    statement.execute("INSERT INTO product_sku (id, product_id, sku_code) VALUES (33, 21, 'SHARED-SKU')")
                }.isInstanceOf(Exception::class.java)
            }
        }
    }

    private companion object {
        @Container
        @JvmStatic
        val mysqlContainer: MySQLContainer<*> =
            MySQLContainer(DockerImageName.parse("mysql:8.0"))
                .withDatabaseName("catalog_ownership_migration_test")
                .withUsername("test")
                .withPassword("test")
    }
}
