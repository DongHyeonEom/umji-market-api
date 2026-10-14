package com.buyeong.umji.api.domain.operation.audit.service

data class OperationAuditChange(
    val beforeValues: Map<String, Any?>,
    val afterValues: Map<String, Any?>,
    val reason: String?,
)