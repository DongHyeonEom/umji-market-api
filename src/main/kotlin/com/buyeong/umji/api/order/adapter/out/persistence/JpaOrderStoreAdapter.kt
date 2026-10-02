package com.buyeong.umji.api.order.adapter.out.persistence

import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.order.application.model.OrderDraft
import com.buyeong.umji.api.order.application.model.OrderItemDraft
import com.buyeong.umji.api.order.application.model.OrderItemView
import com.buyeong.umji.api.order.application.model.OrderPage
import com.buyeong.umji.api.order.application.model.OrderView
import com.buyeong.umji.api.order.application.port.out.OrderStorePort
import com.buyeong.umji.api.persistence.jpa.account.AccountJpaEntityService
import com.buyeong.umji.api.persistence.jpa.account.BuyerGroupJpaEntityService
import com.buyeong.umji.api.persistence.jpa.catalog.CatalogJpaEntityService
import com.buyeong.umji.api.persistence.jpa.order.OrderCancellationHistoryRepository
import com.buyeong.umji.api.persistence.jpa.order.OrderItemEntity
import com.buyeong.umji.api.persistence.jpa.order.OrderJpaEntityService
import com.buyeong.umji.api.persistence.jpa.order.OrderNumberSequenceEntity
import com.buyeong.umji.api.persistence.jpa.order.OrderPaymentJpaEntityService
import com.buyeong.umji.api.persistence.jpa.order.OrderShipmentJpaEntityService
import com.buyeong.umji.api.persistence.jpa.order.OrderStatusHistoryEntity
import com.buyeong.umji.api.persistence.jpa.order.PurchaseOrderEntity
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

@Component
class JpaOrderStoreAdapter(
    private val accounts: AccountJpaEntityService,
    private val buyerGroups: BuyerGroupJpaEntityService,
    private val catalog: CatalogJpaEntityService,
    private val orders: OrderJpaEntityService,
    private val payments: OrderPaymentJpaEntityService,
    private val shipments: OrderShipmentJpaEntityService,
    private val cancellationHistory: OrderCancellationHistoryRepository,
) : OrderStorePort {
    override fun defaultTaxInvoiceRequested(accountId: UUID): Boolean =
        account(accountId).defaultTaxInvoiceRequested

    override fun updateDefaultTaxInvoiceRequested(accountId: UUID, requested: Boolean) {
        val account = account(accountId)
        account.defaultTaxInvoiceRequested = requested
        accounts.save(account)
    }

    override fun save(draft: OrderDraft): OrderView {
        val account = account(draft.accountId)
        val buyerGroup = buyerGroups.ensureForAccount(draft.accountId)
        val order = PurchaseOrderEntity().apply {
            this.account = account
            this.buyerGroup = buyerGroup
            orderNumber = nextOrderNumber(draft.orderedAt)
            status = draft.status
            orderedAt = draft.orderedAt
            subtotalAmount = draft.subtotalAmount
            totalAmount = draft.totalAmount
            taxInvoiceRequested = draft.taxInvoiceRequested
            depositBankName = draft.depositBankName
            depositAccountNumber = draft.depositAccountNumber
            depositAccountHolder = draft.depositAccountHolder
            shippingRecipientName = draft.shippingAddress.recipientName
            shippingRecipientPhone = draft.shippingAddress.recipientPhone
            shippingPostalCode = draft.shippingAddress.postalCode
            shippingAddress1 = draft.shippingAddress.address1
            shippingAddress2 = draft.shippingAddress.address2
        }
        draft.items.forEach { item -> order.add(item.toEntity()) }
        val saved = orders.saveAndFlush(order)
        saved.payment = payments.initialize(saved)
        saved.shipment = shipments.initialize(saved)
        orders.saveHistory(
            OrderStatusHistoryEntity().apply {
                this.order = saved
                toStatus = draft.status
            },
        )
        return saved.toView()
    }

    override fun findAll(accountId: UUID, page: Int, size: Int): OrderPage {
        val result = orders.findAll(buyerGroupInternalId(accountId), PageRequest.of(page, size, Sort.by("orderedAt").descending()))
        return OrderPage(result.content.map { it.toView() }, result.number, result.size, result.totalElements, result.totalPages)
    }

    override fun find(accountId: UUID, orderId: UUID): OrderView? =
        orders.findWithItems(orderId, buyerGroupInternalId(accountId))?.toView()

    private fun buyerGroupInternalId(accountPublicId: UUID): Long =
        requireNotNull(buyerGroups.activeForAccountPublicId(accountPublicId)?.id) { "계정의 활성 구매자 그룹을 찾을 수 없습니다." }

    private fun account(publicId: UUID) = accounts.findByPublicId(publicId) ?: throw ItemNotFoundException("계정을 찾을 수 없습니다.")

    private fun nextOrderNumber(orderedAt: java.time.Instant): String {
        val date = orderedAt.atZone(ZoneOffset.UTC).toLocalDate()
        val sequence = orders.lockedSequence(date) ?: orders.saveAndFlushSequence(
            OrderNumberSequenceEntity().apply { orderDate = date },
        )
        sequence.lastValue += 1
        return "UMJ-${ORDER_DATE.format(orderedAt)}-${sequence.lastValue.toString().padStart(6, '0')}"
    }

    private fun OrderItemDraft.toEntity() = OrderItemEntity().apply {
        sku = catalog.sku(skuId) ?: throw ItemNotFoundException("SKU를 찾을 수 없습니다.")
        productName = this@toEntity.productName
        skuName = this@toEntity.skuName
        skuCode = this@toEntity.skuCode
        unitPrice = this@toEntity.unitPrice
        quantity = this@toEntity.quantity
        lineAmount = this@toEntity.lineAmount
        reservationKey = this@toEntity.reservationKey
        status = this@toEntity.status
    }

    private fun PurchaseOrderEntity.toView() = OrderView(
        id = requireNotNull(publicId),
        orderNumber = orderNumber,
        status = status,
        subtotalAmount = subtotalAmount,
        totalAmount = totalAmount,
        orderedAt = orderedAt,
        items = items.map { item ->
            OrderItemView(
                id = requireNotNull(item.publicId), skuId = requireNotNull(item.sku.publicId), reservationKey = item.reservationKey,
                productName = item.productName, skuName = item.skuName, skuCode = item.skuCode,
                unitPrice = item.unitPrice, quantity = item.quantity, lineAmount = item.lineAmount, status = item.status,
            )
        },
        paymentMethod = payment.paymentMethod,
        paymentStatus = payment.status,
        taxInvoiceRequested = taxInvoiceRequested,
        depositBankName = depositBankName,
        depositAccountNumber = depositAccountNumber,
        depositAccountHolder = depositAccountHolder,
        shippingStatus = shipment.status,
        carrierCode = shipment.carrierCode,
        trackingNumber = shipment.trackingNumber,
        cancellationRequestStatus = cancellationHistory.findLatestStatus(requireNotNull(publicId), PageRequest.of(0, 1)).firstOrNull(),
        orderedByName = account.name,
        orderedByPhoneSuffix = account.phone?.filter(Char::isDigit)?.takeLast(4)?.takeIf(String::isNotEmpty),
        shippingRecipientName = shippingRecipientName,
        shippingRecipientPhone = shippingRecipientPhone,
        shippingPostalCode = shippingPostalCode,
        shippingAddress1 = shippingAddress1,
        shippingAddress2 = shippingAddress2,
    )

    private companion object {
        val ORDER_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC)
    }
}