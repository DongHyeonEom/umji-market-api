package com.buyeong.umji.api.domain.shipment.service

import com.buyeong.umji.api.domain.shipment.dto.BrandShippingRateDto
import com.buyeong.umji.api.persistence.jpa.shipment.service.BrandShippingJpaEntityService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import java.util.UUID

class BrandShippingServiceTest : DescribeSpec({
    val persistence = mockk<BrandShippingJpaEntityService>()
    val service = BrandShippingService(persistence)
    val brandId = UUID.randomUUID()
    val otherBrandId = UUID.randomUUID()
    val skuId = UUID.randomUUID()
    val operatorId = UUID.randomUUID()
    val rate = BrandShippingRateDto(brandId, skuId, "SKU", "DAESIN", 1200)

    beforeTest {
        SecurityContextHolder.clearContext()
        io.mockk.clearMocks(persistence)
    }

    afterTest { SecurityContextHolder.clearContext() }

    fun authenticate(permission: String) {
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken.authenticated(
            operatorId,
            "",
            listOf(SimpleGrantedAuthority(permission)),
        )
    }

    it("SHIPMENT_ASSIGN 보유자는 브랜드·품목·택배사 요율을 설정할 수 있다") {
        authenticate("SHIPMENT_ASSIGN")
        every { persistence.setRate(brandId, skuId, "DAESIN", 1200, operatorId) } returns rate

        service.setRate(brandId, skuId, "DAESIN", 1200, operatorId) shouldBe rate

        verify(exactly = 1) { persistence.setRate(brandId, skuId, "DAESIN", 1200, operatorId) }
    }

    it("SHIPMENT_ASSIGN 보유자는 담당 배정 여부와 무관하게 브랜드 요율을 관리할 수 있다") {
        authenticate("SHIPMENT_ASSIGN")
        val otherBrandRate = rate.copy(brandId = otherBrandId)
        every { persistence.setRate(otherBrandId, skuId, "DAESIN", 1200, operatorId) } returns otherBrandRate

        service.setRate(otherBrandId, skuId, "DAESIN", 1200, operatorId) shouldBe otherBrandRate

        verify(exactly = 1) { persistence.setRate(otherBrandId, skuId, "DAESIN", 1200, operatorId) }
    }

    it("SHIPMENT_WRITE만 가진 작업자는 브랜드 요율을 변경할 수 없다") {
        authenticate("SHIPMENT_WRITE")

        shouldThrow<AccessDeniedException> {
            service.setRate(brandId, skuId, "DAESIN", 1200, operatorId)
        }

        verify(exactly = 0) { persistence.setRate(any(), any(), any(), any(), any()) }
    }
})
