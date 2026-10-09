package com.buyeong.umji.api.operation.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.util.UUID

@Schema(description = "OrganizationProfileRequest API 데이터 모델")
data class OrganizationProfileRequest(
    @field:NotBlank @field:Size(max = 200,)
    @field:Schema(description = "사업자 상호명", example = "예시 값", type = "string", required = true)
    val businessName: String,

    @field:Size(max = 30,)
    @field:Schema(description = "사업자 등록 번호", example = "예시 값", type = "string", required = false)
    val businessRegistrationNumber: String? = null,

    @field:Size(max = 100,)
    @field:Schema(description = "사업자 대표자 이름", example = "예시 값", type = "string", required = false)
    val representativeName: String? = null,

    @field:Size(max = 30,)
    @field:Schema(description = "사업자 연락처", example = "예시 값", type = "string", required = false)
    val businessPhone: String? = null,

    @field:Size(max = 20,)
    @field:Schema(description = "우편번호", example = "예시 값", type = "string", required = false)
    val postalCode: String? = null,

    @field:Size(max = 255,)
    @field:Schema(description = "기본 주소", example = "예시 값", type = "string", required = false)
    val address1: String? = null,

    @field:Size(max = 255,)
    @field:Schema(description = "상세 주소", example = "예시 값", type = "string", required = false)
    val address2: String? = null,
)

@Schema(description = "CreateOperationAccountRequest API 데이터 모델")
data class CreateOperationAccountRequest(
    @field:NotBlank @field:Size(max = 100,)
    @field:Schema(description = "이름 또는 표시 이름", example = "홍길동", type = "string", required = true)
    val name: String,

    @field:NotBlank @field:Size(max = 30,)
    @field:Schema(description = "휴대폰 번호", example = "01012345678", type = "string", required = true)
    val phone: String,

    @field:Size(max = 255,)
    @field:Schema(description = "이메일 주소", example = "user@example.com", type = "string", required = false)
    val email: String? = null,

    @field:Valid
    @field:Schema(description = "Organization 공통 사업자 프로필", example = "예시 값", type = "object", required = false)
    val organizationProfile: OrganizationProfileRequest? = null,

    @field:jakarta.validation.constraints.Pattern(regexp = "BUYER|SELLER|OPERATOR")
    @field:Schema(description = "계정이 대표로 생성할 Organization capability", example = "SELLER", required = false)
    val organizationCapability: String = "BUYER",
)

@Schema(description = "UpdateAccountStatusRequest API 데이터 모델")
data class UpdateAccountStatusRequest(
    @field:NotBlank
    @field:Schema(description = "현재 상태 코드", example = "ACTIVE", type = "string", required = true)
    val status: String,
)

@Schema(description = "AssignOrganizationRequest 요청 또는 응답 모델")
data class AssignOrganizationRequest(
    @field:NotNull @field:Schema(
        description = "Organization 공개 식별자(UUID)",
        example = "00000000-0000-0000-0000-000000000001",
        format = "uuid",
        type = "string",
        required = true,
    )
    val organizationId: UUID,
)

@Schema(description = "CreateConsentRequest API 데이터 모델")
data class CreateConsentRequest(
    @field:NotBlank @field:Size(max = 100,)
    @field:Schema(description = "Consent Type 정보", example = "예시 값", type = "string", required = true)
    val consentType: String,

    @field:NotBlank @field:Size(max = 100,)
    @field:Schema(description = "Document Version 정보", example = "예시 값", type = "string", required = true)
    val documentVersion: String,

    @field:NotBlank
    @field:Schema(description = "Consent Method 정보", example = "예시 값", type = "string", required = true)
    val consentMethod: String,

    @field:Size(max = 500,)
    @field:Schema(description = "Evidence Reference 정보", example = "예시 값", type = "string", required = false)
    val evidenceReference: String? = null,
)
