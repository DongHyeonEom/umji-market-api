package com.buyeong.umji.api.domain.operation.audit.dto

data class OperationAuditPageDto(
    val items: List<OperationAuditEntryDto>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)