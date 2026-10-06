package com.buyeong.umji.api.account.service

import com.buyeong.umji.api.account.model.CustomerProfile
import com.buyeong.umji.api.account.model.SharedAddress
import com.buyeong.umji.api.account.model.SharedAddressCommand
import com.buyeong.umji.api.persistence.jpa.account.service.CustomerAccountJpaEntityService
import com.buyeong.umji.api.exception.ItemNotFoundException
import org.springframework.transaction.annotation.Transactional
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class CustomerAccountService(
    private val accounts: CustomerAccountJpaEntityService,
) {
    @Transactional(readOnly = true)
    fun profile(accountPublicId: UUID): CustomerProfile =
        accounts.profile(accountPublicId) ?: throw ItemNotFoundException("계정을 찾을 수 없습니다.")

    @Transactional(readOnly = true)
    fun addresses(accountPublicId: UUID): List<SharedAddress> = accounts.addresses(accountPublicId)

    @Transactional
    fun createAddress(accountPublicId: UUID, command: SharedAddressCommand): SharedAddress {
        validate(command)
        return accounts.createAddress(accountPublicId, command)
    }

    @Transactional
    fun updateAddress(accountPublicId: UUID, addressPublicId: UUID, command: SharedAddressCommand): SharedAddress {
        validate(command)
        return accounts.updateAddress(accountPublicId, addressPublicId, command)
            ?: throw ItemNotFoundException("Organization 배송지를 찾을 수 없습니다.")
    }

    @Transactional
    fun setDefaultAddress(accountPublicId: UUID, addressPublicId: UUID): SharedAddress =
        accounts.setDefaultAddress(accountPublicId, addressPublicId)
            ?: throw ItemNotFoundException("Organization 배송지를 찾을 수 없습니다.")

    @Transactional
    fun deleteAddress(accountPublicId: UUID, addressPublicId: UUID) {
        if (!accounts.deleteAddress(accountPublicId, addressPublicId)) {
            throw ItemNotFoundException("Organization 배송지를 찾을 수 없습니다.")
        }
    }

    private fun validate(command: SharedAddressCommand) {
        require(command.recipientName.isNotBlank()) { "수령인 이름은 필수입니다." }
        require(command.recipientPhone.isNotBlank()) { "수령인 연락처는 필수입니다." }
        require(command.postalCode.isNotBlank()) { "우편번호는 필수입니다." }
        require(command.address1.isNotBlank()) { "기본 주소는 필수입니다." }
        require(command.recipientName.length <= 100) { "수령인 이름은 100자 이하여야 합니다." }
        require(command.recipientPhone.length <= 30) { "수령인 연락처는 30자 이하여야 합니다." }
        require(command.postalCode.length <= 20) { "우편번호는 20자 이하여야 합니다." }
        require(command.address1.length <= 255) { "기본 주소는 255자 이하여야 합니다." }
        require(command.address2 == null || command.address2.length <= 255) { "상세 주소는 255자 이하여야 합니다." }
    }
}
