package com.buyeong.umji.api.persistence.jpa.order.service

import com.buyeong.umji.api.exception.ClientBadRequestException
import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.order.model.OrderDraft
import com.buyeong.umji.api.order.model.OrderItemDraft
import com.buyeong.umji.api.order.model.OrderItemView
import com.buyeong.umji.api.order.model.OrderPage
import com.buyeong.umji.api.order.model.OrderView
import com.buyeong.umji.api.order.model.TaxInvoiceBuyer
import com.buyeong.umji.api.order.model.TaxInvoiceSnapshot
import com.buyeong.umji.api.order.model.TaxInvoiceSupplier
import com.buyeong.umji.api.persistence.jpa.account.repository.OrganizationMemberRepository
import com.buyeong.umji.api.persistence.jpa.account.service.AccountJpaEntityService
import com.buyeong.umji.api.persistence.jpa.account.service.OrganizationJpaEntityService
import com.buyeong.umji.api.persistence.jpa.catalog.repository.SalesOfferRepository
import com.buyeong.umji.api.persistence.jpa.catalog.service.CatalogJpaEntityService
import com.buyeong.umji.api.persistence.jpa.order.entity.OrderItemEntity
import com.buyeong.umji.api.persistence.jpa.order.entity.OrderNumberSequenceEntity
import com.buyeong.umji.api.persistence.jpa.order.entity.OrderStatusHistoryEntity
import com.buyeong.umji.api.persistence.jpa.order.entity.PurchaseOrderEntity
import com.buyeong.umji.api.persistence.jpa.order.entity.PurchaseOrderTaxInvoiceEntity
import com.buyeong.umji.api.persistence.jpa.order.repository.OrderCancellationHistoryRepository
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class OrderCheckoutJpaEntityService(
    private val accounts: AccountJpaEntityService,
    private val organizationMembers: OrganizationMemberRepository,
    private val organizations: OrganizationJpaEntityService,
    private val catalog: CatalogJpaEntityService,
    private val salesOffers: SalesOfferRepository,
    private val orders: OrderJpaEntityService,
    private val payments: OrderPaymentJpaEntityService,
    private val shipments: OrderShipmentJpaEntityService,
    private val cancellationHistory: OrderCancellationHistoryRepository,
) {
    fun defaultTaxInvoiceRequested(accountId: UUID): Boolean =
        organizations.defaultTaxInvoiceRequestedForAccount(accountId)

    @Transactional
    fun updateDefaultTaxInvoiceRequested(accountId: UUID, requested: Boolean) {
        organizations.updateDefaultTaxInvoiceRequestedForAccount(accountId, requested)
    }

    @Transactional
    fun save(draft: OrderDraft): OrderView {
        require(draft.taxInvoiceRequested == (draft.taxInvoiceSnapshot != null)) {
            "세금계산서 발행 요청과 세금계산서 snapshot이 일치하지 않습니다."
        }
        val account = account(draft.accountId)
        val creator = account(draft.createdByAccountId)
        val organization = organizationMembers.findFirstByAccount_IdAndStatus(requireNotNull(account.id), "ACTIVE")?.organization
            ?.takeIf { it.status == "ACTIVE" }
            ?: throw ClientBadRequestException("주문 전 개인 또는 사업자 그룹 등록이 필요합니다.")
        val order = PurchaseOrderEntity().apply {
            this.account = account
            createdByAccount = creator
            orderSource = draft.orderSource
            this.organization = organization
            salesChannelCode = draft.channelCode
            orderNumber = nextOrderNumber(draft.orderedAt)
            status = draft.status
            orderedAt = draft.orderedAt
            subtotalAmount = draft.subtotalAmount
            totalAmount = draft.totalAmount
            depositBankName = draft.depositBankName
            depositAccountNumber = draft.depositAccountNumber
            depositAccountHolder = draft.depositAccountHolder
            shippingRecipientName = draft.shippingAddress.recipientName
            shippingRecipientPhone = draft.shippingAddress.recipientPhone
            shippingPostalCode = draft.shippingAddress.postalCode
            shippingAddress1 = draft.shippingAddress.address1
            shippingAddress2 = draft.shippingAddress.address2
        }
        draft.taxInvoiceSnapshot?.let { snapshot ->
            order.taxInvoice = PurchaseOrderTaxInvoiceEntity().apply {
                this.order = order
                status = WAITING_FOR_SHIPMENT
                supplierRegistrationNumber = snapshot.supplier.businessRegistrationNumber
                supplierBusinessName = snapshot.supplier.businessName
                supplierName = snapshot.supplier.representativeName
                supplierAddress = snapshot.supplier.businessAddress
                supplierIndustry = snapshot.supplier.businessIndustry
                supplierItem = snapshot.supplier.businessItem
                supplierEmail = snapshot.supplier.email
                buyerRegistrationNumber = snapshot.buyer.businessRegistrationNumber
                buyerBusinessName = snapshot.buyer.businessName
                buyerName = snapshot.buyer.representativeName
                buyerPostalCode = snapshot.buyer.postalCode
                buyerAddress1 = snapshot.buyer.address1
                buyerAddress2 = snapshot.buyer.address2
                buyerIndustry = snapshot.buyer.businessIndustry
                buyerItem = snapshot.buyer.businessItem
                buyerEmail = snapshot.buyer.email
            }
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

    fun findAll(accountId: UUID, page: Int, size: Int): OrderPage {
        val result = orders.findAll(organizationInternalId(accountId), PageRequest.of(page, size, Sort.by("orderedAt").descending()))
        return OrderPage(result.content.map { it.toView() }, result.number, result.size, result.totalElements, result.totalPages)
    }

    fun find(accountId: UUID, orderId: UUID): OrderView? =
        orders.findWithItems(orderId, organizationInternalId(accountId))?.toView()

    private fun organizationInternalId(accountPublicId: UUID): Long =
        requireNotNull(organizations.activeBuyerForAccountPublicId(accountPublicId)?.id) { "계정의 활성 구매 Organization을 찾을 수 없습니다." }

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
        salesOffer = salesOffers.findByPublicId(salesOfferId) ?: throw ItemNotFoundException("판매 오퍼를 찾을 수 없습니다.")
        productName = this@toEntity.productName
        skuName = this@toEntity.skuName
        skuCode = this@toEntity.skuCode
        unitPrice = this@toEntity.unitPrice
        quantity = this@toEntity.quantity
        unitsPerSale = this@toEntity.unitsPerSale
        lineAmount = this@toEntity.lineAmount
        reservationKey = this@toEntity.reservationKey
        status = this@toEntity.status
    }

    private fun PurchaseOrderEntity.toView() = OrderView(
        id = requireNotNull(publicId),
        orderNumber = orderNumber,
        status = status,
        channelCode = salesChannelCode,
        subtotalAmount = subtotalAmount,
        totalAmount = totalAmount,
        orderedAt = orderedAt,
        items = items.map { item ->
            OrderItemView(
                id = requireNotNull(item.publicId), skuId = requireNotNull(item.sku.publicId), reservationKey = item.reservationKey,
                productName = item.productName, skuName = item.skuName, skuCode = item.skuCode,
                unitPrice = item.unitPrice, quantity = item.quantity, lineAmount = item.lineAmount, status = item.status,
                salesOfferId = requireNotNull(item.salesOffer.publicId),
                unitsPerSale = item.unitsPerSale,
                sellerOrganizationId = item.salesOffer.organization?.publicId,
            )
        },
        paymentMethod = payment.paymentMethod,
        paymentStatus = payment.status,
        taxInvoiceRequested = taxInvoice != null,
        depositBankName = depositBankName,
        depositAccountNumber = depositAccountNumber,
        depositAccountHolder = depositAccountHolder,
        shippingStatus = shipment.status,
        carrierCode = shipment.carrierCode,
        trackingNumber = shipment.trackingNumber,
        cancellationRequestStatus = cancellationHistory.findLatestStatus(requireNotNull(publicId), PageRequest.of(0, 1)).firstOrNull(),
        orderedByName = account.name.firstOrNull()?.let { first -> first + "*".repeat((account.name.length - 1).coerceAtLeast(1)) },
        orderedByPhoneSuffix = account.phone?.filter(Char::isDigit)?.takeLast(4)?.takeIf(String::isNotEmpty),
        shippingRecipientName = shippingRecipientName,
        shippingRecipientPhone = shippingRecipientPhone,
        shippingPostalCode = shippingPostalCode,
        shippingAddress1 = shippingAddress1,
        shippingAddress2 = shippingAddress2,
        taxInvoiceSnapshot = toTaxInvoiceSnapshot(),
        sellerOrganizationId = items.mapNotNull { it.salesOffer.organization?.publicId }.distinct().singleOrNull(),
    )

    private fun PurchaseOrderEntity.toTaxInvoiceSnapshot(): TaxInvoiceSnapshot? {
        val invoice = taxInvoice ?: return null
        if (invoice.status !in setOf(WAITING_FOR_SHIPMENT, READY_FOR_ISSUANCE, MANUALLY_ISSUED)) return null
        return TaxInvoiceSnapshot(
            status = invoice.status,
            supplier = TaxInvoiceSupplier(
                requireNotNull(invoice.supplierRegistrationNumber),
                requireNotNull(invoice.supplierBusinessName),
                requireNotNull(invoice.supplierName),
                requireNotNull(invoice.supplierAddress),
                requireNotNull(invoice.supplierIndustry),
                requireNotNull(invoice.supplierItem),
                requireNotNull(invoice.supplierEmail),
            ),
            buyer = TaxInvoiceBuyer(
                organizationId = requireNotNull(organization.publicId),
                businessRegistrationNumber = invoice.buyerRegistrationNumber,
                businessName = invoice.buyerBusinessName,
                representativeName = invoice.buyerName,
                postalCode = invoice.buyerPostalCode,
                address1 = invoice.buyerAddress1,
                address2 = invoice.buyerAddress2,
                businessIndustry = invoice.buyerIndustry,
                businessItem = invoice.buyerItem,
                email = invoice.buyerEmail,
                complete = true,
            ),
            writtenDate = invoice.writtenDate,
            supplyDate = invoice.supplyDate,
            supplyAmount = items.sumOf { it.lineAmount },
            approvalNumber = invoice.invoiceApprovalNumber,
            issuedAt = invoice.issuedAt,
            taxAmount = invoice.taxAmount,
            totalAmount = invoice.totalAmount,
        )
    }

    private companion object {
        val ORDER_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC)
        const val WAITING_FOR_SHIPMENT = "WAITING_FOR_SHIPMENT"
        const val READY_FOR_ISSUANCE = "READY_FOR_ISSUANCE"
        const val MANUALLY_ISSUED = "MANUALLY_ISSUED"
    }
}
