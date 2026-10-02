package com.buyeong.umji.api.account.adapter.out.persistence

import com.buyeong.umji.api.account.application.model.CustomerProfile
import com.buyeong.umji.api.account.application.model.SharedAddress
import com.buyeong.umji.api.account.application.model.SharedAddressCommand
import com.buyeong.umji.api.account.application.port.out.CustomerAccountPort
import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.order.application.model.ShippingAddressSnapshot
import com.buyeong.umji.api.order.application.port.out.OrderShippingAddressPort
import com.buyeong.umji.api.persistence.jpa.account.AccountEntity
import com.buyeong.umji.api.persistence.jpa.account.AccountJpaEntityService
import com.buyeong.umji.api.persistence.jpa.account.BuyerGroupAddressEntity
import com.buyeong.umji.api.persistence.jpa.account.BuyerGroupAddressRepository
import com.buyeong.umji.api.persistence.jpa.account.BuyerGroupEntity
import com.buyeong.umji.api.persistence.jpa.account.BuyerGroupJpaEntityService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Component
class JpaCustomerAccountAdapter(
    private val accounts: AccountJpaEntityService,
    private val buyerGroups: BuyerGroupJpaEntityService,
    private val addresses: BuyerGroupAddressRepository,
) : CustomerAccountPort, OrderShippingAddressPort {
    @Transactional(readOnly = true)
    override fun profile(accountPublicId: UUID): CustomerProfile? = accounts.findByPublicId(accountPublicId)?.let { account ->
        CustomerProfile(requireNotNull(account.publicId), account.name, account.phone, account.email, account.status)
    }

    @Transactional(readOnly = true)
    override fun addresses(accountPublicId: UUID): List<SharedAddress> {
        val group = buyerGroups.activeForAccountPublicId(accountPublicId) ?: return emptyList()
        return addresses.findAllByBuyerGroupId(requireNotNull(group.id)).map { it.toModel() }
    }

    @Transactional
    override fun createAddress(accountPublicId: UUID, command: SharedAddressCommand): SharedAddress {
        val account = accounts.findByPublicId(accountPublicId) ?: throw ItemNotFoundException("계정을 찾을 수 없습니다.")
        val group = buyerGroups.lockActiveForAccountPublicId(accountPublicId)
        val groupId = requireNotNull(group.id)
        val currentAddresses = addresses.findAllByBuyerGroupId(groupId)
        val makeDefault = command.isDefault || currentAddresses.none { it.isDefault }
        if (command.isDefault) currentAddresses.forEach { it.setDefault(false) }

        return addresses.save(command.toEntity(group, account, makeDefault)).toModel()
    }

    @Transactional
    override fun updateAddress(accountPublicId: UUID, addressPublicId: UUID, command: SharedAddressCommand): SharedAddress? {
        val group = buyerGroups.lockActiveForAccountPublicId(accountPublicId)
        val groupId = requireNotNull(group.id)
        val address = addresses.findByPublicIdAndBuyerGroup_Id(addressPublicId, groupId) ?: return null
        val currentAddresses = addresses.findAllByBuyerGroupId(groupId)
        val wasDefault = address.isDefault
        if (command.isDefault) currentAddresses.filterNot { it.id == address.id }.forEach { it.setDefault(false) }
        address.apply(command, command.isDefault)
        if (wasDefault && !command.isDefault) {
            currentAddresses.filterNot { it.id == address.id }.firstOrNull()?.setDefault(true)
        }
        return address.toModel()
    }

    @Transactional
    override fun setDefaultAddress(accountPublicId: UUID, addressPublicId: UUID): SharedAddress? {
        val group = buyerGroups.lockActiveForAccountPublicId(accountPublicId)
        val groupId = requireNotNull(group.id)
        val address = addresses.findByPublicIdAndBuyerGroup_Id(addressPublicId, groupId) ?: return null
        addresses.findAllByBuyerGroupId(groupId).forEach { it.setDefault(it.id == address.id) }
        return address.toModel()
    }

    @Transactional
    override fun deleteAddress(accountPublicId: UUID, addressPublicId: UUID): Boolean {
        val group = buyerGroups.lockActiveForAccountPublicId(accountPublicId)
        val groupId = requireNotNull(group.id)
        val address = addresses.findByPublicIdAndBuyerGroup_Id(addressPublicId, groupId) ?: return false
        val wasDefault = address.isDefault
        addresses.delete(address)
        if (wasDefault) {
            addresses.findAllByBuyerGroupId(groupId).firstOrNull()?.setDefault(true)
        }
        return true
    }

    @Transactional(readOnly = true)
    override fun findForAccount(accountPublicId: UUID, addressPublicId: UUID): ShippingAddressSnapshot? {
        val groupId = buyerGroups.activeForAccountPublicId(accountPublicId)?.id ?: return null
        val address = addresses.findByPublicIdAndBuyerGroup_Id(addressPublicId, groupId) ?: return null
        return ShippingAddressSnapshot(
            address.recipientName,
            address.recipientPhone,
            address.postalCode,
            address.address1,
            address.address2,
        )
    }

    private fun SharedAddressCommand.toEntity(
        group: BuyerGroupEntity,
        account: AccountEntity,
        makeDefault: Boolean,
    ) = BuyerGroupAddressEntity().apply {
        buyerGroup = group
        createdByAccount = account
        apply(this@toEntity, makeDefault)
    }

    private fun BuyerGroupAddressEntity.apply(command: SharedAddressCommand, makeDefault: Boolean) {
        recipientName = command.recipientName
        recipientPhone = command.recipientPhone
        postalCode = command.postalCode
        address1 = command.address1
        address2 = command.address2
        isDefault = makeDefault
        updatedAt = Instant.now()
    }

    private fun BuyerGroupAddressEntity.setDefault(makeDefault: Boolean) {
        if (isDefault != makeDefault) {
            isDefault = makeDefault
            updatedAt = Instant.now()
        }
    }

    private fun BuyerGroupAddressEntity.toModel() = SharedAddress(
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