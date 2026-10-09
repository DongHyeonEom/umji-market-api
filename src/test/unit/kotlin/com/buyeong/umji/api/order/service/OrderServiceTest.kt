package com.buyeong.umji.api.order.service

import com.buyeong.umji.api.account.model.OrganizationTaxInvoiceProfile
import com.buyeong.umji.api.cart.model.CartItemView
import com.buyeong.umji.api.cart.model.CartView
import com.buyeong.umji.api.cart.service.CartService
import com.buyeong.umji.api.inventory.service.InventoryService
import com.buyeong.umji.api.notification.model.NotificationEventType
import com.buyeong.umji.api.notification.service.NotificationEventService
import com.buyeong.umji.api.order.model.OrderItemView
import com.buyeong.umji.api.order.model.OrderView
import com.buyeong.umji.api.order.model.ShippingAddressSnapshot
import com.buyeong.umji.api.order.model.TaxInvoiceSupplier
import com.buyeong.umji.api.payment.integration.BankAccountInstructionsService
import com.buyeong.umji.api.payment.integration.TaxInvoiceSupplierService
import com.buyeong.umji.api.persistence.jpa.account.entity.AccountEntity
import com.buyeong.umji.api.persistence.jpa.account.service.AccountJpaEntityService
import com.buyeong.umji.api.persistence.jpa.account.service.CustomerAccountJpaEntityService
import com.buyeong.umji.api.persistence.jpa.account.service.OrganizationJpaEntityService
import com.buyeong.umji.api.persistence.jpa.account.service.OrganizationTaxInvoiceJpaEntityService
import com.buyeong.umji.api.persistence.jpa.catalog.entity.ProductEntity
import com.buyeong.umji.api.persistence.jpa.catalog.entity.ProductSkuEntity
import com.buyeong.umji.api.persistence.jpa.catalog.entity.SalesChannelEntity
import com.buyeong.umji.api.persistence.jpa.catalog.entity.SalesOfferEntity
import com.buyeong.umji.api.persistence.jpa.catalog.service.CatalogJpaEntityService
import com.buyeong.umji.api.persistence.jpa.order.service.OrderCheckoutJpaEntityService
import com.buyeong.umji.api.sales.service.SalesCommissionService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID

