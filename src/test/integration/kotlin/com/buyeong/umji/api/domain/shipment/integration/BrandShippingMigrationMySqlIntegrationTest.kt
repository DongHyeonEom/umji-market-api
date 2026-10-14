package com.buyeong.umji.api.domain.shipment.integration

import org.assertj.core.api.Assertions.assertThat
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.MigrationVersion
import org.junit.jupiter.api.Test
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.sql.DriverManager

@Testcontainers(disabledWithoutDocker = true)
class BrandShippingMigrationMySqlIntegrationTest {
    @Test
    fun `v51 adds shipment packages and migrates existing tracking rows with item allocations`() {
        val mysql = mysqlContainer
        DriverManager.getConnection(mysql.jdbcUrl, mysql.username, mysql.password).use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("CREATE TABLE role (id BIGINT PRIMARY KEY, code VARCHAR(80) NOT NULL UNIQUE, name VARCHAR(100), is_system BOOLEAN)")
                statement.execute("CREATE TABLE permission (id BIGINT AUTO_INCREMENT PRIMARY KEY, code VARCHAR(80) NOT NULL UNIQUE, name VARCHAR(100))")
                statement.execute("CREATE TABLE role_permission (role_id BIGINT NOT NULL, permission_id BIGINT NOT NULL, UNIQUE KEY UQ_role_permission (role_id, permission_id))")
                statement.execute("CREATE TABLE account (id BIGINT PRIMARY KEY)")
                statement.execute("CREATE TABLE brand (id BIGINT PRIMARY KEY)")
                statement.execute("CREATE TABLE product (id BIGINT PRIMARY KEY, brand_id BIGINT NULL)")
                statement.execute("CREATE TABLE product_sku (id BIGINT PRIMARY KEY, product_id BIGINT NOT NULL)")
                statement.execute("CREATE TABLE purchase_order (id BIGINT PRIMARY KEY)")
                statement.execute("CREATE TABLE order_item (id BIGINT PRIMARY KEY, order_id BIGINT NOT NULL, sku_id BIGINT NOT NULL, quantity INT NOT NULL)")
                statement.execute(
                    """CREATE TABLE order_shipment (
                        id BIGINT PRIMARY KEY, order_id BIGINT NOT NULL UNIQUE, status VARCHAR(40) NOT NULL,
                        carrier_code VARCHAR(80), tracking_number VARCHAR(100), processed_by BIGINT,
                        created_at DATETIME(3) NOT NULL, updated_at DATETIME(3) NOT NULL
                    )""",
                )
                statement.execute("INSERT INTO role (id, code, name, is_system) VALUES (1, 'ADMIN', 'Admin', TRUE), (2, 'SUPER_ADMIN', 'Super Admin', TRUE)")
                statement.execute("INSERT INTO account (id) VALUES (10), (11)")
                statement.execute("INSERT INTO brand (id) VALUES (20)")
                statement.execute("INSERT INTO product (id, brand_id) VALUES (30, 20)")
                statement.execute("INSERT INTO product_sku (id, product_id) VALUES (40, 30)")
                statement.execute("INSERT INTO purchase_order (id) VALUES (50)")
                statement.execute("INSERT INTO order_item (id, order_id, sku_id, quantity) VALUES (60, 50, 40, 3)")
                statement.execute("INSERT INTO order_shipment (id, order_id, status, carrier_code, tracking_number, processed_by, created_at, updated_at) VALUES (70, 50, 'IN_TRANSIT', 'DAESIN', 'TRACK-OLD', 10, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3))")
            }
        }

        Flyway.configure()
            .dataSource(mysql.jdbcUrl, mysql.username, mysql.password)
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .baselineVersion(MigrationVersion.fromVersion("50"))
            .target(MigrationVersion.fromVersion("51"))
            .load()
            .migrate()

        DriverManager.getConnection(mysql.jdbcUrl, mysql.username, mysql.password).use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery(
                    "SELECT package.brand_id, package.status, item.box_quantity FROM order_shipment_package package JOIN order_shipment_package_item item ON item.package_id = package.id WHERE package.tracking_number = 'TRACK-OLD'",
                ).use { result ->
                    assertThat(result.next()).isTrue()
                    assertThat(result.getLong("brand_id")).isEqualTo(20)
                    assertThat(result.getString("status")).isEqualTo("IN_TRANSIT")
                    assertThat(result.getInt("box_quantity")).isEqualTo(3)
                    assertThat(result.next()).isFalse()
                }
                statement.executeQuery("SELECT carrier_code, tracking_number FROM order_shipment WHERE id = 70").use { result ->
                    assertThat(result.next()).isTrue()
                    assertThat(result.getString("carrier_code")).isNull()
                    assertThat(result.getString("tracking_number")).isNull()
                }
            }
        }
    }

    private companion object {
        @Container
        @JvmStatic
        val mysqlContainer: MySQLContainer<*> = MySQLContainer(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("brand_shipping_migration_test")
            .withUsername("test")
            .withPassword("test")
    }
}
