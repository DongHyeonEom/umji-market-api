package com.buyeong.umji.api.account.application.port.`in`

import com.buyeong.umji.api.account.application.model.CustomerProfile
import com.buyeong.umji.api.account.application.model.SharedAddress
import com.buyeong.umji.api.account.application.model.SharedAddressCommand
import java.util.UUID

interface CustomerAccountUseCase {
    fun profile(accountPublicId: UUID): CustomerProfile
    fun addresses(accountPublicId: UUID): List<SharedAddress>
    fun createAddress(accountPublicId: UUID, command: SharedAddressCommand): SharedAddress
    fun updateAddress(accountPublicId: UUID, addressPublicId: UUID, command: SharedAddressCommand): SharedAddress
    fun setDefaultAddress(accountPublicId: UUID, addressPublicId: UUID): SharedAddress
    fun deleteAddress(accountPublicId: UUID, addressPublicId: UUID)
}