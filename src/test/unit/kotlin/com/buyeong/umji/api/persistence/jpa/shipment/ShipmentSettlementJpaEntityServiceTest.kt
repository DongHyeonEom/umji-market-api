package com.buyeong.umji.api.persistence.jpa.shipment

import com.buyeong.umji.api.domain.shipment.dto.ShipmentSettlementQueryDto
import com.buyeong.umji.api.persistence.jpa.catalog.entity.BrandEntity
import com.buyeong.umji.api.persistence.jpa.catalog.repository.BrandRepository
import com.buyeong.umji.api.persistence.jpa.order.repository.OrderShipmentPackageRepository
import com.buyeong.umji.api.persistence.jpa.order.repository.ShipmentCarrierCostProjection
import com.buyeong.umji.api.persistence.jpa.order.repository.ShipmentCostSummaryProjection
import com.buyeong.umji.api.persistence.jpa.shipment.service.ShipmentSettlementJpaEntityService
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.data.domain.PageImpl
import java.time.Instant
import java.time.YearMonth
import java.util.UUID

class ShipmentSettlementJpaEntityServiceTest : DescribeSpec({
    val brands = mockk<BrandRepository>()
    val packages = mockk<OrderShipmentPackageRepository>()
    val service = ShipmentSettlementJpaEntityService(brands, packages)

    it("월 운송비 집계와 송장 상세 페이지를 같은 브랜드 범위로 조합한다") {
        val brandId = UUID.randomUUID()
        val from = Instant.parse("2026-01-31T15:00:00Z")
        val until = Instant.parse("2026-02-28T15:00:00Z")
        every { brands.findByPublicIdAndDeletedAtIsNull(brandId) } returns BrandEntity().apply { name = "엄지 브랜드" }
        val summary = mockk<ShipmentCostSummaryProjection> {
            every { getPackageCount() } returns 3
            every { getActualCostTotal() } returns 12000
            every { getMissingCostCount() } returns 1
        }
        every { packages.aggregateShippingCosts(brandId, from, until) } returns summary
        val carrier = mockk<ShipmentCarrierCostProjection> {
            every { getCarrierCode() } returns "DAESIN"
            every { getPackageCount() } returns 3
            every { getActualCostTotal() } returns 12000
            every { getMissingCostCount() } returns 1
        }
        every { packages.aggregateShippingCostsByCarrier(brandId, from, until) } returns listOf(carrier)
        every { packages.findAllForSettlement(eq(brandId), eq(from), eq(until), any()) } returns PageImpl(emptyList(), org.springframework.data.domain.PageRequest.of(0, 20), 3)

        val result = service.monthly(ShipmentSettlementQueryDto(brandId, YearMonth.of(2026, 2), from, until, 0, 20))

        result.brandName shouldBe "엄지 브랜드"
        result.packageCount shouldBe 3
        result.actualCostTotal shouldBe 12000
        result.pricedPackageCount shouldBe 2
        result.missingCostCount shouldBe 1
        result.carriers.single().carrierCode shouldBe "DAESIN"
        result.totalElements shouldBe 3
        result.items shouldBe emptyList()
    }
})
