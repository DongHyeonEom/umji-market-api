package com.buyeong.umji.api.operation.model

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class BusinessProfileRequest(
    @field:NotBlank @field:Size(
        max = 200,
    ) val businessName: String,
    @field:Size(
        max = 30,
    ) val businessRegistrationNumber: String? = null,
    @field:Size(
        max = 100,
    ) val representativeName: String? = null,
    @field:Size(
        max = 30,
    ) val businessPhone: String? = null,
    @field:Size(
        max = 20,
    ) val postalCode: String? = null,
    @field:Size(max = 255) val address1: String? = null,
    @field:Size(max = 255) val address2: String? = null,
)
data class CreateOperationAccountRequest(
    @field:NotBlank @field:Size(max = 100) val name: String,
    @field:NotBlank @field:Size(max = 30) val phone: String,
    @field:Size(max = 255) val email: String? = null,
    @field:Valid val businessProfile: BusinessProfileRequest? = null,
)
data class UpdateAccountStatusRequest(@field:NotBlank val status: String)
data class CreateConsentRequest(
    @field:NotBlank @field:Size(max = 100) val consentType: String,
    @field:NotBlank @field:Size(max = 100) val documentVersion: String,
    @field:NotBlank val consentMethod: String,
    @field:Size(max = 500) val evidenceReference: String? = null,
)