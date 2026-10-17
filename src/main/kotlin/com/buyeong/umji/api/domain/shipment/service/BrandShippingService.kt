package com.buyeong.umji.api.domain.shipment.service

import com.buyeong.umji.api.domain.shipment.dto.BrandShippingAssignmentDto
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
    fun assignManager(brandId: UUID, accountId: UUID, assignedBy: UUID): BrandShippingAssignmentDto =
        persistence.assignManager(brandId, accountId, assignedBy)

    fun assignOperator(brandId: UUID, accountId: UUID, assignedBy: UUID): BrandShippingAssignmentDto =
        persistence.assignOperator(brandId, accountId, assignedBy)

    fun removeManager(brandId: UUID, accountId: UUID) = persistence.removeManager(brandId, accountId)

    fun removeOperator(brandId: UUID, accountId: UUID) = persistence.removeOperator(brandId, accountId)

    fun managers(brandId: UUID): List<BrandShippingAssignmentDto> = persistence.staff(brandId, MANAGER)

    fun operators(brandId: UUID): List<BrandShippingAssignmentDto> = persistence.staff(brandId, OPERATOR)

    fun setRate(brandId: UUID, skuId: UUID, carrierCode: String, amountPerBox: Long, operatorId: UUID): BrandShippingRateDto {
        if (!canConfigureBrandOrAdmin(brandId, operatorId)) throw AccessDeniedException("담당 브랜드의 배송 관리자만 기본 운송비를 설정할 수 있습니다.")
        return persistence.setRate(brandId, skuId, carrierCode, amountPerBox, operatorId)
    }

    fun rates(brandId: UUID, accountId: UUID): List<BrandShippingRateDto> {
        requireReadable(brandId, accountId)
        return persistence.rates(brandId)
    }

    fun rate(brandId: UUID, skuId: UUID, carrierCode: String): BrandShippingRateDto? =
        persistence.rate(brandId, skuId, carrierCode)

    fun assignedBrandIds(accountId: UUID): Set<UUID> = persistence.assignedBrandIds(accountId)

    fun canManageBrandOrAdmin(brandId: UUID, accountId: UUID): Boolean =
        isAdmin() || persistence.isAssigned(brandId, accountId)

    fun canConfigureBrandOrAdmin(brandId: UUID, accountId: UUID): Boolean = isAdmin() || (
        SecurityContextHolder.getContext().authentication?.authorities?.any { it.authority == CONFIGURE_PERMISSION } == true &&
            persistence.isManagerAssigned(brandId, accountId)
        )

    private fun requireReadable(brandId: UUID, accountId: UUID) {
        if (!isAdmin() && !persistence.isAssigned(brandId, accountId)) {
            throw AccessDeniedException("담당 브랜드의 배송 정보만 조회할 수 있습니다.")
        }
    }

    private fun isAdmin(): Boolean = SecurityContextHolder.getContext().authentication?.authorities
        ?.any { it.authority == "SHIPMENT_ASSIGN" } == true

    private companion object {
        const val MANAGER = "MANAGER"
        const val OPERATOR = "OPERATOR"
        const val CONFIGURE_PERMISSION = "SHIPMENT_CONFIGURE"
    }
}
