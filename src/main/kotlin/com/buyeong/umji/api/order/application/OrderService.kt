package com.buyeong.umji.api.order.application

import com.buyeong.umji.api.cart.application.CartService
import com.buyeong.umji.api.exception.ClientBadRequestException
import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.inventory.application.InventoryService
import com.buyeong.umji.api.notification.application.NotificationEventService
import com.buyeong.umji.api.notification.application.model.NotificationEventType
import com.buyeong.umji.api.order.application.model.OrderCheckoutOptions
import com.buyeong.umji.api.order.application.model.OrderDraft
import com.buyeong.umji.api.order.application.model.OrderItemDraft
import com.buyeong.umji.api.order.application.model.OrderPage
import com.buyeong.umji.api.order.application.model.OrderView
import com.buyeong.umji.api.order.application.model.TaxInvoiceSnapshotDraft
import com.buyeong.umji.api.payment.adapter.BankAccountInstructionsService
import com.buyeong.umji.api.payment.adapter.TaxInvoiceSupplierAdapter
import com.buyeong.umji.api.persistence.jpa.account.BuyerGroupTaxInvoiceJpaEntityService
import com.buyeong.umji.api.persistence.jpa.account.CustomerAccountJpaEntityService
import com.buyeong.umji.api.persistence.jpa.order.OrderCheckoutJpaEntityService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional(readOnly = true)
class OrderService(
    private val checkoutCart: CartService,
    private val inventory: InventoryService,
    private val orders: OrderCheckoutJpaEntityService,
    private val shippingAddresses: CustomerAccountJpaEntityService,
    private val bankAccounts: BankAccountInstructionsService,
    private val notifications: NotificationEventService,
    private val taxInvoiceSuppliers: TaxInvoiceSupplierAdapter,
    private val taxInvoiceBuyers: BuyerGroupTaxInvoiceJpaEntityService,
) {
    fun checkoutOptions(accountPublicId: UUID): OrderCheckoutOptions {
        val buyer = taxInvoiceBuyers.forAccount(accountPublicId)?.let {
            com.buyeong.umji.api.order.application.model.TaxInvoiceBuyer(
                buyerGroupId = it.buyerGroupId,
                businessRegistrationNumber = it.businessRegistrationNumber,
                businessName = it.businessName,
                representativeName = it.representativeName,
                postalCode = it.postalCode,
                address1 = it.address1,
                address2 = it.address2,
                businessIndustry = it.businessIndustry,
                businessItem = it.businessItem,
                email = it.email,
                complete = it.complete,
            )
        }
        val available = taxInvoiceSuppliers.supplier() != null && buyer?.complete == true
        return OrderCheckoutOptions(
            defaultTaxInvoiceRequested = orders.defaultTaxInvoiceRequested(accountPublicId),
            taxInvoiceAvailable = available,
            standardBankAccount = bankAccounts.standard(),
            taxInvoiceBankAccount = bankAccounts.taxInvoice(),
        )
    }

    @Transactional
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
        val invoiceBuyer = taxInvoiceBuyers.forAccount(accountPublicId)?.let {
            com.buyeong.umji.api.order.application.model.TaxInvoiceBuyer(
                buyerGroupId = it.buyerGroupId,
                businessRegistrationNumber = it.businessRegistrationNumber,
                businessName = it.businessName,
                representativeName = it.representativeName,
                postalCode = it.postalCode,
                address1 = it.address1,
                address2 = it.address2,
                businessIndustry = it.businessIndustry,
                businessItem = it.businessItem,
                email = it.email,
                complete = it.complete,
            )
        }
        val canRequestInvoice = invoiceSupplier != null && invoiceBuyer?.complete == true
        val selectedPreference = taxInvoiceRequested ?: (currentDefaultPreference && canRequestInvoice)
        if (selectedPreference && !canRequestInvoice) {
            throw ClientBadRequestException("공급자와 사업자 그룹의 세금계산서 필수 정보를 먼저 입력해야 합니다.")
        }
        val defaultPreference = currentDefaultPreference
        val bankAccount = if (selectedPreference) bankAccounts.taxInvoice() else bankAccounts.standard()
        val lines = checkoutCart.cart(accountPublicId).items.map {
            com.buyeong.umji.api.order.application.model.CheckoutLine(
                it.skuId, it.skuCode, it.productName, it.skuName, it.unitPrice, it.quantity, it.salesStatus, it.salesOfferId, it.channelCode, it.unitsPerSale,
            )
        }
        require(lines.isNotEmpty()) { "장바구니가 비어 있습니다." }
        require(lines.all { it.salesStatus == ON_SALE }) { "판매 중지된 SKU가 포함되어 있습니다." }
        require(lines.map { it.channelCode }.distinct().size == 1) { "한 주문에는 하나의 판매 채널 상품만 포함할 수 있습니다." }

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
                salesOfferId = line.salesOfferId,
                unitsPerSale = line.unitsPerSale,
            )
        }
        items.forEach { Math.multiplyExact(it.quantity, it.unitsPerSale) }
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
                lines.first().channelCode,
            ),
        )
        if (updateDefaultTaxInvoicePreference && selectedPreference != defaultPreference) {
            orders.updateDefaultTaxInvoiceRequested(accountPublicId, selectedPreference)
        }
        saved.items.forEach { item -> inventory.reserve(item.skuId, Math.multiplyExact(item.quantity, item.unitsPerSale), item.reservationKey, null) }
        checkoutCart.clearForCheckout(accountPublicId)
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
