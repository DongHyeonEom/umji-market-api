package com.buyeong.umji.api.operation.account.application.port.`in`

import com.buyeong.umji.api.operation.account.application.model.AccountData
import com.buyeong.umji.api.operation.account.application.model.BusinessProfileData
import com.buyeong.umji.api.operation.account.application.model.ConsentCommand
import com.buyeong.umji.api.operation.account.application.model.ManagedRole
import com.buyeong.umji.api.operation.account.application.model.NewAccount
import java.util.UUID

interface OperationAccountUseCase {
    fun create(command: NewAccount): AccountData
    fun list(status: String?, page: Int, size: Int): List<AccountData>
    fun detail(id: UUID): AccountData
    fun status(id: UUID, status: String): AccountData
    fun profile(id: UUID, profile: BusinessProfileData): AccountData
    fun assignBuyerGroup(id: UUID, buyerGroupId: UUID): AccountData
    fun consent(id: UUID, consent: ConsentCommand): AccountData
    fun approve(id: UUID): AccountData
    fun managedRoles(): List<ManagedRole>
    fun roles(id: UUID): List<ManagedRole>
    fun grantRole(id: UUID, roleCode: String, grantedBy: UUID): List<ManagedRole>
    fun revokeRole(id: UUID, roleCode: String): List<ManagedRole>
}