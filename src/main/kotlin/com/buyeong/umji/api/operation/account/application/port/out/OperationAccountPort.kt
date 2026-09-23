package com.buyeong.umji.api.operation.account.application.port.out

import com.buyeong.umji.api.operation.account.application.model.*
import java.util.UUID

interface OperationAccountPort {
    fun create(command: NewAccount): AccountData
    fun find(id: UUID): AccountData?
    fun list(status: String?, page: Int, size: Int): List<AccountData>
    fun updateStatus(id: UUID, status: String): AccountData?
    fun updateProfile(id: UUID, profile: BusinessProfileData, nextStatus: String): AccountData?
    fun addConsent(id: UUID, consent: ConsentCommand, nextStatus: String): AccountData?
    fun hasConsent(id: UUID, consentType: String): Boolean
    fun approve(id: UUID): AccountData?
}
