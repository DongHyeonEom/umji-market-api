package com.buyeong.umji.api.account.adapter.`in`.web

import com.buyeong.umji.api.account.application.model.CustomerProfile
import com.buyeong.umji.api.account.application.model.SharedAddress
import com.buyeong.umji.api.account.application.model.SharedAddressCommand
import com.buyeong.umji.api.account.application.port.`in`.CustomerAccountUseCase
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class TransactionalCustomerAccountUseCase(private val account: CustomerAccountUseCase) {
    @Transactional(readOnly = true)
    fun profile(accountPublicId: UUID): CustomerProfile = account.profile(accountPublicId)

    @Transactional(readOnly = true)
    fun addresses(accountPublicId: UUID): List<SharedAddress> = account.addresses(accountPublicId)

    @Transactional
    fun createAddress(accountPublicId: UUID, command: SharedAddressCommand): SharedAddress = account.createAddress(accountPublicId, command)

    @Transactional
    fun updateAddress(accountPublicId: UUID, addressPublicId: UUID, command: SharedAddressCommand): SharedAddress =
        account.updateAddress(accountPublicId, addressPublicId, command)

    @Transactional
    fun setDefaultAddress(accountPublicId: UUID, addressPublicId: UUID): SharedAddress =
        account.setDefaultAddress(accountPublicId, addressPublicId)

    @Transactional
    fun deleteAddress(accountPublicId: UUID, addressPublicId: UUID) = account.deleteAddress(accountPublicId, addressPublicId)
}