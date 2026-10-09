package com.buyeong.umji.api.persistence.jpa.order.service

import com.buyeong.umji.api.persistence.jpa.account.entity.AccountEntity
import com.buyeong.umji.api.persistence.jpa.account.service.AccountJpaEntityService
import com.buyeong.umji.api.persistence.jpa.entity.backbone.DomainPublicEntity
import com.buyeong.umji.api.persistence.jpa.order.entity.PurchaseOrderEntity
import com.buyeong.umji.api.persistence.jpa.order.entity.PurchaseOrderTaxInvoiceEntity
import com.buyeong.umji.api.persistence.jpa.order.repository.PurchaseOrderTaxInvoiceEventRepository
import com.buyeong.umji.api.persistence.jpa.order.repository.PurchaseOrderTaxInvoiceRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class OperationTaxInvoiceJpaEntityServiceTest : DescribeSpec({
    val invoices = mockk<PurchaseOrderTaxInvoiceRepository>()
    val events = mockk<PurchaseOrderTaxInvoiceEventRepository>()
    val accounts = mockk<AccountJpaEntityService>()
    val service = OperationTaxInvoiceJpaEntityService(invoices, events, accounts)
    val orderId = UUID.randomUUID()
    val actorId = UUID.randomUUID()
    val approvalNumber = "20261009-ABC-123"

    fun invoice(status: String = "READY_FOR_ISSUANCE"): PurchaseOrderTaxInvoiceEntity {
        val account = AccountEntity().apply {
            name = "구매자"
            this.status = "ACTIVE"
        }
        val order = PurchaseOrderEntity().apply {
            orderNumber = "UMJ-20261009-000001"
            this.status = "PENDING_PAYMENT"
            subtotalAmount = 10_000
            totalAmount = 10_000
            orderedAt = Instant.parse("2026-10-09T00:00:00Z")
            this.account = account
        }
        val publicId = DomainPublicEntity::class.java.getDeclaredField("publicId").apply { isAccessible = true }
        publicId.set(order, orderId)
        return PurchaseOrderTaxInvoiceEntity().apply {
            this.order = order
            this.status = status
        }
    }

    beforeTest { clearMocks(invoices, events, accounts) }

    describe("수기 발행 결과 등록") {
        it("준비 주문에 승인번호·금액·날짜·처리자와 append-only 이벤트를 기록한다") {
            val row = invoice()
            val actor = AccountEntity().apply { status = "ACTIVE" }
            every { invoices.findLockedByOrderPublicId(orderId) } returns row
            every { events.existsByInvoiceApprovalNumber(approvalNumber) } returns false
            every { events.saveAndFlush(any()) } answers { firstArg() }
            every { accounts.findByPublicId(actorId) } returns actor

            val result = service.recordManualIssue(
                orderId, actorId, approvalNumber, LocalDate.parse("2026-10-09"),
                LocalDate.parse("2026-10-08"), LocalDate.parse("2026-10-08"), 10_000, 1_000, 11_000, "전화 주문 수기 발행",
            )

            row.status shouldBe "MANUALLY_ISSUED"
            row.invoiceApprovalNumber shouldBe approvalNumber
            row.supplyAmount shouldBe 10_000
            row.taxAmount shouldBe 1_000
            row.totalAmount shouldBe 11_000
            result.status shouldBe "MANUALLY_ISSUED"
            verify(exactly = 1) { events.saveAndFlush(match { it.eventType == "MANUAL_ISSUED" && it.invoiceApprovalNumber == approvalNumber && it.totalAmount == 11_000L }) }
        }

        it("주문 공급가액과 다르거나 승인번호가 중복이면 완료 상태로 변경하지 않는다") {
            val row = invoice()
            val actor = AccountEntity().apply { status = "ACTIVE" }
            every { invoices.findLockedByOrderPublicId(orderId) } returns row
            every { events.existsByInvoiceApprovalNumber(approvalNumber) } returns false
            every { accounts.findByPublicId(actorId) } returns actor

            shouldThrow<IllegalArgumentException> {
                service.recordManualIssue(
                    orderId, actorId, approvalNumber, LocalDate.now(), LocalDate.now(), LocalDate.now(),
                    9_999, 1_000, 10_999, null,
                )
            }
            row.status shouldBe "READY_FOR_ISSUANCE"

            every { events.existsByInvoiceApprovalNumber(approvalNumber) } returns true
            shouldThrow<IllegalArgumentException> {
                service.recordManualIssue(
                    orderId, actorId, approvalNumber, LocalDate.now(), LocalDate.now(), LocalDate.now(),
                    10_000, 1_000, 11_000, null,
                )
            }
            row.status shouldBe "READY_FOR_ISSUANCE"
        }

        it("수기 발행 완료 후에는 같은 주문을 다시 변경할 수 없다") {
            val row = invoice("MANUALLY_ISSUED")
            every { invoices.findLockedByOrderPublicId(orderId) } returns row

            shouldThrow<IllegalStateException> {
                service.recordManualIssue(
                    orderId, actorId, approvalNumber, LocalDate.now(), LocalDate.now(), LocalDate.now(),
                    10_000, 1_000, 11_000, null,
                )
            }
        }
    }
})