package com.buyeong.umji.api.domain.account.service

import com.buyeong.umji.api.domain.account.dto.CustomerProfileDto
import com.buyeong.umji.api.domain.account.dto.SharedAddressCommandDto
import com.buyeong.umji.api.domain.account.dto.SharedAddressDto
import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.persistence.jpa.account.service.CustomerAccountJpaEntityService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class CustomerAccountService(
    private val accounts: CustomerAccountJpaEntityService,
) {
    @Transactional(readOnly = true)
    fun profile(accountPublicId: UUID): CustomerProfileDto =
        accounts.profile(accountPublicId) ?: throw ItemNotFoundException("계정을 찾을 수 없습니다.")

    @Transactional(readOnly = true)
    fun addresses(accountPublicId: UUID): List<SharedAddressDto> = accounts.addresses(accountPublicId)

    @Transactional
    fun createAddress(accountPublicId: UUID, command: SharedAddressCommandDto): SharedAddressDto {
        return accounts.createAddress(accountPublicId, command)
    }

    @Transactional
    fun updateAddress(accountPublicId: UUID, addressPublicId: UUID, command: SharedAddressCommandDto): SharedAddressDto {
        return accounts.updateAddress(accountPublicId, addressPublicId, command)
            ?: throw ItemNotFoundException("Organization 배송지를 찾을 수 없습니다.")
    }

    @Transactional
    fun setDefaultAddress(accountPublicId: UUID, addressPublicId: UUID): SharedAddressDto =
        accounts.setDefaultAddress(accountPublicId, addressPublicId)
            ?: throw ItemNotFoundException("Organization 배송지를 찾을 수 없습니다.")

    @Transactional
    fun deleteAddress(accountPublicId: UUID, addressPublicId: UUID) {
        if (!accounts.deleteAddress(accountPublicId, addressPublicId)) {
            throw ItemNotFoundException("Organization 배송지를 찾을 수 없습니다.")
        }
    }

}
