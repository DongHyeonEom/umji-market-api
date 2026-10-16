package com.buyeong.umji.api.domain.operation.account.service

import com.buyeong.umji.api.domain.operation.account.dto.OrganizationCommercePolicyDto
import com.buyeong.umji.api.domain.operation.audit.service.OperationAuditChangeContext
import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.persistence.jpa.account.service.OrganizationJpaEntityService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class OrganizationCommercePolicyService(
    private val organizations: OrganizationJpaEntityService,
    private val auditChanges: OperationAuditChangeContext,
) {
    @Transactional
    fun update(
        organizationId: UUID,
        buyerType: String,
        paymentBeforeShipmentRequired: Boolean,
        reason: String,
    ): OrganizationCommercePolicyDto {
        val current = organizations.commercePolicy(organizationId)
            ?: throw ItemNotFoundException("활성 구매 Organization을 찾을 수 없습니다.")
        val before = mapOf(
            "buyerType" to current.buyerType,
            "paymentBeforeShipmentRequired" to current.paymentBeforeShipmentRequired,
        )
        val updated = organizations.updateCommercePolicy(organizationId, buyerType, paymentBeforeShipmentRequired)
        val after = mapOf(
            "buyerType" to updated.buyerType,
            "paymentBeforeShipmentRequired" to updated.paymentBeforeShipmentRequired,
        )
        auditChanges.capture(before, after, reason)
        return OrganizationCommercePolicyDto(organizationId, updated.buyerType, updated.paymentBeforeShipmentRequired)
    }
}
