package com.buyeong.umji.api.operation.account.adapter.`in`.web

import com.buyeong.umji.api.operation.account.application.model.*
import com.buyeong.umji.api.operation.account.application.port.`in`.OperationAccountUseCase
import com.buyeong.umji.api.operation.model.*
import com.buyeong.umji.api.util.PhoneNumberHelper
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/operation/accounts")
@Validated
class OperationAccountController(private val useCase: OperationAccountUseCase) {
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: CreateOperationAccountRequest) = useCase.create(
        NewAccount(request.name.trim(), request.phone.trim(), PhoneNumberHelper.normalizeMobilePhoneNumber(request.phone), request.email.clean(), request.businessProfile?.toData()),
    ).toResponse()

    @GetMapping
    fun list(@RequestParam(required = false) status: String?, @RequestParam(defaultValue = "0") @Min(0) page: Int, @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int) =
        useCase.list(status, page, size).map { it.toResponse() }

    @GetMapping("/{id}") fun detail(@PathVariable id: UUID) = useCase.detail(id).toResponse()
    @PatchMapping("/{id}/status") fun status(@PathVariable id: UUID, @Valid @RequestBody request: UpdateAccountStatusRequest) = useCase.status(id, request.status).toResponse()
    @PutMapping("/{id}/business-profile") fun profile(@PathVariable id: UUID, @Valid @RequestBody request: BusinessProfileRequest) = useCase.profile(id, request.toData()).toResponse()
    @PostMapping("/{id}/consents") fun consent(@PathVariable id: UUID, @Valid @RequestBody request: CreateConsentRequest) = useCase.consent(id, ConsentCommand(request.consentType, request.documentVersion, request.consentMethod, request.evidenceReference.clean())).toResponse()
    @PostMapping("/{id}/approve") fun approve(@PathVariable id: UUID) = useCase.approve(id).toResponse()

    private fun BusinessProfileRequest.toData() = BusinessProfileData(businessName.trim(), businessRegistrationNumber.clean(), representativeName.clean(), businessPhone.clean(), postalCode.clean(), address1.clean(), address2.clean())
    private fun BusinessProfileData.toResponse() = OperationBusinessProfileResponse(businessName, businessRegistrationNumber, representativeName, businessPhone, postalCode, address1, address2, status)
    private fun AccountData.toResponse() = OperationAccountResponse(id, name, phone, email, status, tokenVersion, profile?.toResponse(), consents.map { OperationConsentResponse(it.consentType, it.documentVersion, it.consentMethod, it.evidenceReference, it.consentedAt) })
    private fun String?.clean() = this?.trim()?.ifBlank { null }
}
