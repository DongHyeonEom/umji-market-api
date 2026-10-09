package com.buyeong.umji.api.operation.order.service

import com.buyeong.umji.api.notification.dto.NotificationEventType
import com.buyeong.umji.api.notification.service.NotificationEventService
import com.buyeong.umji.api.operation.order.dto.TaxInvoiceQueueDataDto
import com.buyeong.umji.api.operation.order.dto.TaxInvoiceQueueItemDto
import com.buyeong.umji.api.persistence.jpa.order.service.OperationTaxInvoiceJpaEntityService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
@Transactional(readOnly = true)
class OperationTaxInvoiceService(
    private val invoices: OperationTaxInvoiceJpaEntityService,
    private val notifications: NotificationEventService,
) {
    fun queue(page: Int, size: Int): TaxInvoiceQueueDataDto = invoices.queue(page, size)

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
    ): TaxInvoiceQueueItemDto {
        val issued = invoices.recordManualIssue(
            orderId, actorId, approvalNumber.trim(), issuedAt, writtenDate, supplyDate,
            supplyAmount, taxAmount, totalAmount, reason,
        )
        notifications.record(NotificationEventType.TAX_INVOICE_ISSUED, orderId)
        return issued
    }
}