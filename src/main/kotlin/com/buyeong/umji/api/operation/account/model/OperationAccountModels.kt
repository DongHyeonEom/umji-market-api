package com.buyeong.umji.api.operation.account.model

import java.time.Instant
import java.util.UUID

data class BusinessProfileData(
    val businessName: String,
    val businessRegistrationNumber: String?,
    val representativeName: String?,
    val businessPhone: String?,
    val postalCode: String?,
    val address1: String?,
    val address2: String?,
    val status: String = "COMPLETED",
)
data class ConsentData(
    val consentType: String,
    val documentVersion: String,
    val consentMethod: String,
    val evidenceReference: String?,
    val processedBy: UUID?,
    val consentedAt: Instant,
)
data class AccountData(
    val id: UUID,
    val name: String,
    val phone: String?,
    val email: String?,
    val status: String,
    val tokenVersion: Long,
    val profile: BusinessProfileData? = null,
    val consents: List<ConsentData> = emptyList(),
    val buyerGroupId: UUID? = null,
)
data class NewAccount(val name: String, val phone: String, val normalizedPhone: String, val email: String?, val profile: BusinessProfileData?)
data class ConsentCommand(
    val consentType: String,
    val documentVersion: String,
    val consentMethod: String,
    val evidenceReference: String?,
    val processedBy: UUID,
)
data class ManagedRole(val code: String, val name: String)
