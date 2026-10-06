package com.buyeong.umji.api.catalog.integration

import com.buyeong.umji.api.catalog.service.CatalogService
import com.buyeong.umji.api.operation.catalog.model.SalesOfferCommand
import com.buyeong.umji.api.operation.catalog.service.OperationCatalogService
import java.nio.ByteBuffer
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.Rollback
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

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
    fun `categories listings and offer prices are isolated by channel while sku is shared`() {
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
                "INSERT INTO sales_offer (public_id, sales_channel_id, product_sku_id, sale_price, sales_status) VALUES (?, ?, ?, ?, 'ON_SALE')",
                UUID.randomUUID().bytes(),
                channelId,
                skuInternalId,
                price,
            )
        }
        operationCatalog.updateSalesOffer(SalesOfferCommand("WHOLESALE", skuId, 1000, null, "ON_SALE", 12))
        operationCatalog.updateSalesOffer(SalesOfferCommand("WHOLESALE", skuId, 1000, null, "ON_SALE"))
        assertThatThrownBy {
            operationCatalog.updateSalesOffer(SalesOfferCommand("RETAIL", skuId, 1500, null, "ON_SALE", 2))
        }.isInstanceOf(IllegalArgumentException::class.java)

        assertThat(catalog.categories("WHOLESALE").map { it.name })
            .contains("Wholesale test")
            .doesNotContain("Retail test")
        assertThat(catalog.categories("RETAIL").map { it.name })
            .contains("Retail test")
            .doesNotContain("Wholesale test")
        val wholesale = catalog.product(productId, "WHOLESALE")
        val retail = catalog.product(productId, "RETAIL")
        assertThat(wholesale.skus.single().salePrice).isEqualTo(1000L)
        assertThat(retail.skus.single().salePrice).isEqualTo(1500L)
        assertThat(wholesale.skus.single().id).isEqualTo(retail.skus.single().id)
        assertThat(wholesale.skus.single().salesOfferId).isNotEqualTo(retail.skus.single().salesOfferId)
        assertThat(wholesale.skus.single().unitsPerSale).isEqualTo(12)
        assertThat(retail.skus.single().unitsPerSale).isEqualTo(1)
        assertThat(catalog.products(0, 100, "WHOLESALE").items.first { it.id == productId }.startingUnitsPerSale).isEqualTo(12)
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
