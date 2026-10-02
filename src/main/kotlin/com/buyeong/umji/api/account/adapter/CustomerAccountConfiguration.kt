package com.buyeong.umji.api.account.adapter

import com.buyeong.umji.api.account.application.CustomerAccountService
import com.buyeong.umji.api.account.application.port.`in`.CustomerAccountUseCase
import com.buyeong.umji.api.account.application.port.out.CustomerAccountPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CustomerAccountConfiguration {
    @Bean
    fun customerAccountUseCase(accounts: CustomerAccountPort): CustomerAccountUseCase = CustomerAccountService(accounts)
}