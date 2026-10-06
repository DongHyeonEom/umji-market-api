package com.buyeong.umji.api.order.application

import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.notification.application.model.NotificationEventType
import com.buyeong.umji.api.notification.application.port.`in`.NoOpNotificationEventUseCase
import com.buyeong.umji.api.notification.application.port.`in`.NotificationEventUseCase
import com.buyeong.umji.api.order.application.model.OrderCheckoutOptions
import com.buyeong.umji.api.order.application.model.OrderDraft
import com.buyeong.umji.api.order.application.model.OrderItemDraft
import com.buyeong.umji.api.order.application.model.OrderPage
import com.buyeong.umji.api.order.application.model.OrderView
import com.buyeong.umji.api.order.application.model.TaxInvoiceSnapshotDraft
import com.buyeong.umji.api.order.application.port.out.BankAccountInstructionsPort
import com.buyeong.umji.api.order.application.port.out.CheckoutCartPort
import com.buyeong.umji.api.order.application.port.out.InventoryReservationPort
import com.buyeong.umji.api.order.application.port.out.OrderShippingAddressPort
import com.buyeong.umji.api.order.application.port.out.OrderStorePort
import com.buyeong.umji.api.order.application.port.out.TaxInvoiceBuyerProfilePort
import com.buyeong.umji.api.order.application.port.out.TaxInvoiceSupplierPort
import com.buyeong.umji.api.exception.ClientBadRequestException
import java.time.Instant
import java.util.UUID

class OrderService(
    private val checkoutCart: CheckoutCartPort,
    private val inventory: InventoryReservationPort,
    private val orders: OrderStorePort,
    private val shippingAddresses: OrderShippingAddressPort,
    private val bankAccounts: BankAccountInstructionsPort = object : BankAccountInstructionsPort {
        override fun standard() = com.buyeong.umji.api.order.application.model.BankAccountInstructions("", "", "")
        override fun taxInvoice() = standard()
    },
    private val notifications: NotificationEventUseCase = NoOpNotificationEventUseCase,
    private val taxInvoiceSuppliers: TaxInvoiceSupplierPort = object : TaxInvoiceSupplierPort {
        override fun supplier() = null
    },
    private val taxInvoiceBuyers: TaxInvoiceBuyerProfilePort = object : TaxInvoiceBuyerProfilePort {
        override fun forAccount(accountPublicId: UUID) = null
    },
) {
    fun checkoutOptions(accountPublicId: UUID): OrderCheckoutOptions {
        val buyer = taxInvoiceBuyers.forAccount(accountPublicId)
        val available = taxInvoiceSuppliers.supplier() != null && buyer?.complete == true
        return OrderCheckoutOptions(
            defaultTaxInvoiceRequested = orders.defaultTaxInvoiceRequested(accountPublicId),
            taxInvoiceAvailable = available,
            standardBankAccount = bankAccounts.standard(),
            taxInvoiceBankAccount = bankAccounts.taxInvoice(),
        )
    }

    fun create(
        accountPublicId: UUID,
        shippingAddressPublicId: UUID,
        taxInvoiceRequested: Boolean?,
        updateDefaultTaxInvoicePreference: Boolean,
    ): OrderView {
        val shippingAddress = shippingAddresses.findForAccount(accountPublicId, shippingAddressPublicId)
            ?: throw ItemNotFoundException("구매자 그룹 배송지를 찾을 수 없습니다.")
        val currentDefaultPreference = if (taxInvoiceRequested == null || updateDefaultTaxInvoicePreference) {
            orders.defaultTaxInvoiceRequested(accountPublicId)
        } else {
            taxInvoiceRequested
        }
        val invoiceSupplier = taxInvoiceSuppliers.supplier()
        val invoiceBuyer = taxInvoiceBuyers.forAccount(accountPublicId)
        val canRequestInvoice = invoiceSupplier != null && invoiceBuyer?.complete == true
        val selectedPreference = taxInvoiceRequested ?: (currentDefaultPreference && canRequestInvoice)
        if (selectedPreference && !canRequestInvoice) {
            throw ClientBadRequestException("공급자와 사업자 그룹의 세금계산서 필수 정보를 먼저 입력해야 합니다.")
        }
        val defaultPreference = currentDefaultPreference
        val bankAccount = if (selectedPreference) bankAccounts.taxInvoice() else bankAccounts.standard()
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
        val saved = orders.save(
            OrderDraft(
                accountPublicId, PENDING_PAYMENT, orderedAt, subtotal, subtotal,
                selectedPreference,
                bankAccount.bankName,
                bankAccount.accountNumber,
                bankAccount.accountHolder,
                shippingAddress,
                items,
                if (selectedPreference) TaxInvoiceSnapshotDraft(requireNotNull(invoiceSupplier), requireNotNull(invoiceBuyer)) else null,
            ),
        )
        if (updateDefaultTaxInvoicePreference && selectedPreference != defaultPreference) {
            orders.updateDefaultTaxInvoiceRequested(accountPublicId, selectedPreference)
        }
        saved.items.forEach { item -> inventory.reserve(item.skuId, item.quantity, item.reservationKey, null) }
        checkoutCart.clear(accountPublicId)
        notifications.record(NotificationEventType.ORDER_CREATED, saved.id)
        return saved
    }

    fun create(accountPublicId: UUID, shippingAddressPublicId: UUID): OrderView =
        create(accountPublicId, shippingAddressPublicId, false, false)

    fun list(accountPublicId: UUID, page: Int, size: Int): OrderPage = orders.findAll(accountPublicId, page, size)

    fun detail(accountPublicId: UUID, orderId: UUID): OrderView =
        orders.find(accountPublicId, orderId) ?: throw ItemNotFoundException("주문을 찾을 수 없습니다.")

    private companion object {
        const val PENDING_PAYMENT = "PENDING_PAYMENT"
        const val RESERVED = "RESERVED"
        const val ON_SALE = "ON_SALE"
    }
}
