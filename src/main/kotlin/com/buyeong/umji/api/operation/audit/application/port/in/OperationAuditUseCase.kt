package com.buyeong.umji.api.operation.audit.application.port.`in`

import com.buyeong.umji.api.operation.audit.application.model.OperationAuditEvent
import com.buyeong.umji.api.operation.audit.application.model.OperationAuditPage
import com.buyeong.umji.api.operation.audit.application.model.OperationAuditQuery
import java.time.Instant

interface OperationAuditUseCase {
    fun record(event: OperationAuditEvent)
    fun search(query: OperationAuditQuery): OperationAuditPage
    fun purgeExpired(before: Instant, batchSize: Int): Int
}