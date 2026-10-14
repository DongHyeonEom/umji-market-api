package com.buyeong.umji.api.domain.shipment.service

import com.buyeong.umji.api.domain.shipment.dto.ShipmentSettlementDto
import com.buyeong.umji.api.domain.shipment.dto.ShipmentSettlementQueryDto
import com.buyeong.umji.api.persistence.jpa.shipment.service.ShipmentSettlementJpaEntityService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.security.access.AccessDeniedException
import java.time.Instant
import java.util.UUID

class ShipmentSettlementServiceTest : DescribeSpec({
    val persistence = mockk<ShipmentSettlementJpaEntityService>()
    val brandShipping = mockk<BrandShippingService>()
    val service = ShipmentSettlementService(persistence, brandShipping)

    describe("브랜드 월 운송비 조회") {
        it("송장 등록일 기준 한국 월 경계를 저장소에 전달한다") {
            clearMocks(persistence, brandShipping)
            val brandId = UUID.randomUUID()
            val managerId = UUID.randomUUID()
            val captured = slot<ShipmentSettlementQueryDto>()
            every { brandShipping.canManageBrandOrAdmin(brandId, managerId) } returns true
            every { persistence.monthly(capture(captured)) } returns mockk<ShipmentSettlementDto>(relaxed = true)

            service.monthly(brandId, "2026-02", managerId, 0, 20)

            captured.captured.from shouldBe Instant.parse("2026-01-31T15:00:00Z")
            captured.captured.until shouldBe Instant.parse("2026-02-28T15:00:00Z")
            captured.captured.month.toString() shouldBe "2026-02"
        }

        it("타 브랜드 조회와 잘못된 월·페이지를 거부한다") {
            clearMocks(persistence, brandShipping)
            val brandId = UUID.randomUUID()
            val managerId = UUID.randomUUID()
            every { brandShipping.canManageBrandOrAdmin(brandId, managerId) } returns false

            shouldThrow<AccessDeniedException> { service.monthly(brandId, "2026-02", managerId, 0, 20) }
            shouldThrow<IllegalArgumentException> { service.monthly(brandId, "2026-13", managerId, 0, 20) }
            shouldThrow<IllegalArgumentException> { service.monthly(brandId, "2026-02", managerId, 0, 0) }
            verify(exactly = 0) { persistence.monthly(any()) }
        }
    }
})