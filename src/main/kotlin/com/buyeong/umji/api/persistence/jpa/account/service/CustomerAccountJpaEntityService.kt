package com.buyeong.umji.api.persistence.jpa.account.service

import com.buyeong.umji.api.account.model.CustomerProfile
import com.buyeong.umji.api.account.model.SharedAddress
import com.buyeong.umji.api.account.model.SharedAddressCommand
import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.order.model.ShippingAddressSnapshot
import com.buyeong.umji.api.persistence.jpa.account.entity.AccountEntity
import com.buyeong.umji.api.persistence.jpa.account.entity.OrganizationAddressEntity
import com.buyeong.umji.api.persistence.jpa.account.entity.OrganizationEntity
import com.buyeong.umji.api.persistence.jpa.account.repository.OrganizationAddressRepository
import java.time.Instant
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CustomerAccountJpaEntityService(
    private val accounts: AccountJpaEntityService,
    private val organizations: OrganizationJpaEntityService,
    private val addresses: OrganizationAddressRepository,
) {
    fun profile(accountPublicId: UUID): CustomerProfile? = accounts.findByPublicId(accountPublicId)?.let { account ->
        CustomerProfile(requireNotNull(account.publicId), account.name, account.phone, account.email, account.status)
    }

    fun addresses(accountPublicId: UUID): List<SharedAddress> {
        val group = organizations.activeBuyerForAccountPublicId(accountPublicId) ?: return emptyList()
        return addresses.findAllByOrganizationId(requireNotNull(group.id)).map { it.toModel() }
    }

    @Transactional
    fun createAddress(accountPublicId: UUID, command: SharedAddressCommand): SharedAddress {
        val account = accounts.findByPublicId(accountPublicId) ?: throw ItemNotFoundException("계정을 찾을 수 없습니다.")
        val group = organizations.lockActiveForAccountPublicId(accountPublicId)
        val organizationId = requireNotNull(group.id)
        val currentAddresses = addresses.findAllByOrganizationId(organizationId)
        val makeDefault = command.isDefault || currentAddresses.none { it.isDefault }
        if (command.isDefault) currentAddresses.forEach { it.setDefault(false) }

        return addresses.save(command.toEntity(group, account, makeDefault)).toModel()
    }

    @Transactional
    fun updateAddress(accountPublicId: UUID, addressPublicId: UUID, command: SharedAddressCommand): SharedAddress? {
        val group = organizations.lockActiveForAccountPublicId(accountPublicId)
        val organizationId = requireNotNull(group.id)
        val address = addresses.findByPublicIdAndOrganization_Id(addressPublicId, organizationId) ?: return null
        val currentAddresses = addresses.findAllByOrganizationId(organizationId)
        val wasDefault = address.isDefault
        if (command.isDefault) currentAddresses.filterNot { it.id == address.id }.forEach { it.setDefault(false) }
        address.apply(command, command.isDefault)
        if (wasDefault && !command.isDefault) {
            currentAddresses.filterNot { it.id == address.id }.firstOrNull()?.setDefault(true)
        }
        return address.toModel()
    }

    @Transactional
    fun setDefaultAddress(accountPublicId: UUID, addressPublicId: UUID): SharedAddress? {
        val group = organizations.lockActiveForAccountPublicId(accountPublicId)
        val organizationId = requireNotNull(group.id)
        val address = addresses.findByPublicIdAndOrganization_Id(addressPublicId, organizationId) ?: return null
        addresses.findAllByOrganizationId(organizationId).forEach { it.setDefault(it.id == address.id) }
        return address.toModel()
    }

    @Transactional
    fun deleteAddress(accountPublicId: UUID, addressPublicId: UUID): Boolean {
        val group = organizations.lockActiveForAccountPublicId(accountPublicId)
        val organizationId = requireNotNull(group.id)
        val address = addresses.findByPublicIdAndOrganization_Id(addressPublicId, organizationId) ?: return false
        val wasDefault = address.isDefault
        addresses.delete(address)
        if (wasDefault) {
            addresses.findAllByOrganizationId(organizationId).firstOrNull()?.setDefault(true)
        }
        return true
    }

    fun findForAccount(accountPublicId: UUID, addressPublicId: UUID): ShippingAddressSnapshot? {
        val organizationId = organizations.activeBuyerForAccountPublicId(accountPublicId)?.id ?: return null
        val address = addresses.findByPublicIdAndOrganization_Id(addressPublicId, organizationId) ?: return null
        return ShippingAddressSnapshot(
            address.recipientName,
            address.recipientPhone,
            address.postalCode,
            address.address1,
            address.address2,
        )
    }

    private fun SharedAddressCommand.toEntity(
        group: OrganizationEntity,
        account: AccountEntity,
        makeDefault: Boolean,
    ) = OrganizationAddressEntity().apply {
        organization = group
        createdByAccount = account
        apply(this@toEntity, makeDefault)
    }

    private fun OrganizationAddressEntity.apply(command: SharedAddressCommand, makeDefault: Boolean) {
        recipientName = command.recipientName
        recipientPhone = command.recipientPhone
        postalCode = command.postalCode
        address1 = command.address1
        address2 = command.address2
        isDefault = makeDefault
        updatedAt = Instant.now()
    }

    private fun OrganizationAddressEntity.setDefault(makeDefault: Boolean) {
        if (isDefault != makeDefault) {
            isDefault = makeDefault
            updatedAt = Instant.now()
        }
    }

    private fun OrganizationAddressEntity.toModel() = SharedAddress(
        requireNotNull(publicId),
        recipientName,
        recipientPhone,
        postalCode,
        address1,
        address2,
        isDefault,
        createdAt,
        updatedAt,
    )
}
