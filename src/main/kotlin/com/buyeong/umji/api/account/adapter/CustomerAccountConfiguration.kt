package com.buyeong.umji.api.account.adapter

import com.buyeong.umji.api.account.application.BuyerGroupMembershipService
import com.buyeong.umji.api.account.application.BuyerGroupTaxInvoiceProfileService
import com.buyeong.umji.api.account.application.CustomerAccountService
import com.buyeong.umji.api.account.application.port.`in`.BuyerGroupMembershipUseCase
import com.buyeong.umji.api.account.application.port.`in`.CustomerAccountUseCase
import com.buyeong.umji.api.account.application.port.out.BuyerGroupMembershipPort
import com.buyeong.umji.api.account.application.port.`in`.BuyerGroupTaxInvoiceProfileUseCase
import com.buyeong.umji.api.account.application.port.out.BuyerGroupTaxInvoiceProfilePort
import com.buyeong.umji.api.account.application.port.out.CustomerAccountPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CustomerAccountConfiguration {
    @Bean
    fun customerAccountUseCase(accounts: CustomerAccountPort): CustomerAccountUseCase = CustomerAccountService(accounts)

    @Bean
    fun buyerGroupMembershipUseCase(groups: BuyerGroupMembershipPort): BuyerGroupMembershipUseCase = BuyerGroupMembershipService(groups)

    @Bean
    fun buyerGroupTaxInvoiceProfileUseCase(profiles: BuyerGroupTaxInvoiceProfilePort): BuyerGroupTaxInvoiceProfileUseCase =
        BuyerGroupTaxInvoiceProfileService(profiles)
}
