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
        account(accountId).defaultTaxInvoiceRequested

    @Transactional
    fun updateDefaultTaxInvoiceRequested(accountId: UUID, requested: Boolean) {
        val account = account(accountId)
        account.defaultTaxInvoiceRequested = requested
        accounts.save(account)
    }

    @Transactional
    fun save(draft: OrderDraft): OrderView {
        val account = account(draft.accountId)
        val organization = organizationMembers.findFirstByAccount_IdAndStatus(requireNotNull(account.id), "ACTIVE")?.organization
            ?.takeIf { it.status == "ACTIVE" }
            ?: throw ClientBadRequestException("주문 전 개인 또는 사업자 그룹 등록이 필요합니다.")
        val order = PurchaseOrderEntity().apply {
            this.account = account
            this.organization = organization
            salesChannelCode = draft.channelCode
            orderNumber = nextOrderNumber(draft.orderedAt)
            status = draft.status
            orderedAt = draft.orderedAt
            subtotalAmount = draft.subtotalAmount
            totalAmount = draft.totalAmount
            taxInvoiceRequested = draft.taxInvoiceRequested
            draft.taxInvoiceSnapshot?.let { snapshot ->
                taxInvoiceStatus = WAITING_FOR_SHIPMENT
                taxInvoiceSupplierRegistrationNumber = snapshot.supplier.businessRegistrationNumber
                taxInvoiceSupplierBusinessName = snapshot.supplier.businessName
                taxInvoiceSupplierName = snapshot.supplier.representativeName
                taxInvoiceSupplierAddress = snapshot.supplier.businessAddress
                taxInvoiceSupplierIndustry = snapshot.supplier.businessIndustry
                taxInvoiceSupplierItem = snapshot.supplier.businessItem
                taxInvoiceSupplierEmail = snapshot.supplier.email
                taxInvoiceBuyerRegistrationNumber = snapshot.buyer.businessRegistrationNumber
                taxInvoiceBuyerBusinessName = snapshot.buyer.businessName
                taxInvoiceBuyerName = snapshot.buyer.representativeName
                taxInvoiceBuyerPostalCode = snapshot.buyer.postalCode
                taxInvoiceBuyerAddress1 = snapshot.buyer.address1
                taxInvoiceBuyerAddress2 = snapshot.buyer.address2
                taxInvoiceBuyerIndustry = snapshot.buyer.businessIndustry
                taxInvoiceBuyerItem = snapshot.buyer.businessItem
                taxInvoiceBuyerEmail = snapshot.buyer.email
            }
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
        orderedByName = account.name.firstOrNull()?.let { first -> first + "*".repeat((account.name.length - 1).coerceAtLeast(1)) },
        orderedByPhoneSuffix = account.phone?.filter(Char::isDigit)?.takeLast(4)?.takeIf(String::isNotEmpty),
        shippingRecipientName = shippingRecipientName,
        shippingRecipientPhone = shippingRecipientPhone,
        shippingPostalCode = shippingPostalCode,
        shippingAddress1 = shippingAddress1,
        shippingAddress2 = shippingAddress2,
        taxInvoiceSnapshot = toTaxInvoiceSnapshot(),
    )

    private fun PurchaseOrderEntity.toTaxInvoiceSnapshot(): TaxInvoiceSnapshot? {
        if (taxInvoiceStatus !in setOf(WAITING_FOR_SHIPMENT, READY_FOR_ISSUANCE)) return null
        return TaxInvoiceSnapshot(
            status = requireNotNull(taxInvoiceStatus),
            supplier = TaxInvoiceSupplier(
                requireNotNull(taxInvoiceSupplierRegistrationNumber),
                requireNotNull(taxInvoiceSupplierBusinessName),
                requireNotNull(taxInvoiceSupplierName),
                requireNotNull(taxInvoiceSupplierAddress),
                requireNotNull(taxInvoiceSupplierIndustry),
                requireNotNull(taxInvoiceSupplierItem),
                requireNotNull(taxInvoiceSupplierEmail),
            ),
            buyer = TaxInvoiceBuyer(
                organizationId = requireNotNull(organization.publicId),
                businessRegistrationNumber = taxInvoiceBuyerRegistrationNumber,
                businessName = taxInvoiceBuyerBusinessName,
                representativeName = taxInvoiceBuyerName,
                postalCode = taxInvoiceBuyerPostalCode,
                address1 = taxInvoiceBuyerAddress1,
                address2 = taxInvoiceBuyerAddress2,
                businessIndustry = taxInvoiceBuyerIndustry,
                businessItem = taxInvoiceBuyerItem,
                email = taxInvoiceBuyerEmail,
                complete = true,
            ),
            writtenDate = taxInvoiceWrittenDate,
            supplyDate = taxInvoiceSupplyDate,
            supplyAmount = items.sumOf { it.lineAmount },
        )
    }

    private companion object {
        val ORDER_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC)
        const val WAITING_FOR_SHIPMENT = "WAITING_FOR_SHIPMENT"
        const val READY_FOR_ISSUANCE = "READY_FOR_ISSUANCE"
    }
}
