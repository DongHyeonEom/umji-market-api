package com.buyeong.umji.api.operation.account.model

import java.time.Instant
import java.util.UUID

data class OrganizationProfileData(
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
    val profile: OrganizationProfileData? = null,
    val consents: List<ConsentData> = emptyList(),
    val organizationId: UUID? = null,
    val organizationCapabilities: Set<String> = emptySet(),
)
data class NewAccount(
    val name: String,
    val phone: String,
    val normalizedPhone: String,
    val email: String?,
    val profile: OrganizationProfileData?,
    val organizationCapability: String = "BUYER",
)
data class ConsentCommand(
    val consentType: String,
    val documentVersion: String,
    val consentMethod: String,
    val evidenceReference: String?,
    val processedBy: UUID,
)
data class ManagedRole(val code: String, val name: String)
