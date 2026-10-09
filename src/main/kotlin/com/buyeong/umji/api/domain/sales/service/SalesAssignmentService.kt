package com.buyeong.umji.api.domain.sales.service

import com.buyeong.umji.api.exception.InvalidRequestParameterException
import com.buyeong.umji.api.persistence.jpa.sales.service.OrganizationSalesAssignmentJpaEntityService
import com.buyeong.umji.api.domain.sales.dto.SalesAssignmentCommandDto
import com.buyeong.umji.api.domain.sales.dto.SalesAssignmentViewDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class SalesAssignmentService(
    private val assignments: OrganizationSalesAssignmentJpaEntityService,
) {
    @Transactional(readOnly = true)
    fun history(organizationId: UUID): List<SalesAssignmentViewDto> = assignments.history(organizationId)

    @Transactional
    fun assign(organizationId: UUID, command: SalesAssignmentCommandDto): List<SalesAssignmentViewDto> {
        if (command.commissionRateBps != null && command.commissionRateBps !in 1..10_000) {
            throw InvalidRequestParameterException("인센티브율은 1~10000 basis points 범위여야 합니다.")
        }
        if (command.assignmentReason.isBlank() || command.assignmentReason.length > 30) {
            throw InvalidRequestParameterException("배정 사유는 1~30자여야 합니다.")
        }
        return assignments.assign(organizationId, command)
    }
}