class OrderServiceTest : DescribeSpec({
    val carts = mockk<CartService>()
    val inventory = mockk<InventoryService>(relaxed = true)
    val orders = mockk<OrderCheckoutJpaEntityService>()
    val shippingAddresses = mockk<CustomerAccountJpaEntityService>()
    val notifications = mockk<NotificationEventService>(relaxed = true)
    val taxInvoiceSuppliers = mockk<TaxInvoiceSupplierService>(relaxed = true)
    val taxInvoiceBuyers = mockk<OrganizationTaxInvoiceJpaEntityService>(relaxed = true)
    val bankAccounts = mockk<BankAccountInstructionsService>(relaxed = true)
    val salesCommissions = mockk<SalesCommissionService>(relaxed = true)
    val accounts = mockk<AccountJpaEntityService>(relaxed = true)
    val catalog = mockk<CatalogJpaEntityService>(relaxed = true)
    val organizations = mockk<OrganizationJpaEntityService>(relaxed = true)
    val service =
        OrderService(carts, inventory, orders, shippingAddresses, bankAccounts, notifications, taxInvoiceSuppliers, taxInvoiceBuyers, salesCommissions, accounts, catalog, organizations)
    val accountId = UUID.randomUUID()
    val addressId = UUID.randomUUID()
    val skuId = UUID.randomUUID()
    val offerId = UUID.randomUUID()

    beforeTest {
        clearMocks(carts, inventory, orders, shippingAddresses, notifications, taxInvoiceSuppliers, taxInvoiceBuyers, bankAccounts, salesCommissions, accounts, catalog, organizations)
    }

    describe("주문 생성") {
        it("활성 구매자에 대해 서버의 활성 오퍼 가격을 사용하고 전화 주문 생성자와 출처를 기록한다") {
            val buyerId = UUID.randomUUID()
            val creatorId = UUID.randomUUID()
            val buyerProfile = OrganizationTaxInvoiceProfile(
                UUID.randomUUID(), "BUSINESS", null, "구매자", null, null, null, null,
                null, null, null, false, "NOT_REQUIRED", null, null,
            )
            val buyerAccount = AccountEntity().apply {
                status = "ACTIVE"
                phoneNormalized = "01012345678"
            }
            val creator = AccountEntity().apply { status = "ACTIVE" }
            val channel = mockk<SalesChannelEntity> { every { code } returns "WHOLESALE" }
            val product = mockk<ProductEntity> {
                every { name } returns "전화 주문 상품"
                every { salesStatus } returns "ON_SALE"
                every { displayStatus } returns "DISPLAYED"
                every { deletedAt } returns null
            }
            val sku = mockk<ProductSkuEntity>()
            every { sku.publicId } returns skuId
            every { sku.skuCode } returns "SKU-001"
            every { sku.name } returns "규격 A"
            every { sku.salesStatus } returns "ON_SALE"
            every { sku.product } returns product
            val offer = mockk<SalesOfferEntity> {
                every { publicId } returns offerId
                every { salesStatus } returns "ON_SALE"
                every { salesChannel } returns channel
                every { productSku } returns sku
                every { salePrice } returns 1250L
                every { unitsPerSale } returns 6
                every { organization } returns null
            }
            every { accounts.findByPublicId(buyerId) } returns buyerAccount
            every { accounts.findByPublicId(creatorId) } returns creator
            every { taxInvoiceBuyers.forAccount(buyerId) } returns buyerProfile
            every { catalog.salesOffer(offerId) } returns offer
            every { bankAccounts.standard() } returns com.buyeong.umji.api.order.model.BankAccountInstructions("은행", "123", "예금주")
            every { orders.save(any()) } answers {
                val draft = firstArg<com.buyeong.umji.api.order.model.OrderDraft>()
                draft.orderSource shouldBe "ADMIN_PHONE"
                draft.createdByAccountId shouldBe creatorId
                draft.items.single().unitPrice shouldBe 1250L
                draft.items.single().lineAmount shouldBe 2500L
                OrderView(
                    UUID.randomUUID(),
                    "UMJ-20261009-000001",
                    draft.status,
                    draft.subtotalAmount,
                    draft.totalAmount,
                    draft.orderedAt,
                    draft.items.map {
                        OrderItemView(
                            UUID.randomUUID(), it.skuId, it.reservationKey, it.productName, it.skuName, it.skuCode,
                            it.unitPrice, it.quantity, it.lineAmount, it.status, it.salesOfferId, it.unitsPerSale
                        )
                    },
                )
            }

            val result = service.createAdminPhoneOrder(
                creatorId,
                buyerId,
                ShippingAddressSnapshot("수령인", "01012345678", "12345", "서울 주소", null),
                listOf(com.buyeong.umji.api.order.model.AdminPhoneOrderLine(offerId, 2)),
                false,
            )

            result.single().totalAmount shouldBe 2500L
            verify(exactly = 1) { inventory.reserve(skuId, 12, any(), null, null) }
            verify(exactly = 1) { notifications.record(NotificationEventType.ORDER_CREATED, result.single().id) }
        }

        it("활성 구매자 Organization이 없으면 관리자 전화 주문을 생성하지 않는다") {
            val buyerId = UUID.randomUUID()
            every { accounts.findByPublicId(buyerId) } returns AccountEntity().apply {
                status = "ACTIVE"
                phoneNormalized = "01012345678"
            }
            every { taxInvoiceBuyers.forAccount(buyerId) } returns null

            shouldThrow<IllegalArgumentException> {
                service.createAdminPhoneOrder(
                    UUID.randomUUID(),
                    buyerId,
                    ShippingAddressSnapshot("수령인", "01012345678", "12345", "서울 주소", null),
                    listOf(com.buyeong.umji.api.order.model.AdminPhoneOrderLine(offerId, 1)),
                    false,
                )
            }

            verify(exactly = 0) { orders.save(any()) }
            verify(exactly = 0) { inventory.reserve(any(), any(), any(), any()) }
        }

        it("박스 수량과 입수량을 snapshot하고 기준 SKU 재고를 예약한다") {
            every { shippingAddresses.findForAccount(accountId, addressId) } returns
                ShippingAddressSnapshot("수령인", "01012345678", "12345", "서울 주소", null)
            every { carts.cart(accountId) } returns
                CartView(listOf(CartItemView(UUID.randomUUID(), skuId, "SKU-001", "테스트 상품", "규격 A", 3, 12000, "ON_SALE", offerId, "WHOLESALE", 12)))
            every { orders.save(any()) } answers {
                val draft = firstArg<com.buyeong.umji.api.order.model.OrderDraft>()
                draft.channelCode shouldBe "WHOLESALE"
                draft.items.single().salesOfferId shouldBe offerId
                draft.items.single().unitsPerSale shouldBe 12
                OrderView(
                    UUID.randomUUID(),
                    "UMJ-20260923-000001",
                    draft.status,
                    draft.subtotalAmount,
                    draft.totalAmount,
                    draft.orderedAt,
                    draft.items.map {
                        OrderItemView(
                            UUID.randomUUID(), it.skuId, it.reservationKey, it.productName, it.skuName, it.skuCode,
                            it.unitPrice, it.quantity, it.lineAmount, it.status, it.salesOfferId, it.unitsPerSale,
                        )
                    },
                )
            }
            every { carts.clearForCheckout(accountId) } returns Unit

            val result = service.create(accountId, addressId)
            val order = result.single()

            order.status shouldBe "PENDING_PAYMENT"
            order.subtotalAmount shouldBe 36000L
            order.items.single().unitPrice shouldBe 12000L
            order.items.single().quantity shouldBe 3
            order.items.single().unitsPerSale shouldBe 12
            verify(exactly = 1) { notifications.record(NotificationEventType.ORDER_CREATED, order.id, null) }
            verify(exactly = 1) { salesCommissions.snapshotOrder(order.id, accountId, any(), 36_000L) }
            verify(exactly = 1) { inventory.reserve(skuId, 36, any(), null) }
            verify(exactly = 1) { carts.clearForCheckout(accountId) }
        }

        it("입수량을 곱한 재고 수량이 정수 범위를 넘으면 주문을 저장하지 않는다") {
            every { shippingAddresses.findForAccount(accountId, addressId) } returns
                ShippingAddressSnapshot("수령인", "01012345678", "12345", "서울 주소", null)
            every { carts.cart(accountId) } returns
                CartView(
                    listOf(
                        CartItemView(UUID.randomUUID(), skuId, "SKU-001", "테스트 상품", "규격 A", Int.MAX_VALUE, 1, "ON_SALE", offerId, "WHOLESALE", 2),
                    ),
                )

            shouldThrow<ArithmeticException> { service.create(accountId, addressId) }

            verify(exactly = 0) { orders.save(any()) }
            verify(exactly = 0) { inventory.reserve(any(), any(), any(), any()) }
        }

        it("판매 Organization별 주문을 만들고 세금계산서 공급자 snapshot과 재고 귀속을 분리한다") {
            val sellerA = UUID.randomUUID()
            val sellerB = UUID.randomUUID()
            val sellerASku = UUID.randomUUID()
            val sellerBSku = UUID.randomUUID()
            val buyer = OrganizationTaxInvoiceProfile(
                UUID.randomUUID(), "BUSINESS", "1234567890", "구매자", "대표", "12345", "주소", null,
                "도소매", "공구", null, true, "ACTIVE", null, null,
            )
            val supplierA = TaxInvoiceSupplier("1111111111", "판매자 A", "대표 A", "주소 A", "도소매", "공구", "a@example.com")
            val supplierB = TaxInvoiceSupplier("2222222222", "판매자 B", "대표 B", "주소 B", "도소매", "공구", "b@example.com")
            every { shippingAddresses.findForAccount(accountId, addressId) } returns
                ShippingAddressSnapshot("수령인", "01012345678", "12345", "서울 주소", null)
            every { carts.cart(accountId) } returns CartView(
                listOf(
                    CartItemView(UUID.randomUUID(), sellerASku, "A-001", "상품 A", "규격 A", 2, 1000, "ON_SALE", UUID.randomUUID(), "WHOLESALE", 4, sellerA),
                    CartItemView(UUID.randomUUID(), sellerBSku, "B-001", "상품 B", "규격 B", 3, 2000, "ON_SALE", UUID.randomUUID(), "WHOLESALE", 6, sellerB),
                ),
            )
            every { taxInvoiceBuyers.forAccount(accountId) } returns buyer
            every { taxInvoiceBuyers.isSellerBusinessProfileReady(sellerA) } returns true
            every { taxInvoiceBuyers.isSellerBusinessProfileReady(sellerB) } returns true
            every { taxInvoiceBuyers.supplierForOrganization(sellerA) } returns supplierA
            every { taxInvoiceBuyers.supplierForOrganization(sellerB) } returns supplierB
            every { bankAccounts.taxInvoice() } returns com.buyeong.umji.api.order.model.BankAccountInstructions("은행", "123", "예금주")
            every { orders.save(any()) } answers {
                val draft = firstArg<com.buyeong.umji.api.order.model.OrderDraft>()
                val savedSeller = if (draft.items.single().skuId == sellerASku) sellerA else sellerB
                draft.items.size shouldBe 1
                draft.taxInvoiceSnapshot?.supplier shouldBe if (savedSeller == sellerA) supplierA else supplierB
                OrderView(
                    UUID.randomUUID(), "UMJ-20260923-000001", draft.status, draft.subtotalAmount, draft.totalAmount,
                    draft.orderedAt,
                    draft.items.map {
                        OrderItemView(
                            UUID.randomUUID(), it.skuId, it.reservationKey, it.productName, it.skuName, it.skuCode,
                            it.unitPrice, it.quantity, it.lineAmount, it.status, it.salesOfferId, it.unitsPerSale, savedSeller,
                        )
                    },
                    taxInvoiceRequested = draft.taxInvoiceRequested,
                    sellerOrganizationId = savedSeller,
                )
            }
            every { carts.clearForCheckout(accountId) } returns Unit

            val result = service.create(accountId, addressId, true, false)

            result.map { it.sellerOrganizationId } shouldBe listOf(sellerA, sellerB)
            result.map { it.totalAmount } shouldBe listOf(2000L, 6000L)
            verify(exactly = 1) { inventory.reserve(sellerASku, 8, any(), null, sellerA) }
            verify(exactly = 1) { inventory.reserve(sellerBSku, 18, any(), null, sellerB) }
            verify(exactly = 2) { notifications.record(NotificationEventType.ORDER_CREATED, any(), null) }
            verify(exactly = 1) { carts.clearForCheckout(accountId) }
        }

        it("확인 완료된 사업자 프로필이 없는 판매자의 상품은 주문할 수 없다") {
            val sellerOrganizationId = UUID.randomUUID()
            every { shippingAddresses.findForAccount(accountId, addressId) } returns
                ShippingAddressSnapshot("수령인", "01012345678", "12345", "서울 주소", null)
            every { carts.cart(accountId) } returns CartView(
                listOf(
                    CartItemView(
                        UUID.randomUUID(), skuId, "SKU-001", "판매자 상품", "규격 A", 1, 1000,
                        "ON_SALE", offerId, "WHOLESALE", 1, sellerOrganizationId,
                    ),
                ),
            )
            every { taxInvoiceBuyers.isSellerBusinessProfileReady(sellerOrganizationId) } returns false

            shouldThrow<IllegalArgumentException> { service.create(accountId, addressId) }

            verify(exactly = 0) { orders.save(any()) }
            verify(exactly = 0) { inventory.reserve(any(), any(), any(), any()) }
            verify(exactly = 0) { carts.clearForCheckout(accountId) }
        }
    }
})