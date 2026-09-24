package com.buyeong.umji.api.operation.catalog.adapter

import com.buyeong.umji.api.operation.catalog.application.OperationCatalogService
import com.buyeong.umji.api.operation.catalog.application.port.`in`.OperationCatalogUseCase
import com.buyeong.umji.api.operation.catalog.application.port.out.OperationCatalogPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Configuration
class OperationCatalogConfiguration {
    @Bean
    fun operationCatalogUseCase(catalog: OperationCatalogPort): OperationCatalogUseCase = TransactionalOperationCatalogUseCase(OperationCatalogService(catalog))
}

@Transactional
class TransactionalOperationCatalogUseCase(private val delegate: OperationCatalogUseCase) : OperationCatalogUseCase by delegate {
    @Transactional(readOnly = true)
    override fun categories() = delegate.categories()

    @Transactional(readOnly = true)
    override fun brands(page: Int, size: Int) = delegate.brands(page, size)

    @Transactional(readOnly = true)
    override fun products(page: Int, size: Int) = delegate.products(page, size)

    @Transactional(readOnly = true)
    override fun product(id: UUID) = delegate.product(id)
}