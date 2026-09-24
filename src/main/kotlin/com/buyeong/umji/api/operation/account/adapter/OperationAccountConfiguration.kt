package com.buyeong.umji.api.operation.account.adapter

import com.buyeong.umji.api.operation.account.application.OperationAccountService
import com.buyeong.umji.api.operation.account.application.port.`in`.OperationAccountUseCase
import com.buyeong.umji.api.operation.account.application.port.out.OperationAccountPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Configuration
class OperationAccountConfiguration {
    @Bean fun operationAccountUseCase(accounts: OperationAccountPort): OperationAccountUseCase = TransactionalOperationAccountUseCase(OperationAccountService(accounts))
}

@Transactional
class TransactionalOperationAccountUseCase(private val delegate: OperationAccountUseCase) : OperationAccountUseCase by delegate {
    @Transactional(readOnly = true)
    override fun list(status: String?, page: Int, size: Int) = delegate.list(status, page, size)

    @Transactional(readOnly = true)
    override fun detail(id: UUID) = delegate.detail(id)
}