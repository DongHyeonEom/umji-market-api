package com.buyeong.umji.api.persistence.jpa.order.service

import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.operation.order.model.TaxInvoiceQueueData
import com.buyeong.umji.api.operation.order.model.TaxInvoiceQueueItem
import com.buyeong.umji.api.persistence.jpa.account.service.AccountJpaEntityService
import com.buyeong.umji.api.persistence.jpa.order.entity.PurchaseOrderTaxInvoiceEventEntity
import com.buyeong.umji.api.persistence.jpa.order.repository.PurchaseOrderTaxInvoiceEventRepository
import com.buyeong.umji.api.persistence.jpa.order.repository.PurchaseOrderTaxInvoiceRepository
import java.time.LocalDate
import java.util.UUID
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class OperationTaxInvoiceJpaEntityService(
    private val invoices: PurchaseOrderTaxInvoiceRepository,
    private val events: PurchaseOrderTaxInvoiceEventRepository,
    private val accounts: AccountJpaEntityService,
) {
    fun queue(page: Int, size: Int): TaxInvoiceQueueData {
        val result = invoices.findAllByStatusInOrderByOrder_OrderedAtDesc(
            listOf(READY_FOR_ISSUANCE, MANUALLY_ISSUED), PageRequest.of(page, size, Sort.unsorted()),
        )
        return TaxInvoiceQueueData(result.content.map { it.toData() }, result.number, result.size, result.totalElements, result.totalPages)
    }

    @Transactional
    fun recordManualIssue(
        orderId: UUID,
        actorId: UUID,
        approvalNumber: String,
        issuedAt: LocalDate,
        writtenDate: LocalDate,
        supplyDate: LocalDate,
        supplyAmount: Long,
        taxAmount: Long,
        totalAmount: Long,
        reason: String?,
    ): TaxInvoiceQueueItem {
        val invoice = invoices.findLockedByOrderPublicId(orderId) ?: throw ItemNotFoundException("세금계산서 발행 요청 주문을 찾을 수 없습니다.")
        check(invoice.status == READY_FOR_ISSUANCE) { "발행 준비 상태의 세금계산서만 수기 발행 결과를 등록할 수 있습니다." }
        require(!events.existsByInvoiceApprovalNumber(approvalNumber)) { "이미 등록된 승인번호입니다." }
        val issuer = accounts.findByPublicId(actorId)?.takeIf { it.status == "ACTIVE" }
            ?: throw ItemNotFoundException("관리자 계정을 찾을 수 없습니다.")
        val order = invoice.order
        require(order.subtotalAmount == supplyAmount) { "공급가액은 주문 시점 상품 금액과 일치해야 합니다." }
        require(supplyAmount >= 0 && taxAmount >= 0 && supplyAmount <= Long.MAX_VALUE - taxAmount) { "세금계산서 금액 범위를 확인해야 합니다." }
        require(supplyAmount + taxAmount == totalAmount) { "합계 금액은 공급가액과 세액의 합과 일치해야 합니다." }
        invoice.status = MANUALLY_ISSUED
        invoice.invoiceApprovalNumber = approvalNumber
        invoice.issuedAt = issuedAt
        invoice.writtenDate = writtenDate
        invoice.supplyDate = supplyDate
        invoice.supplyAmount = supplyAmount
        invoice.taxAmount = taxAmount
        invoice.totalAmount = totalAmount
        invoice.issuedBy = issuer
        events.saveAndFlush(PurchaseOrderTaxInvoiceEventEntity().apply {
            this.order = order
            eventType = "MANUAL_ISSUED"
            invoiceApprovalNumber = approvalNumber
            this.issuedAt = issuedAt
            this.writtenDate = writtenDate
            this.supplyDate = supplyDate
            this.supplyAmount = supplyAmount
            this.taxAmount = taxAmount
            this.totalAmount = totalAmount
            processedBy = issuer
            this.reason = reason?.trim()?.ifBlank { null }
        })
        return invoice.toData()
    }

    private fun com.buyeong.umji.api.persistence.jpa.order.entity.PurchaseOrderTaxInvoiceEntity.toData() = TaxInvoiceQueueItem(
        orderId = requireNotNull(order.publicId),
        orderNumber = order.orderNumber,
        orderStatus = order.status,
        buyerName = order.account.name,
        buyerPhoneSuffix = order.account.phone?.filter(Char::isDigit)?.takeLast(4),
        orderedAt = order.orderedAt,
        orderAmount = order.totalAmount,
        supplierBusinessName = supplierBusinessName,
        buyerBusinessName = buyerBusinessName,
        items = order.items.map { item ->
            com.buyeong.umji.api.operation.order.model.TaxInvoiceOrderItem(
                item.productName, item.skuCode, item.quantity, item.unitPrice, item.lineAmount,
            )
        },
        status = status,
        approvalNumber = invoiceApprovalNumber,
        issuedAt = issuedAt,
        writtenDate = writtenDate,
        supplyDate = supplyDate,
        supplyAmount = supplyAmount ?: order.items.sumOf { it.lineAmount },
        taxAmount = taxAmount,
        totalAmount = totalAmount,
        issuedByAccountId = issuedBy?.publicId,
    )

    companion object {
        const val READY_FOR_ISSUANCE = "READY_FOR_ISSUANCE"
        const val MANUALLY_ISSUED = "MANUALLY_ISSUED"
    }
}
