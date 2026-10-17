package com.buyeong.umji.api.domain.shipment.integration

import org.assertj.core.api.Assertions.assertThat
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.MigrationVersion
import org.junit.jupiter.api.Test
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.nio.file.Files
import java.sql.DriverManager

@Testcontainers(disabledWithoutDocker = true)
class ShipmentRolePermissionMigrationMySqlIntegrationTest {
    @Test
    fun `v56 and v57 keep scoped assignments and remove unscoped shipping roles`() {
        val mysql = mysqlContainer
        DriverManager.getConnection(mysql.jdbcUrl, mysql.username, mysql.password).use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("CREATE TABLE role (id BIGINT PRIMARY KEY, code VARCHAR(80) NOT NULL UNIQUE, name VARCHAR(100), is_system BOOLEAN)")
                statement.execute("CREATE TABLE permission (id BIGINT AUTO_INCREMENT PRIMARY KEY, code VARCHAR(80) NOT NULL UNIQUE, name VARCHAR(100))")
                statement.execute("CREATE TABLE role_permission (role_id BIGINT NOT NULL, permission_id BIGINT NOT NULL, UNIQUE KEY UQ_role_permission (role_id, permission_id))")
                statement.execute("CREATE TABLE account (id BIGINT PRIMARY KEY, token_version BIGINT NOT NULL DEFAULT 0)")
                statement.execute("CREATE TABLE account_role (account_id BIGINT NOT NULL, role_id BIGINT NOT NULL, granted_by BIGINT NOT NULL, PRIMARY KEY (account_id, role_id))")
                statement.execute(
                    """CREATE TABLE brand_shipping_manager_assignment (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY, brand_id BIGINT NOT NULL, account_id BIGINT NOT NULL,
                        assigned_by_account_id BIGINT NOT NULL, created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                        CONSTRAINT UQ_brand_shipping_manager_brand_account UNIQUE (brand_id, account_id)
                    )""",
                )
                statement.execute(
                    "INSERT INTO role (id, code, name, is_system) VALUES (1, 'ADMIN', 'Admin', TRUE), (2, 'SUPER_ADMIN', 'Super Admin', TRUE), (3, 'SHIPPING_MANAGER', 'Shipping Manager', TRUE), (4, 'SHIPPING_OPERATOR', 'Shipping Operator', TRUE)",
                )
                statement.execute("INSERT INTO account (id) VALUES (21), (22), (23)")
                statement.execute("INSERT INTO account_role (account_id, role_id, granted_by) VALUES (21, 3, 31), (22, 3, 31), (23, 4, 31)")
                statement.execute(
                    "INSERT INTO permission (code, name) VALUES ('SHIPMENT_ASSIGN', 'Assign'), ('SHIPMENT_READ', 'Read'), ('SHIPMENT_WRITE', 'Write'), ('SHIPMENT_CONFIGURE', 'Configure')",
                )
                statement.execute("INSERT INTO brand_shipping_manager_assignment (brand_id, account_id, assigned_by_account_id) VALUES (11, 21, 31)")
            }
        }

        val migrationDirectory = Files.createTempDirectory("shipment-role-migration")
        try {
            listOf(
                "V56__separate_shipment_manager_and_operator_permissions.sql",
                "V57__remove_unscoped_shipping_roles.sql",
            ).forEach { name ->
                val destination = migrationDirectory.resolve(name)
                javaClass.getResourceAsStream("/db/migration/$name")!!.use { input -> Files.copy(input, destination) }
            }
            Flyway.configure()
                .dataSource(mysql.jdbcUrl, mysql.username, mysql.password)
                .locations("filesystem:${migrationDirectory.toAbsolutePath()}")
                .baselineOnMigrate(true)
                .baselineVersion(MigrationVersion.fromVersion("55"))
                .target(MigrationVersion.fromVersion("57"))
                .load()
                .migrate()

            DriverManager.getConnection(mysql.jdbcUrl, mysql.username, mysql.password).use { connection ->
                connection.createStatement().use { statement ->
                    statement.executeQuery("SELECT assignment_type FROM brand_shipping_assignment WHERE brand_id = 11 AND account_id = 21").use { result ->
                        assertThat(result.next()).isTrue()
                        assertThat(result.getString("assignment_type")).isEqualTo("MANAGER")
                    }
                statement.executeQuery(
                    "SELECT role.code FROM account_role JOIN role ON role.id = account_role.role_id WHERE account_role.account_id = 21",
                ).use { result ->
                    assertThat(result.next()).isTrue()
                    assertThat(result.getString(1)).isEqualTo("SHIPPING_MANAGER")
                    assertThat(result.next()).isFalse()
                }
                statement.executeQuery("SELECT token_version FROM account WHERE id IN (21, 22, 23) ORDER BY id").use { result ->
                    assertThat(result.next()).isTrue()
                    assertThat(result.getLong(1)).isZero()
                    assertThat(result.next()).isTrue()
                    assertThat(result.getLong(1)).isEqualTo(1L)
                    assertThat(result.next()).isTrue()
                    assertThat(result.getLong(1)).isEqualTo(1L)
                }
                    statement.executeQuery(
                        """SELECT role.code AS role_code, permission.code AS permission_code
                            FROM role_permission JOIN role ON role.id = role_permission.role_id
                            JOIN permission ON permission.id = role_permission.permission_id
                            WHERE role.code IN ('SHIPPING_MANAGER', 'SHIPPING_OPERATOR')""",
                    ).use { result ->
                        val grants = mutableSetOf<Pair<String, String>>()
                        while (result.next()) grants += result.getString("role_code") to result.getString("permission_code")
                        assertThat(grants).contains(
                            "SHIPPING_MANAGER" to "SHIPMENT_CONFIGURE",
                            "SHIPPING_OPERATOR" to "SHIPMENT_READ",
                            "SHIPPING_OPERATOR" to "SHIPMENT_WRITE",
                        )
                        assertThat(grants).doesNotContain("SHIPPING_OPERATOR" to "SHIPMENT_CONFIGURE")
                    }
                }
            }
        } finally {
            Files.list(migrationDirectory).use { files -> files.forEach(Files::deleteIfExists) }
            Files.deleteIfExists(migrationDirectory)
        }
    }

    private companion object {
        @Container
        @JvmStatic
        val mysqlContainer: MySQLContainer<*> = MySQLContainer(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("shipment_role_permission_test")
            .withUsername("test")
            .withPassword("test")
    }
}
