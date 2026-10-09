package com.buyeong.umji.api.seller.integration

import com.buyeong.umji.api.account.model.OrganizationSummary
import com.buyeong.umji.api.account.service.OrganizationMembershipService
import com.buyeong.umji.api.account.service.OrganizationTaxInvoiceProfileService
import com.buyeong.umji.api.auth.service.CurrentAccountService
import com.buyeong.umji.api.exception.DefaultErrorMessageService
import com.buyeong.umji.api.inventory.service.InventoryService
import com.buyeong.umji.api.operation.catalog.service.OperationCatalogService
import com.buyeong.umji.api.persistence.jpa.account.entity.OrganizationEntity
import com.buyeong.umji.api.persistence.jpa.account.repository.OrganizationRepository
import com.buyeong.umji.api.persistence.jpa.catalog.entity.BrandEntity
import com.buyeong.umji.api.persistence.jpa.catalog.entity.CategoryEntity
import com.buyeong.umji.api.persistence.jpa.catalog.entity.SalesChannelEntity
import com.buyeong.umji.api.persistence.jpa.catalog.repository.ChannelProductListingRepository
import com.buyeong.umji.api.persistence.jpa.catalog.repository.SalesChannelRepository
import com.buyeong.umji.api.persistence.jpa.catalog.repository.SalesOfferRepository
import com.buyeong.umji.api.persistence.jpa.catalog.service.CatalogJpaEntityService
import com.buyeong.umji.api.persistence.jpa.catalog.service.OperationCatalogJpaEntityService
import com.buyeong.umji.api.seller.controller.SellerCatalogController
import com.buyeong.umji.api.seller.service.SellerCatalogService
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@WebMvcTest(SellerCatalogController::class)
@AutoConfigureMockMvc(addFilters = false)
@Import(
    SellerCatalogService::class,
    OperationCatalogService::class,
    OperationCatalogJpaEntityService::class,
    DefaultErrorMessageService::class,
)
class SellerCatalogBrandOwnershipApiIntegrationTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockitoBean
    private lateinit var currentAccounts: CurrentAccountService

    @MockitoBean
    private lateinit var organizations: OrganizationMembershipService

    @MockitoBean
    private lateinit var inventory: InventoryService

    @MockitoBean
    private lateinit var businessProfiles: OrganizationTaxInvoiceProfileService

    @MockitoBean
    private lateinit var catalog: CatalogJpaEntityService

    @MockitoBean
    private lateinit var channels: SalesChannelRepository

    @MockitoBean
    private lateinit var listings: ChannelProductListingRepository

    @MockitoBean
    private lateinit var offers: SalesOfferRepository

    @MockitoBean
    private lateinit var organizationRepository: OrganizationRepository

    @Test
    fun `product creation rejects a brand owned by another organization`() {
        val accountId = UUID.randomUUID()
        val sellerOrganizationId = UUID.randomUUID()
        val categoryId = UUID.randomUUID()
        val brandId = UUID.randomUUID()
        val sellerOrganization = organization(1L, sellerOrganizationId)
        val foreignOrganization = organization(2L, UUID.randomUUID())
        val category = CategoryEntity().apply {
            salesChannel = SalesChannelEntity().apply {
                code = "WHOLESALE"
                name = "Wholesale"
            }
            name = "Category"
            path = "Category"
            displayStatus = "DISPLAYED"
        }
        val foreignBrand = BrandEntity().apply {
            organization = foreignOrganization
            name = "Foreign brand"
            displayStatus = "DISPLAYED"
        }
        Mockito.`when`(currentAccounts.activeAccountPublicId()).thenReturn(accountId)
        Mockito.`when`(organizations.current(accountId)).thenReturn(
            OrganizationSummary(sellerOrganizationId, "BUSINESS", "Seller", true, setOf("SELLER")),
        )
        Mockito.`when`(organizationRepository.findByPublicId(sellerOrganizationId)).thenReturn(sellerOrganization)
        Mockito.`when`(catalog.category(categoryId)).thenReturn(category)
        Mockito.`when`(catalog.brand(brandId)).thenReturn(foreignBrand)

        val body = """
            {
              "categoryId": "$categoryId",
              "brandId": "$brandId",
              "name": "Seller product",
              "displayStatus": "DISPLAYED",
              "salesStatus": "ON_SALE",
              "displayOrder": 0,
              "skus": [{
                "skuCode": "SELLER-001",
                "name": "Seller SKU",
                "salePrice": 1000,
                "salesStatus": "ON_SALE"
              }]
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/seller/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body),
        ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.reason").value("상품과 같은 Organization 소유의 브랜드를 선택해야 합니다."))

        Mockito.verify(catalog).brand(brandId)
    }

    private fun organization(id: Long, publicId: UUID) = OrganizationEntity().apply {
        this.id = id
        this.publicId = publicId
        organizationType = "BUSINESS"
        displayName = "Seller"
        status = "ACTIVE"
    }
}