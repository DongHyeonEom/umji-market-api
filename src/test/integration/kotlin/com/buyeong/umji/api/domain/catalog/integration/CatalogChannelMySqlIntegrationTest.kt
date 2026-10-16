package com.buyeong.umji.api.domain.catalog.integration

import com.buyeong.umji.api.domain.catalog.service.CatalogService
import com.buyeong.umji.api.domain.operation.catalog.dto.SalesOfferCommandDto
import com.buyeong.umji.api.domain.operation.catalog.service.OperationCatalogService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.Rollback
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.nio.ByteBuffer
import java.util.UUID

@SpringBootTest(properties = ["spring.flyway.enabled=true", "spring.jpa.hibernate.ddl-auto=none"])
@ActiveProfiles("local")
@Transactional
@Rollback
class CatalogChannelMySqlIntegrationTest {
    @Autowired
    private lateinit var jdbc: JdbcTemplate

    @Autowired
    private lateinit var catalog: CatalogService

    @Autowired
    private lateinit var operationCatalog: OperationCatalogService

    @Test
    fun `active buyers read one catalog with retail and wholesale sku prices`() {
        val buyerAccountId = UUID.randomUUID()
        jdbc.update(
            "INSERT INTO account (public_id, login_id, password_hash, name, status) VALUES (?, ?, 'test-hash', 'Catalog Buyer', 'ACTIVE')",
            buyerAccountId.bytes(),
            "catalog-$buyerAccountId",
        )
        val accountInternalId = jdbc.queryForObject(
            "SELECT id FROM account WHERE public_id = ?",
            Long::class.java,
            buyerAccountId.bytes(),
        )!!
        val organizationId = UUID.randomUUID()
        jdbc.update(
            "INSERT INTO organization (public_id, organization_type, display_name, status, representative_account_id) VALUES (?, 'BUSINESS', 'Catalog Buyer', 'ACTIVE', ?)",
            organizationId.bytes(),
            accountInternalId,
        )
        val organizationInternalId = jdbc.queryForObject(
            "SELECT id FROM organization WHERE public_id = ?",
            Long::class.java,
            organizationId.bytes(),
        )!!
        jdbc.update(
            "INSERT INTO organization_member (organization_id, account_id, status) VALUES (?, ?, 'ACTIVE')",
            organizationInternalId,
            accountInternalId,
        )
        jdbc.update(
            "INSERT INTO organization_capability (organization_id, capability_code) VALUES (?, 'BUYER')",
            organizationInternalId,
        )
        val wholesaleCategory = createCategory("WHOLESALE", "Wholesale test")
        val retailCategory = createCategory("RETAIL", "Retail test")
        val productId = UUID.randomUUID()
        jdbc.update(
            "INSERT INTO product (public_id, category_id, name, display_status, sales_status) VALUES (?, ?, 'Shared test product', 'DISPLAYED', 'ON_SALE')",
            productId.bytes(),
            wholesaleCategory,
        )
        val productInternalId = jdbc.queryForObject(
            "SELECT id FROM product WHERE public_id = ?",
            Long::class.java,
            productId.bytes(),
        )!!
        val skuId = UUID.randomUUID()
        jdbc.update(
            "INSERT INTO product_sku (public_id, product_id, sku_code, name, sale_price, sales_status) VALUES (?, ?, ?, 'Shared SKU', 111, 'ON_SALE')",
            skuId.bytes(),
            productInternalId,
            "CHANNEL-${UUID.randomUUID()}",
        )
        val skuInternalId = jdbc.queryForObject(
            "SELECT id FROM product_sku WHERE public_id = ?",
            Long::class.java,
            skuId.bytes(),
        )!!
        listOf("WHOLESALE" to 1000L, "RETAIL" to 1500L).forEach { (code, price) ->
            val channelId = channelId(code)
            val categoryId = if (code == "WHOLESALE") wholesaleCategory else retailCategory
            jdbc.update(
                "INSERT INTO channel_product_listing (public_id, sales_channel_id, product_id, category_id, display_status) VALUES (?, ?, ?, ?, 'DISPLAYED')",
                UUID.randomUUID().bytes(),
                channelId,
                productInternalId,
                categoryId,
            )
            jdbc.update(
                "INSERT INTO sales_offer (public_id, sales_channel_id, product_sku_id, sale_price, retail_sale_price, wholesale_sale_price, sales_status) VALUES (?, ?, ?, ?, 1500, 1000, 'ON_SALE')",
                UUID.randomUUID().bytes(),
                channelId,
                skuInternalId,
                price,
            )
        }
        operationCatalog.updateSalesOffer(SalesOfferCommandDto("WHOLESALE", skuId, 1000, null, "ON_SALE", 12))
        operationCatalog.updateSalesOffer(SalesOfferCommandDto("WHOLESALE", skuId, 1000, null, "ON_SALE"))
        assertThat(catalog.categories(buyerAccountId, "RETAIL").map { it.name })
            .contains("Wholesale test")
            .doesNotContain("Retail test")
        val wholesale = catalog.product(productId, buyerAccountId, "RETAIL")
        assertThat(wholesale.skus.single().salePrice).isEqualTo(1500L)
        assertThat(wholesale.skus.single().retailSalePrice).isEqualTo(1500L)
        assertThat(wholesale.skus.single().wholesaleSalePrice).isEqualTo(1000L)
        assertThat(wholesale.skus.single().salesOfferId).isNotNull()
        assertThat(wholesale.skus.single().unitsPerSale).isEqualTo(12)
        assertThat(catalog.products(0, 100, buyerAccountId, "WHOLESALE").items.first { it.id == productId }.startingUnitsPerSale).isEqualTo(12)
    }

    private fun createCategory(channelCode: String, name: String): Long {
        val categoryId = UUID.randomUUID()
        jdbc.update(
            "INSERT INTO category (public_id, sales_channel_id, name, path, depth, display_status) VALUES (?, ?, ?, ?, 0, 'DISPLAYED')",
            categoryId.bytes(),
            channelId(channelCode),
            name,
            "${channelCode.lowercase()}-${UUID.randomUUID()}",
        )
        return jdbc.queryForObject(
            "SELECT id FROM category WHERE public_id = ?",
            Long::class.java,
            categoryId.bytes(),
        )!!
    }

    private fun channelId(code: String) = jdbc.queryForObject(
        "SELECT id FROM sales_channel WHERE code = ?",
        Long::class.java,
        code,
    )!!

    private fun UUID.bytes(): ByteArray = ByteBuffer.allocate(16).putLong(mostSignificantBits).putLong(leastSignificantBits).array()
}