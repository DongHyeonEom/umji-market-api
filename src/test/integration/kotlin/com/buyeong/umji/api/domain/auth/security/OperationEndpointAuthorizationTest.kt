package com.buyeong.umji.api.domain.auth.security

import com.buyeong.umji.api.domain.access.controller.AccessContextController
import com.buyeong.umji.api.domain.access.model.AccessAudience
import com.buyeong.umji.api.domain.access.model.AccessContextResponse
import com.buyeong.umji.api.domain.access.model.AccessScreenResponse
import com.buyeong.umji.api.domain.access.service.AccessContextService
import com.buyeong.umji.api.domain.account.service.OrganizationTaxInvoiceProfileService
import com.buyeong.umji.api.domain.auth.config.AuthenticationProperties
import com.buyeong.umji.api.domain.auth.config.JwtProperties
import com.buyeong.umji.api.domain.auth.config.SecurityConfig
import com.buyeong.umji.api.domain.auth.controller.AuthenticationController
import com.buyeong.umji.api.domain.auth.service.AuthenticationService
import com.buyeong.umji.api.domain.auth.service.WebAuthenticationService
import com.buyeong.umji.api.domain.inventory.controller.OperationInventoryController
import com.buyeong.umji.api.domain.inventory.dto.StockViewDto
import com.buyeong.umji.api.domain.inventory.service.InventoryService
import com.buyeong.umji.api.domain.notification.controller.NotificationDeviceTokenController
import com.buyeong.umji.api.domain.notification.dto.NotificationDeviceTokenRegistrationDto
import com.buyeong.umji.api.domain.notification.model.NotificationDevicePlatform
import com.buyeong.umji.api.domain.notification.service.NotificationDeviceTokenService
import com.buyeong.umji.api.domain.operation.account.controller.OperationAccountController
import com.buyeong.umji.api.domain.operation.account.controller.OperationOrganizationController
import com.buyeong.umji.api.domain.operation.account.service.OperationAccountService
import com.buyeong.umji.api.domain.operation.audit.controller.OperationAuditController
import com.buyeong.umji.api.domain.operation.audit.dto.OperationAuditPageDto
import com.buyeong.umji.api.domain.operation.audit.service.OperationAuditService
import com.buyeong.umji.api.domain.operation.catalog.controller.OperationCatalogController
import com.buyeong.umji.api.domain.operation.catalog.service.OperationCatalogService
import com.buyeong.umji.api.domain.operation.order.controller.OperationPhoneOrderController
import com.buyeong.umji.api.domain.operation.order.controller.OperationTaxInvoiceController
import com.buyeong.umji.api.domain.operation.order.dto.TaxInvoiceQueueDataDto
import com.buyeong.umji.api.domain.operation.order.service.OperationTaxInvoiceService
import com.buyeong.umji.api.domain.operation.payment.controller.OperationPaymentController
import com.buyeong.umji.api.domain.operation.shipment.controller.OperationShipmentController
import com.buyeong.umji.api.domain.order.controller.OperationShippingHolidayController
import com.buyeong.umji.api.domain.order.controller.OrderCancellationController
import com.buyeong.umji.api.domain.order.dto.AdminPhoneOrderBuyerDto
import com.buyeong.umji.api.domain.order.dto.CancellationQueuePageDto
import com.buyeong.umji.api.domain.order.service.OrderCancellationService
import com.buyeong.umji.api.domain.order.service.OrderService
import com.buyeong.umji.api.domain.order.service.ShippingHolidayService
import com.buyeong.umji.api.domain.payment.dto.PaymentQueuePageDto
import com.buyeong.umji.api.domain.payment.service.PaymentService
import com.buyeong.umji.api.domain.sales.dto.SalesAssignmentCommandDto
import com.buyeong.umji.api.domain.sales.dto.SalesCommissionPageDto
import com.buyeong.umji.api.domain.sales.dto.SalesCommissionSettlementResultDto
import com.buyeong.umji.api.domain.sales.dto.SalesCommissionViewDto
import com.buyeong.umji.api.domain.sales.service.SalesAssignmentService
import com.buyeong.umji.api.domain.sales.service.SalesCommissionService
import com.buyeong.umji.api.domain.shipment.dto.ShipmentChangeDto
import com.buyeong.umji.api.domain.shipment.service.ShipmentService
import com.buyeong.umji.api.exception.DefaultErrorMessageService
import com.buyeong.umji.api.persistence.jpa.auth.service.AuthenticationJpaEntityService
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.request.RequestPostProcessor
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@WebMvcTest(
    controllers = [
        AuthenticationController::class,
        AccessContextController::class,
        OperationAccountController::class, OperationOrganizationController::class, OperationAuditController::class, OperationCatalogController::class,
        OperationInventoryController::class, OperationPaymentController::class, OperationShipmentController::class,
        com.buyeong.umji.api.domain.sales.controller.SalesAssignmentController::class,
        com.buyeong.umji.api.domain.sales.controller.SalesCommissionController::class,
        OrderCancellationController::class, OperationShippingHolidayController::class, OperationPhoneOrderController::class,
        OperationTaxInvoiceController::class, NotificationDeviceTokenController::class,
    ],
    properties = ["umji.security.authentication.mode=REQUIRED"],
)
@AutoConfigureMockMvc
@Import(SecurityConfig::class, OperationEndpointAuthorizationTestConfiguration::class)
class OperationEndpointAuthorizationTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockitoBean
    private lateinit var accessContexts: AccessContextService

    @MockitoBean
    private lateinit var accounts: OperationAccountService

    @MockitoBean
    private lateinit var salesAssignments: SalesAssignmentService

    @MockitoBean
    private lateinit var salesCommissions: SalesCommissionService

    @MockitoBean
    private lateinit var taxInvoiceProfiles: OrganizationTaxInvoiceProfileService

    @MockitoBean
    private lateinit var catalog: OperationCatalogService

    @MockitoBean
    private lateinit var inventory: InventoryService

    @MockitoBean
    private lateinit var audit: OperationAuditService

    @MockitoBean
    private lateinit var payments: PaymentService

    @MockitoBean
    private lateinit var shipments: ShipmentService

    @MockitoBean
    private lateinit var cancellations: OrderCancellationService

    @MockitoBean
    private lateinit var holidays: ShippingHolidayService

    @MockitoBean
    private lateinit var orders: OrderService

    @MockitoBean
    private lateinit var operationTaxInvoices: OperationTaxInvoiceService

    @MockitoBean
    private lateinit var deviceTokens: NotificationDeviceTokenService

    @MockitoBean
    private lateinit var currentAccounts: com.buyeong.umji.api.domain.auth.service.CurrentAccountService

    @MockitoBean
    private lateinit var authentication: AuthenticationService

    @MockitoBean
    private lateinit var webAuthentication: WebAuthenticationService

    @MockitoBean
    private lateinit var errorMessages: DefaultErrorMessageService

    @MockitoBean
    private lateinit var accountAuthentication: AuthenticationJpaEntityService

    @MockitoBean
    private lateinit var jwtDecoder: JwtDecoder

    @MockitoBean
    private lateinit var jwtEncoder: JwtEncoder

    @Test
    fun `anonymous operation request returns 401`() {
        mockMvc.perform(get("/api/operation/inventory/skus/${UUID.randomUUID()}"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `web password and TOTP management endpoints require an authenticated phone login token`() {
        mockMvc.perform(post("/api/auth/web-password").contentType("application/json").content("{}"))
            .andExpect(status().isUnauthorized)
        mockMvc.perform(post("/api/auth/admin/totp/setup"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `admin TOTP reset requires account management permission`() {
        val accountId = UUID.randomUUID()
        mockMvc.perform(post("/api/operation/accounts/$accountId/totp/reset").with(authorities("PRODUCT_READ")))
            .andExpect(status().isForbidden)

        mockMvc.perform(post("/api/operation/accounts/$accountId/totp/reset").with(authorities("ADMIN_ACCOUNT_MANAGE")))
            .andExpect(status().isNoContent)

        Mockito.verify(webAuthentication).resetTotp(accountId)
    }

    @Test
    fun `anonymous device token registration returns 401`() {
        mockMvc.perform(
            post("/api/notifications/device-tokens")
                .with(csrf())
                .contentType("application/json")
                .content("""{"platform":"ANDROID_FCM","token":"fcm-token"}"""),
        ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `anonymous device token revocation returns 401`() {
        mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
                "/api/notifications/device-tokens/${UUID.randomUUID()}",
            ).with(csrf()),
        ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `unsupported token platform returns 400 without registering a token`() {
        mockMvc.perform(
            post("/api/notifications/device-tokens")
                .with(user("customer"))
                .with(csrf())
                .contentType("application/json")
                .content("""{"platform":"WEB","token":"browser-token"}"""),
        ).andExpect(status().isBadRequest)

        Mockito.verifyNoInteractions(deviceTokens)
    }

    @Test
    fun `authenticated device token registration uses the current account`() {
        val accountId = UUID.randomUUID()
        val tokenId = UUID.randomUUID()
        val registeredAt = java.time.Instant.parse("2026-10-05T00:00:00Z")
        Mockito.`when`(currentAccounts.activeAccountPublicId()).thenReturn(accountId)
        Mockito.`when`(deviceTokens.register(accountId, NotificationDevicePlatform.ANDROID_FCM, "fcm-token"))
            .thenReturn(NotificationDeviceTokenRegistrationDto(tokenId, NotificationDevicePlatform.ANDROID_FCM, registeredAt))

        mockMvc.perform(
            post("/api/notifications/device-tokens")
                .with(user("customer"))
                .with(csrf())
                .contentType("application/json")
                .content("""{"platform":"ANDROID_FCM","token":"fcm-token"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.id").value(tokenId.toString()))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.token").doesNotExist())

        Mockito.verify(deviceTokens).register(accountId, NotificationDevicePlatform.ANDROID_FCM, "fcm-token")
    }

    @Test
    fun `authenticated account can only request revocation under its identity`() {
        val accountId = UUID.randomUUID()
        val tokenId = UUID.randomUUID()
        Mockito.`when`(currentAccounts.activeAccountPublicId()).thenReturn(accountId)

        mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/notifications/device-tokens/$tokenId")
                .with(user("customer"))
                .with(csrf()),
        ).andExpect(status().isNoContent)

        Mockito.verify(deviceTokens).revoke(accountId, tokenId)
    }

    @Test
    fun `unmapped operation endpoint is denied by default`() {
        mockMvc.perform(get("/api/operation/unmapped").with(authorities("PRODUCT_READ")))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `buyer group representative change requires account management permission`() {
        val organizationId = UUID.randomUUID()
        mockMvc.perform(
            put("/api/operation/organizations/$organizationId/representative")
                .with(authorities("ORDER_READ"))
                .with(csrf())
                .contentType("application/json")
                .content("""{"accountId":"${UUID.randomUUID()}"}"""),
        ).andExpect(status().isForbidden)

        mockMvc.perform(
            put("/api/operation/organizations/$organizationId/representative")
                .with(authorities("ADMIN_ACCOUNT_MANAGE"))
                .with(csrf())
                .contentType("application/json")
                .content("""{"accountId":"${UUID.randomUUID()}"}"""),
        ).andExpect(status().isOk)
    }

    @Test
    fun `assigning buyer group requires account management permission`() {
        val accountId = UUID.randomUUID()
        val organizationId = UUID.randomUUID()
        mockMvc.perform(
            put("/api/operation/accounts/$accountId/organization")
                .with(authorities("ORDER_READ"))
                .with(csrf())
                .contentType("application/json")
                .content("""{"organizationId":"$organizationId"}"""),
        ).andExpect(status().isForbidden)

        Mockito.`when`(accounts.assignOrganization(accountId, organizationId)).thenReturn(
            com.buyeong.umji.api.domain.operation.account.dto.AccountDataDto(accountId, "Buyer", "01012345678", null, "ACTIVE", 1, organizationId = organizationId),
        )
        mockMvc.perform(
            put("/api/operation/accounts/$accountId/organization")
                .with(authorities("ADMIN_ACCOUNT_MANAGE"))
                .with(csrf())
                .contentType("application/json")
                .content("""{"organizationId":"$organizationId"}"""),
        ).andExpect(status().isOk)
        Mockito.verify(accounts).assignOrganization(accountId, organizationId)
    }

    @Test
    fun `buyer group tax invoice profile requires account management permission`() {
        mockMvc.perform(
            get("/api/operation/organizations/${UUID.randomUUID()}/tax-invoice-profile")
                .with(authorities("ORDER_READ")),
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `inventory endpoint rejects product permission`() {
        mockMvc.perform(get("/api/operation/inventory/skus/${UUID.randomUUID()}").with(authorities("PRODUCT_READ")))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `inventory endpoint accepts inventory read permission`() {
        val skuId = UUID.randomUUID()
        Mockito.`when`(inventory.stock(skuId)).thenReturn(StockViewDto(skuId, "SKU-1", 10, 0, 10, 0))
        mockMvc.perform(get("/api/operation/inventory/skus/$skuId").with(authorities("INVENTORY_READ")))
            .andExpect(status().isOk)
    }

    @Test
    fun `inventory adjustment rejects read only permission`() {
        mockMvc.perform(
            patch("/api/operation/inventory/skus/${UUID.randomUUID()}")
                .with(authorities("INVENTORY_READ"))
                .with(csrf())
                .contentType("application/json")
                .content("""{"quantityDelta":1,"reason":"ADJUSTMENT"}"""),
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `catalog endpoint rejects inventory permission`() {
        mockMvc.perform(get("/api/operation/categories").with(authorities("INVENTORY_READ")))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `catalog endpoint accepts product read permission`() {
        mockMvc.perform(get("/api/operation/categories").with(authorities("PRODUCT_READ")))
            .andExpect(status().isOk)
    }

    @Test
    fun `catalog write endpoint rejects product read only permission`() {
        mockMvc.perform(
            post("/api/operation/categories")
                .with(authorities("PRODUCT_READ"))
                .with(csrf())
                .contentType("application/json")
                .content("""{"name":"상의","displayStatus":"VISIBLE"}"""),
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `account endpoint rejects catalog permission`() {
        mockMvc.perform(get("/api/operation/accounts/roles").with(authorities("PRODUCT_READ")))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `account endpoint accepts account management permission`() {
        mockMvc.perform(get("/api/operation/accounts/roles").with(authorities("ADMIN_ACCOUNT_MANAGE")))
            .andExpect(status().isOk)
    }

    @Test
    fun `sales assignment read and change endpoints require their dedicated permissions`() {
        val organizationId = UUID.randomUUID()
        val salesAccountId = UUID.randomUUID()
        val operatorId = UUID.randomUUID()
        Mockito.`when`(salesAssignments.history(organizationId)).thenReturn(emptyList())
        Mockito.`when`(currentAccounts.activeAccountPublicId()).thenReturn(operatorId)
        Mockito.`when`(
            salesAssignments.assign(
                organizationId,
                SalesAssignmentCommandDto(salesAccountId, null, "INITIAL_ASSIGNMENT", operatorId),
            ),
        ).thenReturn(emptyList())

        mockMvc.perform(get("/api/operation/organizations/$organizationId/sales-assignment").with(authorities("PRODUCT_READ")))
            .andExpect(status().isForbidden)
        mockMvc.perform(get("/api/operation/organizations/$organizationId/sales-assignment").with(authorities("SALES_GROUP_READ")))
            .andExpect(status().isOk)

        val body = """{"salesAccountId":"$salesAccountId","commissionRateBps":null,"assignmentReason":"INITIAL_ASSIGNMENT"}"""
        mockMvc.perform(
            put("/api/operation/organizations/$organizationId/sales-assignment")
                .with(authorities("SALES_GROUP_READ"))
                .with(csrf())
                .contentType("application/json")
                .content(body),
        ).andExpect(status().isForbidden)
        mockMvc.perform(
            put("/api/operation/organizations/$organizationId/sales-assignment")
                .with(authorities("SALES_GROUP_ASSIGN"))
                .with(csrf())
                .contentType("application/json")
                .content(body),
        ).andExpect(status().isOk)
    }

    @Test
    fun `sales commission reads settlement and payout require dedicated permissions`() {
        val operatorId = UUID.randomUUID()
        val commissionId = UUID.randomUUID()
        Mockito.`when`(currentAccounts.activeAccountPublicId()).thenReturn(operatorId)
        Mockito.`when`(salesCommissions.mine(operatorId, 0, 20)).thenReturn(SalesCommissionPageDto(emptyList(), 0, 20, 0, 0))
        Mockito.`when`(salesCommissions.all(0, 20)).thenReturn(SalesCommissionPageDto(emptyList(), 0, 20, 0, 0))
        Mockito.`when`(salesCommissions.settle(java.time.YearMonth.parse("2026-09"), operatorId))
            .thenReturn(SalesCommissionSettlementResultDto(java.time.YearMonth.parse("2026-09"), 0, 0))
        Mockito.`when`(salesCommissions.markPaid(commissionId, operatorId)).thenReturn(
            SalesCommissionViewDto(
                commissionId, UUID.randomUUID(), UUID.randomUUID(), 30, 100_000, 300, "PAID",
                java.time.LocalDate.parse(
                    "2026-09-01"
                ),
                null, null, java.time.Instant.now()
            ),
        )

        mockMvc.perform(get("/api/operation/sales-commissions/me").with(authorities("SALES_GROUP_READ")))
            .andExpect(status().isForbidden)
        mockMvc.perform(get("/api/operation/sales-commissions/me").with(authorities("SALES_COMMISSION_READ")))
            .andExpect(status().isOk)
        mockMvc.perform(get("/api/operation/sales-commissions").with(authorities("SALES_COMMISSION_READ")))
            .andExpect(status().isForbidden)
        mockMvc.perform(get("/api/operation/sales-commissions").with(authorities("SALES_COMMISSION_READ_ALL")))
            .andExpect(status().isOk)
        mockMvc.perform(post("/api/operation/sales-commissions/settlements/2026-09").with(authorities("SALES_COMMISSION_READ_ALL")).with(csrf()))
            .andExpect(status().isForbidden)
        mockMvc.perform(post("/api/operation/sales-commissions/settlements/2026-09").with(authorities("SALES_COMMISSION_SETTLE")).with(csrf()))
            .andExpect(status().isOk)
        mockMvc.perform(put("/api/operation/sales-commissions/$commissionId/paid").with(authorities("SALES_COMMISSION_SETTLE")).with(csrf()))
            .andExpect(status().isOk)
    }

    @Test
    fun `role management endpoint rejects unrelated sales permission`() {
        mockMvc.perform(
            put("/api/operation/accounts/${UUID.randomUUID()}/roles/SALES_MANAGER")
                .with(authorities("SALES_GROUP_ASSIGN"))
                .with(csrf()),
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `audit log endpoint denies ordinary account management permission`() {
        mockMvc.perform(get("/api/operation/audit-logs").with(authorities("ADMIN_ACCOUNT_MANAGE")))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `audit log endpoint accepts dedicated audit permission`() {
        Mockito.`when`(audit.search(com.buyeong.umji.api.domain.operation.audit.dto.OperationAuditQueryDto(null, null, null, null, 0, 20)))
            .thenReturn(OperationAuditPageDto(emptyList(), 0, 20, 0, 0))

        mockMvc.perform(get("/api/operation/audit-logs").with(authorities("ADMIN_AUDIT_READ")))
            .andExpect(status().isOk)
    }

    @Test
    fun `payment queue rejects non order write permission`() {
        mockMvc.perform(get("/api/operation/payments").with(authorities("PRODUCT_READ")))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `payment queue accepts order write permission`() {
        Mockito.`when`(payments.queue(null, 0, 20)).thenReturn(PaymentQueuePageDto(emptyList(), 0, 20, 0, 0))
        mockMvc.perform(get("/api/operation/payments").with(authorities("ORDER_WRITE")))
            .andExpect(status().isOk)
    }

    @Test
    fun `shipment tracking rejects non order write permission`() {
        mockMvc.perform(
            put(
                "/api/operation/orders/${UUID.randomUUID()}/shipment/tracking",
            )
                .with(authorities("PRODUCT_WRITE"))
                .contentType("application/json")
                .content("""{"carrierCode":"CJ","trackingNumber":"1234567890"}"""),
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `shipment tracking accepts shipment write permission`() {
        val orderId = UUID.randomUUID()
        val operatorId = UUID.randomUUID()
        Mockito.`when`(currentAccounts.activeAccountPublicId()).thenReturn(operatorId)
        Mockito.`when`(shipments.registerTracking(orderId, "CJ", "1234567890", operatorId)).thenReturn(
            ShipmentChangeDto(orderId, "IN_TRANSIT", "CJ", "1234567890", true),
        )

        mockMvc.perform(
            put(
                "/api/operation/orders/$orderId/shipment/tracking",
            )
                .with(authorities("SHIPMENT_WRITE"))
                .contentType("application/json")
                .content("""{"carrierCode":"CJ","trackingNumber":"1234567890"}"""),
        ).andExpect(status().isOk)
    }

    @Test
    fun `shipment delivery completion requires shipment write permission`() {
        mockMvc.perform(
            post("/api/operation/orders/${UUID.randomUUID()}/shipment/delivered")
                .with(authorities("PRODUCT_WRITE")),
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `shipment delivery completion accepts shipment write permission`() {
        val orderId = UUID.randomUUID()
        val operatorId = UUID.randomUUID()
        Mockito.`when`(currentAccounts.activeAccountPublicId()).thenReturn(operatorId)
        Mockito.`when`(shipments.markDelivered(orderId, operatorId)).thenReturn(
            ShipmentChangeDto(orderId, "DELIVERED", "DAESIN", "1501602023302", true),
        )

        mockMvc.perform(
            post("/api/operation/orders/$orderId/shipment/delivered")
                .with(authorities("SHIPMENT_WRITE")),
        ).andExpect(status().isOk)
    }

    @Test
    fun `screen access context does not grant direct shipment API permission`() {
        Mockito.`when`(accessContexts.get(AccessAudience.ADMIN)).thenReturn(
            AccessContextResponse(
                audience = AccessAudience.ADMIN,
                roles = listOf("SHIPPING_MANAGER"),
                permissions = listOf("SHIPMENT_READ"),
                organizationId = null,
                membershipRole = null,
                screens = listOf(AccessScreenResponse("ADMIN_SHIPMENT_LIST", "ADMIN_SHIPMENT_LIST")),
            ),
        )

        mockMvc.perform(get("/api/access-context?audience=ADMIN").with(authorities("SHIPMENT_READ")))
            .andExpect(status().isOk)

        mockMvc.perform(
            post("/api/operation/orders/${UUID.randomUUID()}/shipment/delivered")
                .with(authorities("SHIPMENT_READ")),
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `cancellation queue requires order write permission`() {
        mockMvc.perform(get("/api/operation/order-cancellations").with(authorities("PRODUCT_WRITE")))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `shipping permission cannot access payment or cancellation queues`() {
        mockMvc.perform(get("/api/operation/payments").with(authorities("SHIPMENT_WRITE")))
            .andExpect(status().isForbidden)
        mockMvc.perform(get("/api/operation/order-cancellations").with(authorities("SHIPMENT_WRITE")))
            .andExpect(status().isForbidden)

        Mockito.`when`(payments.queue(null, 0, 20)).thenReturn(PaymentQueuePageDto(emptyList(), 0, 20, 0, 0))
        Mockito.`when`(cancellations.queue(0, 20)).thenReturn(CancellationQueuePageDto(emptyList(), 0, 20, 0, 0))
        mockMvc.perform(get("/api/operation/payments").with(authorities("ORDER_WRITE")))
            .andExpect(status().isOk)
        mockMvc.perform(get("/api/operation/order-cancellations").with(authorities("ORDER_WRITE")))
            .andExpect(status().isOk)
    }

    @Test
    fun `holiday calendar requires order write permission`() {
        mockMvc.perform(get("/api/operation/shipping-holidays").with(authorities("INVENTORY_WRITE")))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `holiday calendar accepts order write permission`() {
        Mockito.`when`(holidays.list()).thenReturn(emptyList())
        mockMvc.perform(get("/api/operation/shipping-holidays").with(authorities("ORDER_WRITE")))
            .andExpect(status().isOk)
    }

    @Test
    fun `phone order and manual invoice endpoints require order write permission`() {
        mockMvc.perform(get("/api/operation/orders/phone-orders/buyers?phone=01012345678").with(authorities("PRODUCT_WRITE")))
            .andExpect(status().isForbidden)
        mockMvc.perform(get("/api/operation/orders/tax-invoices").with(authorities("SHIPMENT_WRITE")))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `order write permission can search phone order buyers and read invoice queue`() {
        val buyerId = UUID.randomUUID()
        Mockito.`when`(orders.findAdminPhoneOrderBuyer("01012345678"))
            .thenReturn(AdminPhoneOrderBuyerDto(buyerId, "구매자", "01012345678", UUID.randomUUID(), "구매 조직"))
        Mockito.`when`(operationTaxInvoices.queue(0, 20)).thenReturn(TaxInvoiceQueueDataDto(emptyList(), 0, 20, 0, 0))

        mockMvc.perform(get("/api/operation/orders/phone-orders/buyers?phone=01012345678").with(authorities("ORDER_WRITE")))
            .andExpect(status().isOk)
        mockMvc.perform(get("/api/operation/orders/tax-invoices").with(authorities("ORDER_WRITE")))
            .andExpect(status().isOk)
    }

    private fun authorities(vararg permissions: String): RequestPostProcessor =
        user("operator").authorities(*permissions.map(::SimpleGrantedAuthority).toTypedArray())
}

@TestConfiguration(proxyBeanMethods = false)
@EnableConfigurationProperties(AuthenticationProperties::class, JwtProperties::class)
class OperationEndpointAuthorizationTestConfiguration {
    @Bean
    fun operationAuthorization(properties: AuthenticationProperties) = OperationAuthorization(properties)
}