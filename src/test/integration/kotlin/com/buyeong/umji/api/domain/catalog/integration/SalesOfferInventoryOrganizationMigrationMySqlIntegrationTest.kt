package com.buyeong.umji.api.domain.catalog.integration

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.MigrationVersion
import org.junit.jupiter.api.Test
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.sql.DriverManager

@Testcontainers(disabledWithoutDocker = true)
class SalesOfferInventoryOrganizationMigrationMySqlIntegrationTest {
    @Test
    fun `flyway v42 preserves legacy offer and inventory rows and scopes uniqueness`() {
        val mysql = mysqlContainer
        DriverManager.getConnection(mysql.jdbcUrl, mysql.username, mysql.password).use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("CREATE TABLE organization (id BIGINT PRIMARY KEY) ENGINE=InnoDB")
                statement.execute("CREATE TABLE product_sku (id BIGINT PRIMARY KEY) ENGINE=InnoDB")
                statement.execute("CREATE TABLE sales_channel (id BIGINT PRIMARY KEY) ENGINE=InnoDB")
                statement.execute("CREATE TABLE cart (id BIGINT PRIMARY KEY) ENGINE=InnoDB")
                statement.execute(
                    """CREATE TABLE sales_offer (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        public_id BINARY(16) NOT NULL,
                        sales_channel_id BIGINT NOT NULL,
                        product_sku_id BIGINT NOT NULL,
                        sale_price BIGINT NOT NULL,
                        list_price BIGINT NULL,
                        sales_status VARCHAR(30) NOT NULL,
                        CONSTRAINT UQ_sales_offer_public_id UNIQUE (public_id),
                        CONSTRAINT UQ_sales_offer_channel_sku UNIQUE (sales_channel_id, product_sku_id),
                        CONSTRAINT FK_sales_offer_channel FOREIGN KEY (sales_channel_id) REFERENCES sales_channel(id),
                        CONSTRAINT FK_sales_offer_sku FOREIGN KEY (product_sku_id) REFERENCES product_sku(id)
                    ) ENGINE=InnoDB""",
                )
                statement.execute(
                    """CREATE TABLE cart_item (
                        id BIGINT PRIMARY KEY,
                        cart_id BIGINT NOT NULL,
                        sku_id BIGINT NOT NULL,
                        sales_offer_id BIGINT NOT NULL,
                        CONSTRAINT UQ_cart_item_sku UNIQUE (cart_id, sku_id),
                        CONSTRAINT FK_cart_item_cart FOREIGN KEY (cart_id) REFERENCES cart(id)
                    ) ENGINE=InnoDB""",
                )
                statement.execute(
                    """CREATE TABLE inventory_stock (
                        id BIGINT PRIMARY KEY,
                        sku_id BIGINT NOT NULL,
                        on_hand_quantity INT NOT NULL,
                        reserved_quantity INT NOT NULL,
                        safety_stock_quantity INT NOT NULL,
                        version BIGINT NOT NULL,
                        CONSTRAINT UQ_inventory_stock_sku UNIQUE (sku_id),
                        CONSTRAINT FK_inventory_stock_sku FOREIGN KEY (sku_id) REFERENCES product_sku(id)
                    ) ENGINE=InnoDB""",
                )
                statement.execute(
                    """CREATE TABLE inventory_movement (
                        id BIGINT PRIMARY KEY,
                        sku_id BIGINT NOT NULL,
                        occurred_at DATETIME(3) NOT NULL,
                        CONSTRAINT FK_inventory_movement_sku FOREIGN KEY (sku_id) REFERENCES product_sku(id),
                        INDEX IX_inventory_movement_sku_occurred_at (sku_id, occurred_at DESC)
                    ) ENGINE=InnoDB""",
                )
                statement.execute(
                    """CREATE TABLE stock_reservation (
                        id BIGINT PRIMARY KEY,
                        reservation_key BINARY(16) NOT NULL,
                        sku_id BIGINT NOT NULL,
                        quantity INT NOT NULL,
                        status VARCHAR(30) NOT NULL,
                        CONSTRAINT UQ_stock_reservation_key UNIQUE (reservation_key),
                        CONSTRAINT FK_stock_reservation_sku FOREIGN KEY (sku_id) REFERENCES product_sku(id),
                        INDEX IX_stock_reservation_status_expires_at (status)
                    ) ENGINE=InnoDB""",
                )
                statement.execute("INSERT INTO organization (id) VALUES (1), (2)")
                statement.execute("INSERT INTO product_sku (id) VALUES (100), (101)")
                statement.execute("INSERT INTO sales_channel (id) VALUES (1), (2)")
                statement.execute("INSERT INTO cart (id) VALUES (1)")
                statement.execute(
                    """INSERT INTO sales_offer (id, public_id, sales_channel_id, product_sku_id, sale_price, sales_status)
                        VALUES (10, UNHEX(REPLACE(UUID(), '-', '')), 1, 100, 1200, 'ON_SALE')""",
                )
                statement.execute("INSERT INTO cart_item (id, cart_id, sku_id, sales_offer_id) VALUES (20, 1, 100, 10)")
                statement.execute("INSERT INTO inventory_stock VALUES (30, 100, 12, 2, 1, 0)")
                statement.execute("INSERT INTO inventory_movement (id, sku_id, occurred_at) VALUES (40, 100, CURRENT_TIMESTAMP(3))")
                statement.execute(
                    """INSERT INTO stock_reservation (id, reservation_key, sku_id, quantity, status)
                        VALUES (50, UNHEX(REPLACE(UUID(), '-', '')), 100, 2, 'RESERVED')""",
                )
            }
        }

        Flyway.configure()
            .dataSource(mysql.jdbcUrl, mysql.username, mysql.password)
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .baselineVersion(MigrationVersion.fromVersion("41"))
            .target(MigrationVersion.fromVersion("42"))
            .load()
            .migrate()

        DriverManager.getConnection(mysql.jdbcUrl, mysql.username, mysql.password).use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT organization_id, sales_channel_id, product_sku_id FROM sales_offer WHERE id = 10").use { result ->
                    assertThat(result.next()).isTrue()
                    assertThat(result.getObject("organization_id")).isNull()
                    assertThat(result.getLong("sales_channel_id")).isEqualTo(1)
                    assertThat(result.getLong("product_sku_id")).isEqualTo(100)
                }
                statement.executeQuery("SELECT organization_id, on_hand_quantity, reserved_quantity FROM inventory_stock WHERE id = 30").use { result ->
                    assertThat(result.next()).isTrue()
                    assertThat(result.getObject("organization_id")).isNull()
                    assertThat(result.getInt("on_hand_quantity")).isEqualTo(12)
                    assertThat(result.getInt("reserved_quantity")).isEqualTo(2)
                }
                statement.executeQuery(
                    """SELECT (SELECT organization_id FROM inventory_movement WHERE id = 40) AS movement_organization_id,
                        (SELECT organization_id FROM stock_reservation WHERE id = 50) AS reservation_organization_id,
                        (SELECT sales_offer_id FROM cart_item WHERE id = 20) AS cart_offer_id""",
                ).use { result ->
                    assertThat(result.next()).isTrue()
                    assertThat(result.getObject("movement_organization_id")).isNull()
                    assertThat(result.getObject("reservation_organization_id")).isNull()
                    assertThat(result.getLong("cart_offer_id")).isEqualTo(10)
                }

                statement.execute(
                    """INSERT INTO sales_offer (public_id, sales_channel_id, product_sku_id, sale_price, sales_status, organization_id)
                        VALUES (UNHEX(REPLACE(UUID(), '-', '')), 1, 100, 1200, 'ON_SALE', 1),
                               (UNHEX(REPLACE(UUID(), '-', '')), 1, 100, 1300, 'ON_SALE', 2),
                               (UNHEX(REPLACE(UUID(), '-', '')), 2, 100, 1400, 'ON_SALE', 1)""",
                )
                statement.execute("INSERT INTO inventory_stock VALUES (31, 1, 100, 5, 0, 0), (32, 2, 100, 8, 1, 0)")
                statement.execute("INSERT INTO inventory_stock VALUES (33, NULL, 100, 4, 0, 0)")
                statement.execute(
                    """INSERT INTO inventory_movement (id, sku_id, occurred_at, organization_id)
                        VALUES (41, 100, CURRENT_TIMESTAMP(3), 1), (42, 100, CURRENT_TIMESTAMP(3), 2)""",
                )
                statement.execute(
                    """INSERT INTO stock_reservation (id, reservation_key, sku_id, quantity, status, organization_id)
                        VALUES (51, UNHEX(REPLACE(UUID(), '-', '')), 100, 2, 'RESERVED', 1),
                               (52, UNHEX(REPLACE(UUID(), '-', '')), 100, 3, 'RESERVED', 2)""",
                )

                assertThatThrownBy {
                    statement.execute(
                        """INSERT INTO sales_offer (public_id, sales_channel_id, product_sku_id, sale_price, sales_status, organization_id)
                            VALUES (UNHEX(REPLACE(UUID(), '-', '')), 1, 100, 1500, 'ON_SALE', 1)""",
                    )
                }.isInstanceOf(Exception::class.java)
                assertThatThrownBy {
                    statement.execute("INSERT INTO inventory_stock VALUES (34, 1, 100, 1, 0, 0)")
                }.isInstanceOf(Exception::class.java)
            }
        }
    }

    private companion object {
        @Container
        @JvmStatic
        val mysqlContainer: MySQLContainer<*> =
            MySQLContainer(DockerImageName.parse("mysql:8.0"))
                .withDatabaseName("seller_offer_inventory_migration_test")
                .withUsername("test")
                .withPassword("test")
    }
}