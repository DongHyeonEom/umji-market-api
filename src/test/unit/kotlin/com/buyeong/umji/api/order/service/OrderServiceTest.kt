package com.buyeong.umji.api.order.service

import com.buyeong.umji.api.account.persistence.AccountEntity
import com.buyeong.umji.api.cart.persistence.CartEntity
import com.buyeong.umji.api.cart.persistence.CartItemEntity
import com.buyeong.umji.api.cart.persistence.CartRepository
import com.buyeong.umji.api.catalog.persistence.ProductEntity
import com.buyeong.umji.api.catalog.persistence.ProductSkuEntity
import com.buyeong.umji.api.inventory.service.InventoryService
import com.buyeong.umji.api.order.persistence.OrderStatusHistoryEntity
import com.buyeong.umji.api.order.persistence.OrderStatusHistoryRepository
import com.buyeong.umji.api.order.persistence.OrderNumberSequenceEntity
import com.buyeong.umji.api.order.persistence.OrderNumberSequenceRepository
import com.buyeong.umji.api.order.persistence.PurchaseOrderEntity
import com.buyeong.umji.api.order.persistence.PurchaseOrderRepository
import com.buyeong.umji.api.persistence.jpa.entity.backbone.DomainPublicEntity
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID

class OrderServiceTest : DescribeSpec({
    val carts = mockk<CartRepository>()
    val orders = mockk<PurchaseOrderRepository>()
    val histories = mockk<OrderStatusHistoryRepository>()
    val sequences = mockk<OrderNumberSequenceRepository>()
    val inventory = mockk<InventoryService>(relaxed = true)
    val service = OrderService(carts, orders, histories, sequences, inventory)
    val account = mockk<AccountEntity>()
    val sku = mockk<ProductSkuEntity>()
    val product = mockk<ProductEntity>()

    beforeTest {
        every { account.id } returns 1L
        every { sku.id } returns 2L
        every { sku.publicId } returns UUID.randomUUID()
        every { sku.salesStatus } returns "ON_SALE"
        every { sku.salePrice } returns 12000L
        every { sku.name } returns "규격 A"
        every { sku.skuCode } returns "SKU-001"
        every { sku.product } returns product
        every { product.name } returns "테스트 상품"
        every { histories.save(any<OrderStatusHistoryEntity>()) } answers { firstArg() }
        every { sequences.findLockedByOrderDate(any()) } returns OrderNumberSequenceEntity().apply { orderDate = java.time.LocalDate.now(java.time.ZoneOffset.UTC) }
    }

    describe("주문 생성") {
        it("장바구니 가격을 스냅샷으로 저장하고 재고를 예약한 뒤 장바구니를 비운다") {
            val cart = CartEntity().apply { this.account = account }
            cart.add(CartItemEntity().apply { this.sku = sku; quantity = 3 })
            every { carts.findLockedByAccountId(1L) } returns cart
            every { orders.saveAndFlush(any<PurchaseOrderEntity>()) } answers {
                firstArg<PurchaseOrderEntity>().also { order ->
                    setPublicId(order, UUID.randomUUID())
                    order.items.forEach { setPublicId(it, UUID.randomUUID()) }
                }
            }

            val response = service.create(account)

            response.status shouldBe "PENDING_PAYMENT"
            response.subtotalAmount shouldBe 36000L
            response.items.single().unitPrice shouldBe 12000L
            response.items.single().quantity shouldBe 3
            cart.items.size shouldBe 0
            verify(exactly = 1) { inventory.reserve(any(), 3, any(), null) }
        }
    }
}) {
    companion object {
        private fun setPublicId(entity: DomainPublicEntity, id: UUID) {
            DomainPublicEntity::class.java.getDeclaredField("publicId").apply { isAccessible = true }.set(entity, id)
        }
    }
}
