package com.buyeong.umji.api.domain.operation.order.service

import com.buyeong.umji.api.domain.operation.order.dto.OperationOrderPageDto
import com.buyeong.umji.api.domain.operation.order.dto.OperationOrderQueryDto
import com.buyeong.umji.api.persistence.jpa.order.service.OperationOrderJpaEntityService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.Instant

class OperationOrderServiceTest : DescribeSpec({
    val persistence = mockk<OperationOrderJpaEntityService>()
    val service = OperationOrderService(persistence)

    describe("관리자 주문 검색") {
        it("검색어를 정규화하고 검색 결과를 반환한다") {
            clearMocks(persistence)
            val captured = slot<OperationOrderQueryDto>()
            every { persistence.search(capture(captured)) } returns OperationOrderPageDto(emptyList(), 1, 30, 42, 2)

            service.search(
                query(
                    organizationName = "  엄지 유통 ",
                    ordererName = " 홍 길동 ",
                    orderNumber = " ORD-123 ",
                    paymentStatus = "PAYMENT_CONFIRMED",
                    shipmentStatus = "IN_TRANSIT",
                    page = 1,
                    size = 30,
                ),
            )

            captured.captured.organizationName shouldBe "엄지 유통"
            captured.captured.ordererName shouldBe "홍 길동"
            captured.captured.orderNumber shouldBe "ord-123"
            captured.captured.paymentStatus shouldBe "PAYMENT_CONFIRMED"
            captured.captured.shipmentStatus shouldBe "IN_TRANSIT"
            captured.captured.page shouldBe 1
            captured.captured.size shouldBe 30
        }

        it("잘못된 기간과 페이지 조건은 저장소 호출 전에 거부한다") {
            clearMocks(persistence)
            shouldThrow<IllegalArgumentException> { service.search(query(until = Instant.parse("2026-02-01T00:00:00Z"))) }
            shouldThrow<IllegalArgumentException> { service.search(query(size = 101)) }
            verify(exactly = 0) { persistence.search(any()) }
        }
    }
})

private fun query(
    from: Instant = Instant.parse("2026-02-01T00:00:00Z"),
    until: Instant = Instant.parse("2026-03-01T00:00:00Z"),
    organizationName: String? = null,
    ordererName: String? = null,
    orderNumber: String? = null,
    paymentStatus: String? = null,
    shipmentStatus: String? = null,
    page: Int = 0,
    size: Int = 20,
) = OperationOrderQueryDto(from, until, organizationName, ordererName, paymentStatus, shipmentStatus, orderNumber, page, size)
