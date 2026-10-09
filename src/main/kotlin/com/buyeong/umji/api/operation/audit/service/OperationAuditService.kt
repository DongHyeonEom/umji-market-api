package com.buyeong.umji.api.operation.audit.service

import com.buyeong.umji.api.operation.audit.dto.OperationAuditEventDto
import com.buyeong.umji.api.operation.audit.dto.OperationAuditPageDto
import com.buyeong.umji.api.operation.audit.dto.OperationAuditQueryDto
import com.buyeong.umji.api.persistence.jpa.operation.audit.service.OperationAuditJpaEntityService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
@Transactional
class OperationAuditService(private val audit: OperationAuditJpaEntityService) {
    fun record(event: OperationAuditEventDto) = audit.record(event)

    @Transactional(readOnly = true)
    fun search(query: OperationAuditQueryDto): OperationAuditPageDto {
        require(query.page >= 0) { "페이지 번호는 0 이상이어야 합니다." }
        require(query.size in 1..100) { "페이지 크기는 1~100이어야 합니다." }
        require(query.from == null || query.until == null || query.from <= query.until) { "조회 시작 시각은 종료 시각보다 늦을 수 없습니다." }
        require(query.resourceType == null || query.resourceType in RESOURCE_TYPES) { "유효하지 않은 감사 리소스 유형입니다." }
        return audit.search(query)
    }

    fun purgeExpired(before: Instant, batchSize: Int): Int {
        require(batchSize in 1..10_000) { "삭제 batch 크기는 1~10000이어야 합니다." }
        return audit.purgeExpired(before, batchSize)
    }

    private companion object {
        val RESOURCE_TYPES = setOf("ACCOUNT", "ACCOUNT_ROLE", "CATEGORY", "BRAND", "PRODUCT", "ORDER", "INVENTORY", "OPERATION")
    }
}