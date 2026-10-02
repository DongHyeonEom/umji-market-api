package com.buyeong.umji.api.operation.account.application.port.out

import com.buyeong.umji.api.operation.account.application.model.AccountData
import com.buyeong.umji.api.operation.account.application.model.BusinessProfileData
import com.buyeong.umji.api.operation.account.application.model.ConsentCommand
import com.buyeong.umji.api.operation.account.application.model.ManagedRole
import com.buyeong.umji.api.operation.account.application.model.NewAccount
import java.util.UUID

interface OperationAccountPort {
    fun create(command: NewAccount): AccountData
    fun find(id: UUID): AccountData?
    fun list(status: String?, page: Int, size: Int): List<AccountData>
    fun updateStatus(id: UUID, status: String): AccountData?
    fun updateProfile(id: UUID, profile: BusinessProfileData, nextStatus: String): AccountData?
    fun assignBuyerGroup(id: UUID, buyerGroupId: UUID): AccountData?
    fun setBuyerGroupRepresentative(groupId: UUID, accountId: UUID)
    fun addConsent(id: UUID, consent: ConsentCommand, nextStatus: String): AccountData?
    fun hasConsent(id: UUID, consentType: String): Boolean
    fun approve(id: UUID): AccountData?
    fun managedRoles(): List<ManagedRole>
    fun roles(id: UUID): List<ManagedRole>?
    fun grantRole(id: UUID, roleCode: String, grantedBy: UUID): List<ManagedRole>?
    fun revokeRole(id: UUID, roleCode: String): List<ManagedRole>?
}