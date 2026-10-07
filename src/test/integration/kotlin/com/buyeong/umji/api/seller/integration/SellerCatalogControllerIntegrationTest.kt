package com.buyeong.umji.api.seller.integration

import com.buyeong.umji.api.account.model.OrganizationSummary
import com.buyeong.umji.api.account.service.OrganizationMembershipService
import com.buyeong.umji.api.account.service.OrganizationTaxInvoiceProfileService
import com.buyeong.umji.api.auth.service.CurrentAccountService
import com.buyeong.umji.api.exception.DefaultErrorMessageService
import com.buyeong.umji.api.inventory.service.InventoryService
import com.buyeong.umji.api.operation.catalog.model.BrandView
import com.buyeong.umji.api.operation.catalog.model.ProductPageView
import com.buyeong.umji.api.operation.catalog.service.OperationCatalogService
import com.buyeong.umji.api.persistence.jpa.catalog.service.CatalogJpaEntityService
import com.buyeong.umji.api.seller.controller.SellerCatalogController
import com.buyeong.umji.api.seller.service.SellerCatalogService
import java.util.UUID
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(SellerCatalogController::class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SellerCatalogService::class, DefaultErrorMessageService::class)
class SellerCatalogControllerIntegrationTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockitoBean
    private lateinit var currentAccounts: CurrentAccountService

    @MockitoBean
    private lateinit var organizations: OrganizationMembershipService

    @MockitoBean
    private lateinit var catalog: OperationCatalogService

    @MockitoBean
    private lateinit var inventory: InventoryService

    @MockitoBean
    private lateinit var commonCatalog: CatalogJpaEntityService

    @MockitoBean
    private lateinit var businessProfiles: OrganizationTaxInvoiceProfileService

    @Test
    fun `brand and product responses are scoped to the active seller organization`() {
        val accountId = UUID.randomUUID()
        val organizationId = UUID.randomUUID()
        val brandId = UUID.randomUUID()
        Mockito.`when`(currentAccounts.activeAccountPublicId()).thenReturn(accountId)
        Mockito.`when`(organizations.current(accountId)).thenReturn(
            OrganizationSummary(organizationId, "BUSINESS", "Seller", true, setOf("SELLER")),
        )
        Mockito.`when`(catalog.sellerBrands(organizationId, 0, 20)).thenReturn(
            listOf(BrandView(brandId, "Seller brand", "DISPLAYED")),
        )
        Mockito.`when`(catalog.sellerProducts(organizationId, 0, 20)).thenReturn(
            ProductPageView(emptyList(), 0, 20, 0, 0),
        )

        mockMvc.perform(get("/api/seller/brands"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(brandId.toString()))
            .andExpect(jsonPath("$[0].name").value("Seller brand"))
        mockMvc.perform(get("/api/seller/products"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items").isArray)
            .andExpect(jsonPath("$.items").isEmpty)

        Mockito.verify(catalog).sellerBrands(organizationId, 0, 20)
        Mockito.verify(catalog).sellerProducts(organizationId, 0, 20)
        Mockito.verify(organizations, Mockito.times(2)).current(accountId)
    }
}
