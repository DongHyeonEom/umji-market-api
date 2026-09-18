package com.buyeong.umji.api.operation.model

import java.time.Instant
import java.util.UUID

data class OperationAccountResponse(val id: UUID, val name: String, val phone: String?, val email: String?, val status: String, val tokenVersion: Long, val businessProfile: OperationBusinessProfileResponse?, val consents: List<OperationConsentResponse> = emptyList())
data class OperationBusinessProfileResponse(val businessName: String, val businessRegistrationNumber: String?, val representativeName: String?, val businessPhone: String?, val postalCode: String?, val address1: String?, val address2: String?, val status: String)
data class OperationConsentResponse(val consentType: String, val documentVersion: String, val consentMethod: String, val evidenceReference: String?, val consentedAt: Instant)
