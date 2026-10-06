package com.buyeong.umji.api.operation.account.service

import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.operation.account.model.AccountData
import com.buyeong.umji.api.operation.account.model.BusinessProfileData
import com.buyeong.umji.api.operation.account.model.ConsentCommand
import com.buyeong.umji.api.operation.account.model.ManagedRole
import com.buyeong.umji.api.operation.account.model.NewAccount
import com.buyeong.umji.api.persistence.jpa.account.OperationAccountJpaEntityService
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class OperationAccountService(private val accounts: OperationAccountJpaEntityService) {
    fun create(command: NewAccount) = accounts.create(command)
    @Transactional(readOnly = true)
    fun list(status: String?, page: Int, size: Int) = accounts.list(status, page, size)
    @Transactional(readOnly = true)
    fun detail(id: UUID) = accounts.find(id) ?: missing()
    fun status(id: UUID, status: String): AccountData {
        require(status in STATUSES) { "유효하지 않은 계정 상태입니다." }
        require(status != ACTIVE) { "개인정보 동의 확인 후 승인 API를 이용해야 합니다." }
        return accounts.updateStatus(id, status) ?: missing()
    }
    fun profile(id: UUID, profile: BusinessProfileData): AccountData {
        val account = accounts.find(id) ?: missing()
        val nextStatus = if (account.status == PENDING_PROFILE) PENDING_REVIEW else account.status
        return accounts.updateProfile(id, profile, nextStatus) ?: missing()
    }
    fun assignBuyerGroup(id: UUID, buyerGroupId: UUID): AccountData = accounts.assignBuyerGroup(id, buyerGroupId) ?: missing()
    fun setBuyerGroupRepresentative(groupId: UUID, accountId: UUID) = accounts.setBuyerGroupRepresentative(groupId, accountId)
    fun consent(id: UUID, consent: ConsentCommand): AccountData {
        require(consent.consentMethod in METHODS) { "유효하지 않은 동의 방식입니다." }
        val account = accounts.find(id) ?: missing()
        val nextStatus = if (account.status == PENDING_CONSENT) {
            if (account.profile == null) PENDING_PROFILE else PENDING_REVIEW
        } else {
            account.status
        }
        return accounts.addConsent(id, consent, nextStatus) ?: missing()
    }
    fun approve(id: UUID): AccountData {
        require(accounts.hasConsent(id, PERSONAL_INFORMATION)) { "개인정보 동의 이력이 필요합니다." }
        return accounts.approve(id) ?: missing()
    }
    @Transactional(readOnly = true)
    fun managedRoles(): List<ManagedRole> = accounts.managedRoles()
    @Transactional(readOnly = true)
    fun roles(id: UUID): List<ManagedRole> = accounts.roles(id) ?: missing()
    fun grantRole(id: UUID, roleCode: String, grantedBy: UUID): List<ManagedRole> {
        require(roleCode in MANAGED_ROLE_CODES) { "부여할 수 없는 role입니다." }
        return accounts.grantRole(id, roleCode, grantedBy) ?: missing()
    }
    fun revokeRole(id: UUID, roleCode: String): List<ManagedRole> {
        require(roleCode in MANAGED_ROLE_CODES) { "회수할 수 없는 role입니다." }
        return accounts.revokeRole(id, roleCode) ?: missing()
    }
    private fun missing(): Nothing = throw ItemNotFoundException("계정을 찾을 수 없습니다.")
    private companion object {
        const val PENDING_CONSENT = "PENDING_CONSENT"
        const val PENDING_PROFILE = "PENDING_PROFILE"
        const val PENDING_REVIEW = "PENDING_REVIEW"
        const val PERSONAL_INFORMATION = "PERSONAL_INFORMATION"
        const val ACTIVE = "ACTIVE"
        val STATUSES = setOf(PENDING_CONSENT, PENDING_PROFILE, PENDING_REVIEW, "ACTIVE", "SUSPENDED", "WITHDRAWN")
        val METHODS = setOf("ONLINE", "WRITTEN")
        val MANAGED_ROLE_CODES = setOf("PRODUCT_MANAGER", "ORDER_MANAGER", "INVENTORY_MANAGER", "SHIPPING_MANAGER")
    }
}
