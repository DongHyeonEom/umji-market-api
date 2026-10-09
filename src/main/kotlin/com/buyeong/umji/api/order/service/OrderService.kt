package com.buyeong.umji.api.order.service

import com.buyeong.umji.api.cart.service.CartService
import com.buyeong.umji.api.exception.ClientBadRequestException
import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.inventory.service.InventoryService
import com.buyeong.umji.api.notification.dto.NotificationEventType
import com.buyeong.umji.api.notification.service.NotificationEventService
import com.buyeong.umji.api.order.dto.AdminPhoneOrderBuyerDto
import com.buyeong.umji.api.order.dto.AdminPhoneOrderLineDto
import com.buyeong.umji.api.order.dto.CheckoutLineDto
import com.buyeong.umji.api.order.dto.OrderCheckoutOptionsDto
import com.buyeong.umji.api.order.dto.OrderDraftDto
import com.buyeong.umji.api.order.dto.OrderItemDraftDto
import com.buyeong.umji.api.order.dto.OrderPageDto
import com.buyeong.umji.api.order.dto.OrderViewDto
import com.buyeong.umji.api.order.dto.ShippingAddressSnapshotDto
import com.buyeong.umji.api.order.dto.TaxInvoiceBuyerDto
import com.buyeong.umji.api.order.dto.TaxInvoiceSnapshotDraftDto
import com.buyeong.umji.api.payment.integration.BankAccountInstructionsService
import com.buyeong.umji.api.payment.integration.TaxInvoiceSupplierService
import com.buyeong.umji.api.persistence.jpa.account.service.AccountJpaEntityService
import com.buyeong.umji.api.persistence.jpa.account.service.CustomerAccountJpaEntityService
import com.buyeong.umji.api.persistence.jpa.account.service.OrganizationJpaEntityService
import com.buyeong.umji.api.persistence.jpa.account.service.OrganizationTaxInvoiceJpaEntityService
import com.buyeong.umji.api.persistence.jpa.catalog.service.CatalogJpaEntityService
import com.buyeong.umji.api.persistence.jpa.order.service.OrderCheckoutJpaEntityService
import com.buyeong.umji.api.sales.service.SalesCommissionService
import com.buyeong.umji.api.util.PhoneNumberHelper
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
    private val taxInvoiceSuppliers: TaxInvoiceSupplierService,
    private val taxInvoiceBuyers: OrganizationTaxInvoiceJpaEntityService,
    private val salesCommissions: SalesCommissionService,
    private val accounts: AccountJpaEntityService,
    private val catalog: CatalogJpaEntityService,
    private val organizations: OrganizationJpaEntityService,
) {
    fun checkoutOptions(accountPublicId: UUID): OrderCheckoutOptionsDto {
        val buyer = taxInvoiceBuyers.forAccount(accountPublicId)?.let {
            com.buyeong.umji.api.order.dto.TaxInvoiceBuyerDto(
                organizationId = it.organizationId,
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
        val sellers = checkoutCart.cart(accountPublicId).items.map { item ->
            if (item.sellerOrganizationId == null) {
                taxInvoiceSuppliers.supplier()
            } else {
                taxInvoiceBuyers.supplierForOrganization(item.sellerOrganizationId)
            }
        }
        val available = sellers.isNotEmpty() && sellers.all { it != null } && buyer?.complete == true
        return OrderCheckoutOptionsDto(
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
    ): List<OrderViewDto> {
        val shippingAddress = shippingAddresses.findForAccount(accountPublicId, shippingAddressPublicId)
            ?: throw ItemNotFoundException("구매자 그룹 배송지를 찾을 수 없습니다.")
        val currentDefaultPreference = if (taxInvoiceRequested == null || updateDefaultTaxInvoicePreference) {
            orders.defaultTaxInvoiceRequested(accountPublicId)
        } else {
            taxInvoiceRequested
        }
        val invoiceBuyer = taxInvoiceBuyers.forAccount(accountPublicId)?.let {
            com.buyeong.umji.api.order.dto.TaxInvoiceBuyerDto(
                organizationId = it.organizationId,
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
        val cart = checkoutCart.cart(accountPublicId)
        val lines = cart.items.map {
            com.buyeong.umji.api.order.dto.CheckoutLineDto(
                it.skuId, it.skuCode, it.productName, it.skuName, it.unitPrice, it.quantity, it.salesStatus,
                it.salesOfferId, it.channelCode, it.unitsPerSale, it.sellerOrganizationId,
            )
        }
        require(lines.isNotEmpty()) { "장바구니가 비어 있습니다." }
        require(lines.all { it.salesStatus == ON_SALE }) { "판매 중지된 SKU가 포함되어 있습니다." }
        require(lines.map { it.channelCode }.distinct().size == 1) { "한 주문에는 하나의 판매 채널 상품만 포함할 수 있습니다." }
        require(lines.mapNotNull { it.sellerOrganizationId }.distinct().all(taxInvoiceBuyers::isSellerBusinessProfileReady)) {
            "판매자의 확인된 사업자 Organization 프로필이 없어 주문할 수 없습니다."
        }
        val supplierByOrganization = lines.map { it.sellerOrganizationId }.distinct().associateWith { organizationId ->
            if (organizationId == null) taxInvoiceSuppliers.supplier() else taxInvoiceBuyers.supplierForOrganization(organizationId)
        }
        val canRequestInvoice = supplierByOrganization.values.all { it != null } && invoiceBuyer?.complete == true
        val selectedPreference = taxInvoiceRequested ?: (currentDefaultPreference && canRequestInvoice)
        if (selectedPreference && !canRequestInvoice) {
            throw ClientBadRequestException("공급자와 사업자 그룹의 세금계산서 필수 정보를 먼저 입력해야 합니다.")
        }
        val defaultPreference = currentDefaultPreference
        val bankAccount = if (selectedPreference) bankAccounts.taxInvoice() else bankAccounts.standard()
        val orderedAt = Instant.now()
        val savedOrders = lines.groupBy { it.sellerOrganizationId }.map { (organizationId, sellerLines) ->
            val items = sellerLines.map { line ->
                val amount = Math.multiplyExact(line.unitPrice, line.quantity.toLong())
                OrderItemDraftDto(
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
            val snapshot = if (selectedPreference) {
                TaxInvoiceSnapshotDraftDto(
                    requireNotNull(supplierByOrganization[organizationId]),
                    requireNotNull(invoiceBuyer),
                )
            } else {
                null
            }
            val savedOrder = orders.save(
                OrderDraftDto(
                    accountPublicId, PENDING_PAYMENT, orderedAt, subtotal, subtotal,
                    selectedPreference,
                    bankAccount.bankName,
                    bankAccount.accountNumber,
                    bankAccount.accountHolder,
                    shippingAddress,
                    items,
                    snapshot,
                    sellerLines.first().channelCode,
                ),
            )
            salesCommissions.snapshotOrder(savedOrder.id, accountPublicId, orderedAt, items.sumOf { it.lineAmount })
            savedOrder
        }
        if (updateDefaultTaxInvoicePreference && selectedPreference != defaultPreference) {
            orders.updateDefaultTaxInvoiceRequested(accountPublicId, selectedPreference)
        }
        savedOrders.forEach { saved ->
            saved.items.forEach { item ->
                inventory.reserve(item.skuId, Math.multiplyExact(item.quantity, item.unitsPerSale), item.reservationKey, null, item.sellerOrganizationId)
            }
        }
        checkoutCart.clearForCheckout(accountPublicId)
        savedOrders.forEach { notifications.record(NotificationEventType.ORDER_CREATED, it.id) }
        return savedOrders
    }

    fun create(accountPublicId: UUID, shippingAddressPublicId: UUID): List<OrderViewDto> =
        create(accountPublicId, shippingAddressPublicId, false, false)

    @Transactional
    fun createAdminPhoneOrder(
        creatorPublicId: UUID,
        buyerPublicId: UUID,
        shippingAddress: ShippingAddressSnapshotDto,
        lines: List<AdminPhoneOrderLineDto>,
        taxInvoiceRequested: Boolean,
    ): List<OrderViewDto> {
        require(lines.isNotEmpty()) { "전화 주문 상품을 한 개 이상 입력해야 합니다." }
        accounts.findByPublicId(buyerPublicId)
            ?.takeIf { it.status == "ACTIVE" && it.phoneNormalized != null }
            ?: throw ItemNotFoundException("활성 구매자 계정을 찾을 수 없습니다.")
        val buyerOrganization = taxInvoiceBuyers.forAccount(buyerPublicId)
        require(buyerOrganization != null) { "활성 구매 Organization에 속한 계정만 전화 주문할 수 있습니다." }
        accounts.findByPublicId(creatorPublicId)
            ?.takeIf { it.status == "ACTIVE" }
            ?: throw ItemNotFoundException("관리자 계정을 찾을 수 없습니다.")

        val checkoutLines = lines.map { requested ->
            require(requested.quantity in 1..9999) { "상품 수량은 1개 이상 9999개 이하여야 합니다." }
            val offer = catalog.salesOffer(requested.salesOfferId)
                ?.takeIf {
                    it.salesStatus == ON_SALE &&
                        it.salesChannel.code == WHOLESALE &&
                        it.productSku.salesStatus == ON_SALE &&
                        it.productSku.product.salesStatus == ON_SALE &&
                        it.productSku.product.displayStatus == DISPLAYED &&
                        it.productSku.product.deletedAt == null
                }
                ?: throw ItemNotFoundException("판매 중인 상품 오퍼를 찾을 수 없습니다.")
            val sku = offer.productSku
            CheckoutLineDto(
                skuId = requireNotNull(sku.publicId),
                skuCode = sku.skuCode,
                productName = sku.product.name,
                skuName = sku.name,
                unitPrice = offer.salePrice,
                quantity = requested.quantity,
                salesStatus = sku.salesStatus,
                salesOfferId = requireNotNull(offer.publicId),
                channelCode = offer.salesChannel.code,
                unitsPerSale = offer.unitsPerSale,
                sellerOrganizationId = offer.organization?.publicId,
            )
        }
        require(checkoutLines.map { it.salesOfferId }.distinct().size == checkoutLines.size) { "같은 상품 오퍼는 한 줄로 합산해야 합니다." }
        require(checkoutLines.map { it.channelCode }.distinct().size == 1) { "한 주문에는 하나의 판매 채널 상품만 포함할 수 있습니다." }
        require(checkoutLines.mapNotNull { it.sellerOrganizationId }.distinct().all(taxInvoiceBuyers::isSellerBusinessProfileReady)) {
            "판매자의 확인된 사업자 Organization 프로필이 없어 주문할 수 없습니다."
        }
        val invoiceBuyer = buyerOrganization.let {
            com.buyeong.umji.api.order.dto.TaxInvoiceBuyerDto(
                it.organizationId, it.businessRegistrationNumber, it.businessName, it.representativeName,
                it.postalCode, it.address1, it.address2, it.businessIndustry, it.businessItem, it.email, it.complete,
            )
        }
        val supplierByOrganization = checkoutLines.map { it.sellerOrganizationId }.distinct().associateWith { organizationId ->
            if (organizationId == null) taxInvoiceSuppliers.supplier() else taxInvoiceBuyers.supplierForOrganization(organizationId)
        }
        if (taxInvoiceRequested && (invoiceBuyer.complete != true || supplierByOrganization.values.any { it == null })) {
            throw ClientBadRequestException("공급자와 구매자의 세금계산서 필수 정보가 완성된 주문만 요청할 수 있습니다.")
        }
        val bankAccount = if (taxInvoiceRequested) bankAccounts.taxInvoice() else bankAccounts.standard()
        val orderedAt = Instant.now()
        val savedOrders = checkoutLines.groupBy { it.sellerOrganizationId }.map { (organizationId, sellerLines) ->
            val items = sellerLines.map { line ->
                val amount = Math.multiplyExact(line.unitPrice, line.quantity.toLong())
                Math.multiplyExact(line.quantity, line.unitsPerSale)
                OrderItemDraftDto(
                    skuId = line.skuId, productName = line.productName, skuName = line.skuName, skuCode = line.skuCode,
                    unitPrice = line.unitPrice, quantity = line.quantity, lineAmount = amount,
                    reservationKey = UUID.randomUUID(), status = RESERVED, salesOfferId = line.salesOfferId,
                    unitsPerSale = line.unitsPerSale,
                )
            }
            val total = items.sumOf { it.lineAmount }
            val snapshot = if (taxInvoiceRequested) {
                TaxInvoiceSnapshotDraftDto(
                    requireNotNull(supplierByOrganization[organizationId]),
                    invoiceBuyer,
                )
            } else {
                null
            }
            val saved = orders.save(
                OrderDraftDto(
                    accountId = buyerPublicId, status = PENDING_PAYMENT, orderedAt = orderedAt,
                    subtotalAmount = total, totalAmount = total, taxInvoiceRequested = taxInvoiceRequested,
                    depositBankName = bankAccount.bankName, depositAccountNumber = bankAccount.accountNumber,
                    depositAccountHolder = bankAccount.accountHolder, shippingAddress = shippingAddress,
                    items = items, taxInvoiceSnapshot = snapshot, channelCode = sellerLines.first().channelCode,
                    createdByAccountId = creatorPublicId, orderSource = "ADMIN_PHONE",
                ),
            )
            salesCommissions.snapshotOrder(saved.id, buyerPublicId, orderedAt, total)
            saved
        }
        savedOrders.forEach { saved ->
            saved.items.forEach { item ->
                inventory.reserve(item.skuId, Math.multiplyExact(item.quantity, item.unitsPerSale), item.reservationKey, null, item.sellerOrganizationId)
            }
            notifications.record(NotificationEventType.ORDER_CREATED, saved.id)
        }
        return savedOrders
    }

    fun findAdminPhoneOrderBuyer(phone: String): AdminPhoneOrderBuyerDto? {
        val normalized = PhoneNumberHelper.normalizeMobilePhoneNumber(phone)
        val account = accounts.findByPhoneNormalized(normalized)?.takeIf { it.status == "ACTIVE" } ?: return null
        val organization = organizations.activeBuyerForAccountPublicId(requireNotNull(account.publicId)) ?: return null
        return AdminPhoneOrderBuyerDto(
            requireNotNull(account.publicId),
            account.name,
            account.phone ?: normalized,
            requireNotNull(organization.publicId),
            organization.displayName,
        )
    }

    fun list(accountPublicId: UUID, page: Int, size: Int): OrderPageDto = orders.findAll(accountPublicId, page, size)

    fun detail(accountPublicId: UUID, orderId: UUID): OrderViewDto =
        orders.find(accountPublicId, orderId) ?: throw ItemNotFoundException("주문을 찾을 수 없습니다.")

    private companion object {
        const val PENDING_PAYMENT = "PENDING_PAYMENT"
        const val RESERVED = "RESERVED"
        const val ON_SALE = "ON_SALE"
        const val WHOLESALE = "WHOLESALE"
        const val DISPLAYED = "DISPLAYED"
    }
}