package com.buyeong.umji.api.order.application

import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.order.application.model.OrderDraft
import com.buyeong.umji.api.order.application.model.OrderItemDraft
import com.buyeong.umji.api.order.application.model.OrderPage
import com.buyeong.umji.api.order.application.model.OrderView
import com.buyeong.umji.api.order.application.port.out.CheckoutCartPort
import com.buyeong.umji.api.order.application.port.out.InventoryReservationPort
import com.buyeong.umji.api.order.application.port.out.OrderStorePort
import com.buyeong.umji.api.order.application.port.`in`.OrderUseCase
import java.time.Instant
import java.util.UUID

class OrderService(
    private val checkoutCart: CheckoutCartPort,
    private val inventory: InventoryReservationPort,
    private val orders: OrderStorePort,
) : OrderUseCase {
    override fun create(accountPublicId: UUID): OrderView {
        val lines = checkoutCart.linesForCheckout(accountPublicId)
        require(lines.isNotEmpty()) { "장바구니가 비어 있습니다." }
        require(lines.all { it.salesStatus == ON_SALE }) { "판매 중지된 SKU가 포함되어 있습니다." }

        val orderedAt = Instant.now()
        val items = lines.map { line ->
            val amount = Math.multiplyExact(line.unitPrice, line.quantity.toLong())
            OrderItemDraft(
                skuId = line.skuId,
                productName = line.productName,
                skuName = line.skuName,
                skuCode = line.skuCode,
                unitPrice = line.unitPrice,
                quantity = line.quantity,
                lineAmount = amount,
                reservationKey = UUID.randomUUID(),
                status = RESERVED,
            )
        }
        val subtotal = items.sumOf { it.lineAmount }
        val saved = orders.save(OrderDraft(accountPublicId, PENDING_PAYMENT, orderedAt, subtotal, subtotal, items))
        saved.items.forEach { item -> inventory.reserve(item.skuId, item.quantity, item.reservationKey, null) }
        checkoutCart.clear(accountPublicId)
        return saved
    }

    override fun list(accountPublicId: UUID, page: Int, size: Int): OrderPage = orders.findAll(accountPublicId, page, size)

    override fun detail(accountPublicId: UUID, orderId: UUID): OrderView =
        orders.find(accountPublicId, orderId) ?: throw ItemNotFoundException("주문을 찾을 수 없습니다.")

    private companion object {
        const val PENDING_PAYMENT = "PENDING_PAYMENT"
        const val RESERVED = "RESERVED"
        const val ON_SALE = "ON_SALE"
    }
}
