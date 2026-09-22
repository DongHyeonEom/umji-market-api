package com.buyeong.umji.api.order.service

import com.buyeong.umji.api.account.persistence.AccountEntity
import com.buyeong.umji.api.cart.persistence.CartRepository
import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.inventory.service.InventoryService
import com.buyeong.umji.api.order.model.OrderItemResponse
import com.buyeong.umji.api.order.model.OrderPageResponse
import com.buyeong.umji.api.order.model.OrderResponse
import com.buyeong.umji.api.order.persistence.OrderItemEntity
import com.buyeong.umji.api.order.persistence.OrderNumberSequenceEntity
import com.buyeong.umji.api.order.persistence.OrderNumberSequenceRepository
import com.buyeong.umji.api.order.persistence.OrderStatusHistoryEntity
import com.buyeong.umji.api.order.persistence.OrderStatusHistoryRepository
import com.buyeong.umji.api.order.persistence.PurchaseOrderEntity
import com.buyeong.umji.api.order.persistence.PurchaseOrderRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
class OrderService(
    private val carts: CartRepository,
    private val orders: PurchaseOrderRepository,
    private val statusHistories: OrderStatusHistoryRepository,
    private val orderNumberSequences: OrderNumberSequenceRepository,
    private val inventory: InventoryService,
) {
    @Transactional
    fun create(account: AccountEntity): OrderResponse {
        val cart = carts.findLockedByAccountId(requireNotNull(account.id)) ?: throw ItemNotFoundException("장바구니가 비어 있습니다.")
        require(cart.items.isNotEmpty()) { "장바구니가 비어 있습니다." }
        val order = PurchaseOrderEntity().apply {
            this.account = account
            status = PENDING_PAYMENT
            orderedAt = Instant.now()
            orderNumber = orderNumber(orderedAt)
        }
        cart.items.forEach { cartItem ->
            val sku = cartItem.sku
            require(sku.salesStatus == ON_SALE) { "판매 중지된 SKU가 포함되어 있습니다." }
            val amount = Math.multiplyExact(sku.salePrice, cartItem.quantity.toLong())
            order.add(OrderItemEntity().apply {
                this.sku = sku
                productName = sku.product.name
                skuName = sku.name
                skuCode = sku.skuCode
                unitPrice = sku.salePrice
                quantity = cartItem.quantity
                lineAmount = amount
                reservationKey = UUID.randomUUID()
                status = RESERVED
            })
        }
        order.subtotalAmount = order.items.sumOf { it.lineAmount }
        order.totalAmount = order.subtotalAmount
        val saved = orders.saveAndFlush(order)
        saved.items.forEach { item -> inventory.reserve(requireNotNull(item.sku.publicId), item.quantity, item.reservationKey, null) }
        statusHistories.save(OrderStatusHistoryEntity().apply { this.order = saved; toStatus = PENDING_PAYMENT })
        cart.items.clear()
        return response(saved)
    }

    @Transactional(readOnly = true)
    fun list(account: AccountEntity, page: Int, size: Int): OrderPageResponse {
        val result = orders.findAllByAccountId(requireNotNull(account.id), PageRequest.of(page, size, Sort.by("orderedAt").descending()))
        return OrderPageResponse(result.content.map(::response), result.number, result.size, result.totalElements, result.totalPages)
    }

    @Transactional(readOnly = true)
    fun detail(account: AccountEntity, orderId: UUID): OrderResponse =
        response(orders.findWithItemsByPublicIdAndAccountId(orderId, requireNotNull(account.id)) ?: throw ItemNotFoundException("주문을 찾을 수 없습니다."))

    private fun response(order: PurchaseOrderEntity) = OrderResponse(
        requireNotNull(order.publicId), order.orderNumber, order.status, order.subtotalAmount, order.totalAmount, order.orderedAt,
        order.items.map { item ->
            OrderItemResponse(requireNotNull(item.publicId), requireNotNull(item.sku.publicId), item.productName, item.skuName, item.skuCode,
                item.unitPrice, item.quantity, item.lineAmount, item.status)
        },
    )

    private fun orderNumber(orderedAt: Instant): String {
        val date = orderedAt.atZone(ZoneOffset.UTC).toLocalDate()
        val sequence = orderNumberSequences.findLockedByOrderDate(date) ?: orderNumberSequences.saveAndFlush(
            OrderNumberSequenceEntity().apply { orderDate = date },
        )
        sequence.lastValue += 1
        return "UMJ-${ORDER_DATE.format(orderedAt)}-${sequence.lastValue.toString().padStart(6, '0')}"
    }

    private companion object {
        const val PENDING_PAYMENT = "PENDING_PAYMENT"
        const val RESERVED = "RESERVED"
        const val ON_SALE = "ON_SALE"
        val ORDER_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC)
    }
}
