package com.buyeong.umji.api.domain.account.integration

import org.assertj.core.api.Assertions.assertThat
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.MigrationVersion
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.nio.ByteBuffer
import java.sql.DriverManager
import java.util.UUID

@SpringBootTest
@ActiveProfiles("local")
class OrganizationConversionMigrationMySqlIntegrationTest {
    @Autowired
    @Qualifier("defaultWriteDataSourceProperties")
    private lateinit var writeDataSourceProperties: DataSourceProperties

    @Test
    fun `flyway v38 keeps account business profile and order organization relationships`() {
        val url = requireNotNull(writeDataSourceProperties.url)
        val username = requireNotNull(writeDataSourceProperties.username)
        val password = requireNotNull(writeDataSourceProperties.password)
        val urlWithoutQuery = url.substringBefore('?')
        val serverUrl = urlWithoutQuery.substringBeforeLast('/')
        val query = url.substringAfter('?', "")
        val testDatabase = "organization_conversion_test_${UUID.randomUUID().toString().replace("-", "")}"
        val testUrl = "$serverUrl/$testDatabase" + if (query.isEmpty()) "" else "?$query"
        val businessPublicId = UUID.randomUUID()
        val individualPublicId = UUID.randomUUID()

        DriverManager.getConnection(url, username, password).use { connection ->
            connection.createStatement().use { it.execute("CREATE DATABASE `$testDatabase` CHARACTER SET utf8mb4") }
        }
        try {
            migrate(testUrl, username, password, "17")
            DriverManager.getConnection(testUrl, username, password).use { connection ->
                connection.prepareStatement(
                    "INSERT INTO account (public_id, name, status, default_tax_invoice_requested) VALUES (?, ?, 'ACTIVE', TRUE), (?, ?, 'ACTIVE', FALSE)",
                ).use { statement ->
                    statement.setBytes(1, businessPublicId.bytes())
                    statement.setString(2, "Business Buyer")
                    statement.setBytes(3, individualPublicId.bytes())
                    statement.setString(4, "Individual Buyer")
                    statement.executeUpdate()
                }
                connection.createStatement().use { statement ->
                    statement.executeUpdate(
                        """INSERT INTO business_profile (account_id, business_name, business_registration_number,
                            representative_name, business_phone, postal_code, address1, address2, status)
                            VALUES (1, 'Legacy Business', '1234567890', 'Buyer Rep', '0212345678', '01234', 'Seoul Road', '2F', 'ACTIVE')""",
                    )
                    statement.executeUpdate(
                        """INSERT INTO purchase_order (public_id, order_number, account_id, status, subtotal_amount,
                            total_amount, ordered_at) VALUES (UNHEX(REPLACE(UUID(), '-', '')), 'ORG-MIGRATION-ORDER',
                            1, 'PENDING_PAYMENT', 1200, 1200, CURRENT_TIMESTAMP(3))""",
                    )
                }
            }

            migrate(testUrl, username, password, "37")
            DriverManager.getConnection(testUrl, username, password).use { connection ->
                connection.prepareStatement(
                    """INSERT INTO buyer_group_address (public_id, buyer_group_id, created_by_account_id,
                        recipient_name, recipient_phone, postal_code, address1, address2, is_default)
                        SELECT ?, group_record.id, 1, 'Buyer Rep', '0212345678', '01234', 'Shipping Road', '3F', TRUE
                        FROM buyer_group group_record WHERE group_record.display_name = 'Legacy Business'""",
                ).use { statement ->
                    statement.setBytes(1, UUID.randomUUID().bytes())
                    assertThat(statement.executeUpdate()).isEqualTo(1)
                }
            }
            migrate(testUrl, username, password, "40")

            DriverManager.getConnection(testUrl, username, password).use { connection ->
                connection.prepareStatement(
                    """SELECT o.organization_type, o.display_name, profile.business_registration_number,
                            profile.representative_name, profile.business_phone, profile.postal_code, profile.address1,
                            profile.address2, member.status, capability.capability_code, o.default_tax_invoice_requested
                            FROM organization o
                            JOIN organization_member member ON member.organization_id = o.id
                            JOIN account a ON a.id = member.account_id
                            JOIN organization_capability capability ON capability.organization_id = o.id
                            LEFT JOIN organization_business_profile profile ON profile.organization_id = o.id
                            WHERE a.public_id = ?""",
                ).use { queryStatement ->
                    queryStatement.setBytes(1, businessPublicId.bytes())
                    queryStatement.executeQuery().use { result ->
                        assertThat(result.next()).isTrue()
                        assertThat(result.getString("organization_type")).isEqualTo("BUSINESS")
                        assertThat(result.getString("display_name")).isEqualTo("Legacy Business")
                        assertThat(result.getString("business_registration_number")).isEqualTo("1234567890")
                        assertThat(result.getString("representative_name")).isEqualTo("Buyer Rep")
                        assertThat(result.getString("business_phone")).isEqualTo("0212345678")
                        assertThat(result.getString("postal_code")).isEqualTo("01234")
                        assertThat(result.getString("address1")).isEqualTo("Seoul Road")
                        assertThat(result.getString("address2")).isEqualTo("2F")
                        assertThat(result.getString("status")).isEqualTo("ACTIVE")
                        assertThat(result.getString("capability_code")).isEqualTo("BUYER")
                        assertThat(result.getBoolean("default_tax_invoice_requested")).isTrue()
                        assertThat(result.next()).isFalse()
                    }
                }
                connection.prepareStatement(
                    """SELECT o.organization_type, o.display_name, member.status, capability.capability_code,
                            o.default_tax_invoice_requested
                            FROM organization o
                            JOIN organization_member member ON member.organization_id = o.id
                            JOIN account a ON a.id = member.account_id
                            JOIN organization_capability capability ON capability.organization_id = o.id
                            WHERE a.public_id = ?""",
                ).use { queryStatement ->
                    queryStatement.setBytes(1, individualPublicId.bytes())
                    queryStatement.executeQuery().use { result ->
                        assertThat(result.next()).isTrue()
                        assertThat(result.getString("organization_type")).isEqualTo("INDIVIDUAL")
                        assertThat(result.getString("display_name")).isEqualTo("Individual Buyer")
                        assertThat(result.getString("status")).isEqualTo("ACTIVE")
                        assertThat(result.getString("capability_code")).isEqualTo("BUYER")
                        assertThat(result.getBoolean("default_tax_invoice_requested")).isFalse()
                        assertThat(result.next()).isFalse()
                    }
                }
                connection.prepareStatement(
                    """SELECT COUNT(*) FROM purchase_order o
                            JOIN organization organization ON organization.id = o.organization_id
                            JOIN organization_member member ON member.organization_id = organization.id
                            JOIN account a ON a.id = member.account_id
                            WHERE o.order_number = 'ORG-MIGRATION-ORDER' AND a.public_id = ? AND member.status = 'ACTIVE'""",
                ).use { queryStatement ->
                    queryStatement.setBytes(1, businessPublicId.bytes())
                    queryStatement.executeQuery().use { result ->
                        assertThat(result.next()).isTrue()
                        assertThat(result.getInt(1)).isEqualTo(1)
                    }
                }
                connection.prepareStatement(
                    """SELECT address.recipient_name, address.postal_code, address.address1, address.address2,
                        address.is_default FROM organization_address address
                        JOIN organization organization ON organization.id = address.organization_id
                        WHERE organization.display_name = 'Legacy Business'""",
                ).use { queryStatement ->
                    queryStatement.executeQuery().use { result ->
                        assertThat(result.next()).isTrue()
                        assertThat(result.getString("recipient_name")).isEqualTo("Buyer Rep")
                        assertThat(result.getString("postal_code")).isEqualTo("01234")
                        assertThat(result.getString("address1")).isEqualTo("Shipping Road")
                        assertThat(result.getString("address2")).isEqualTo("3F")
                        assertThat(result.getBoolean("is_default")).isTrue()
                        assertThat(result.next()).isFalse()
                    }
                }
            }
        } finally {
            DriverManager.getConnection(url, username, password).use { connection ->
                connection.createStatement().use { it.execute("DROP DATABASE IF EXISTS `$testDatabase`") }
            }
        }
    }

    private fun migrate(url: String, username: String, password: String, target: String) {
        Flyway.configure()
            .dataSource(url, username, password)
            .locations("classpath:db/migration")
            .target(MigrationVersion.fromVersion(target))
            .load()
            .migrate()
    }

    private fun UUID.bytes(): ByteArray = ByteBuffer.allocate(16)
        .putLong(mostSignificantBits)
        .putLong(leastSignificantBits)
        .array()
}