package com.buyeong.umji.api.domain.shipment.service

import com.buyeong.umji.api.domain.shipment.dto.BrandShippingManagerDto
import com.buyeong.umji.api.domain.shipment.dto.BrandShippingRateDto
import com.buyeong.umji.api.persistence.jpa.shipment.service.BrandShippingJpaEntityService
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class BrandShippingService(private val persistence: BrandShippingJpaEntityService) {
    fun assignManager(brandId: UUID, accountId: UUID, assignedBy: UUID): BrandShippingManagerDto =
        persistence.assignManager(brandId, accountId, assignedBy)

    fun removeManager(brandId: UUID, accountId: UUID) = persistence.removeManager(brandId, accountId)

    fun managers(brandId: UUID): List<BrandShippingManagerDto> = persistence.managers(brandId)

    fun setRate(brandId: UUID, skuId: UUID, carrierCode: String, amountPerBox: Long, operatorId: UUID): BrandShippingRateDto {
        if (!isAdmin()) throw AccessDeniedException("SHIPMENT_ASSIGN 권한이 필요합니다.")
        if (!canManageBrandOrAdmin(brandId, operatorId)) throw AccessDeniedException("담당 브랜드의 기본 운송비만 설정할 수 있습니다.")
        return persistence.setRate(brandId, skuId, carrierCode, amountPerBox, operatorId)
    }

    fun rates(brandId: UUID, accountId: UUID): List<BrandShippingRateDto> {
        requireReadable(brandId, accountId)
        return persistence.rates(brandId)
    }

    fun rate(brandId: UUID, skuId: UUID, carrierCode: String): BrandShippingRateDto? =
        persistence.rate(brandId, skuId, carrierCode)

    fun assignedBrandIds(accountId: UUID): Set<UUID> = persistence.assignedBrandIds(accountId)

    fun canManageBrand(brandId: UUID, accountId: UUID): Boolean = persistence.isAssigned(brandId, accountId)

    fun canManageBrandOrAdmin(brandId: UUID, accountId: UUID): Boolean =
        isAdmin() || persistence.isAssigned(brandId, accountId)

    private fun requireAssigned(brandId: UUID, accountId: UUID) {
        if (!persistence.isAssigned(brandId, accountId)) throw AccessDeniedException("담당 브랜드의 배송 작업만 처리할 수 있습니다.")
    }

    private fun requireReadable(brandId: UUID, accountId: UUID) {
        if (!isAdmin() && !persistence.isAssigned(brandId, accountId)) {
            throw AccessDeniedException("담당 브랜드의 배송 정보만 조회할 수 있습니다.")
        }
    }

    private fun isAdmin(): Boolean = SecurityContextHolder.getContext().authentication?.authorities
        ?.any { it.authority == "SHIPMENT_ASSIGN" } == true
}
