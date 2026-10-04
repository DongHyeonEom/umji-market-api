package com.buyeong.umji.api.auth.adapter.`in`.security

import com.buyeong.umji.api.auth.application.port.out.AccountAuthenticationPort
import com.buyeong.umji.api.auth.config.AuthenticationProperties
import com.buyeong.umji.api.auth.config.JwtProperties
import com.buyeong.umji.api.auth.config.SecurityConfig
import com.buyeong.umji.api.exception.ErrorMessageService
import com.buyeong.umji.api.inventory.adapter.`in`.web.OperationInventoryController
import com.buyeong.umji.api.inventory.application.model.StockView
import com.buyeong.umji.api.inventory.application.port.`in`.InventoryUseCase
import com.buyeong.umji.api.notification.adapter.`in`.web.NotificationDeviceTokenController
import com.buyeong.umji.api.notification.application.model.NotificationDevicePlatform
import com.buyeong.umji.api.notification.application.model.NotificationDeviceTokenRegistration
import com.buyeong.umji.api.notification.application.port.`in`.NotificationDeviceTokenUseCase
import com.buyeong.umji.api.operation.account.adapter.`in`.web.OperationAccountController
import com.buyeong.umji.api.operation.account.adapter.`in`.web.OperationBuyerGroupController
import com.buyeong.umji.api.operation.account.application.port.`in`.OperationAccountUseCase
import com.buyeong.umji.api.operation.audit.adapter.`in`.web.OperationAuditController
import com.buyeong.umji.api.operation.audit.application.model.OperationAuditPage
import com.buyeong.umji.api.operation.audit.application.port.`in`.OperationAuditUseCase
import com.buyeong.umji.api.operation.catalog.adapter.`in`.web.OperationCatalogController
import com.buyeong.umji.api.operation.catalog.application.port.`in`.OperationCatalogUseCase
import com.buyeong.umji.api.operation.payment.adapter.`in`.web.OperationPaymentController
import com.buyeong.umji.api.operation.payment.adapter.`in`.web.TransactionalPaymentUseCase
import com.buyeong.umji.api.operation.shipment.adapter.`in`.web.OperationShipmentController
import com.buyeong.umji.api.operation.shipment.adapter.`in`.web.TransactionalShipmentUseCase
import com.buyeong.umji.api.order.adapter.`in`.web.OperationShippingHolidayController
import com.buyeong.umji.api.order.adapter.`in`.web.OrderCancellationController
import com.buyeong.umji.api.order.adapter.`in`.web.TransactionalOrderCancellationUseCase
import com.buyeong.umji.api.order.application.port.`in`.ShippingHolidayUseCase
import com.buyeong.umji.api.payment.application.model.PaymentQueuePage
import com.buyeong.umji.api.shipment.application.model.ShipmentChange
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
        OperationAccountController::class, OperationBuyerGroupController::class, OperationAuditController::class, OperationCatalogController::class,
        OperationInventoryController::class, OperationPaymentController::class, OperationShipmentController::class,
        OrderCancellationController::class, OperationShippingHolidayController::class, NotificationDeviceTokenController::class,
    ],
    properties = ["umji.security.authentication.mode=REQUIRED"],
)
@AutoConfigureMockMvc
@Import(SecurityConfig::class, OperationEndpointAuthorizationTestConfiguration::class)
class OperationEndpointAuthorizationTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockitoBean
    private lateinit var accounts: OperationAccountUseCase

    @MockitoBean
    private lateinit var catalog: OperationCatalogUseCase

    @MockitoBean
    private lateinit var inventory: InventoryUseCase

    @MockitoBean
    private lateinit var audit: OperationAuditUseCase

    @MockitoBean
    private lateinit var payments: TransactionalPaymentUseCase

    @MockitoBean
    private lateinit var shipments: TransactionalShipmentUseCase

    @MockitoBean
    private lateinit var cancellations: TransactionalOrderCancellationUseCase

    @MockitoBean
    private lateinit var holidays: ShippingHolidayUseCase

    @MockitoBean
    private lateinit var deviceTokens: NotificationDeviceTokenUseCase

    @MockitoBean
    private lateinit var currentAccounts: com.buyeong.umji.api.auth.application.port.`in`.CurrentAccountPort

    @MockitoBean
    private lateinit var errorMessages: ErrorMessageService

    @MockitoBean
    private lateinit var accountAuthentication: AccountAuthenticationPort

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
            .thenReturn(NotificationDeviceTokenRegistration(tokenId, NotificationDevicePlatform.ANDROID_FCM, registeredAt))

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
        val groupId = UUID.randomUUID()
        mockMvc.perform(
            put("/api/operation/buyer-groups/$groupId/representative")
                .with(authorities("ORDER_READ"))
                .with(csrf())
                .contentType("application/json")
                .content("""{"accountId":"${UUID.randomUUID()}"}"""),
        ).andExpect(status().isForbidden)

        mockMvc.perform(
            put("/api/operation/buyer-groups/$groupId/representative")
                .with(authorities("ADMIN_ACCOUNT_MANAGE"))
                .with(csrf())
                .contentType("application/json")
                .content("""{"accountId":"${UUID.randomUUID()}"}"""),
        ).andExpect(status().isOk)
    }

    @Test
    fun `inventory endpoint rejects product permission`() {
        mockMvc.perform(get("/api/operation/inventory/skus/${UUID.randomUUID()}").with(authorities("PRODUCT_READ")))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `inventory endpoint accepts inventory read permission`() {
        val skuId = UUID.randomUUID()
        Mockito.`when`(inventory.stock(skuId)).thenReturn(StockView(skuId, "SKU-1", 10, 0, 10, 0))
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
    fun `audit log endpoint denies ordinary account management permission`() {
        mockMvc.perform(get("/api/operation/audit-logs").with(authorities("ADMIN_ACCOUNT_MANAGE")))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `audit log endpoint accepts dedicated audit permission`() {
        Mockito.`when`(audit.search(com.buyeong.umji.api.operation.audit.application.model.OperationAuditQuery(null, null, null, null, 0, 20)))
            .thenReturn(OperationAuditPage(emptyList(), 0, 20, 0, 0))

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
        Mockito.`when`(payments.queue(null, 0, 20)).thenReturn(PaymentQueuePage(emptyList(), 0, 20, 0, 0))
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
            ShipmentChange(orderId, "IN_TRANSIT", "CJ", "1234567890", true),
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
            ShipmentChange(orderId, "DELIVERED", "DAESIN", "1501602023302", true),
        )

        mockMvc.perform(
            post("/api/operation/orders/$orderId/shipment/delivered")
                .with(authorities("SHIPMENT_WRITE")),
        ).andExpect(status().isOk)
    }

    @Test
    fun `cancellation queue requires order write permission`() {
        mockMvc.perform(get("/api/operation/order-cancellations").with(authorities("PRODUCT_WRITE")))
            .andExpect(status().isForbidden)
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

    private fun authorities(vararg permissions: String): RequestPostProcessor =
        user("operator").authorities(*permissions.map(::SimpleGrantedAuthority).toTypedArray())
}

@TestConfiguration(proxyBeanMethods = false)
@EnableConfigurationProperties(AuthenticationProperties::class, JwtProperties::class)
class OperationEndpointAuthorizationTestConfiguration {
    @Bean
    fun operationAuthorization(properties: AuthenticationProperties) = OperationAuthorization(properties)
}