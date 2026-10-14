package com.buyeong.umji.api.domain.operation.audit.service

import org.springframework.stereotype.Component

@Component
class OperationAuditChangeContext {
    private val current = ThreadLocal<OperationAuditChange?>()

    fun capture(beforeValues: Map<String, Any?>, afterValues: Map<String, Any?>, reason: String? = null) {
        current.set(OperationAuditChange(beforeValues, afterValues, reason?.trim()?.takeIf(String::isNotEmpty)))
    }

    fun consume(): OperationAuditChange? = current.get().also { current.remove() }

    fun clear() = current.remove()
}