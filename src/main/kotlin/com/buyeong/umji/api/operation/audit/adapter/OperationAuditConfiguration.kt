package com.buyeong.umji.api.operation.audit.adapter

import com.buyeong.umji.api.operation.audit.application.OperationAuditService
import com.buyeong.umji.api.operation.audit.application.port.`in`.OperationAuditUseCase
import com.buyeong.umji.api.operation.audit.application.port.out.OperationAuditPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@EnableScheduling
class OperationAuditConfiguration {
    @Bean
    fun operationAuditUseCase(audit: OperationAuditPort): OperationAuditUseCase = OperationAuditService(audit)
}