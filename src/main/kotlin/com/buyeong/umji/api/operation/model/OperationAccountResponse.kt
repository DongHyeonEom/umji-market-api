package com.buyeong.umji.api.operation.model

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

@Schema(description = "OperationAccountResponse API 데이터 모델")
data class OperationAccountResponse(
    @field:Schema(description = "리소스 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true) val id: UUID,
    @field:Schema(description = "이름 또는 표시 이름", example = "홍길동", type = "string", required = true) val name: String,
    @field:Schema(description = "휴대폰 번호", example = "01012345678", type = "string", required = true) val phone: String?,
    @field:Schema(description = "이메일 주소", example = "user@example.com", type = "string", required = true) val email: String?,
    @field:Schema(description = "현재 상태 코드", example = "ACTIVE", type = "string", required = true) val status: String,
    @field:Schema(description = "Token Version 정보", example = "1", format = "int64", type = "integer", required = true, implementation = Long::class) val tokenVersion: Long,
    @field:Schema(description = "Organization 공통 사업자 프로필", example = "예시 값", type = "object", required = true) val organizationProfile: OperationOrganizationProfileResponse?,
    @field:ArraySchema(
        schema = Schema(implementation = OperationConsentResponse::class),
    ) @field:Schema(description = "Consents 정보", example = "[]", type = "array", required = true) val consents: List<OperationConsentResponse> = emptyList(),
    @field:Schema(
        description = "Organization 공개 식별자(UUID)",
        example = "00000000-0000-0000-0000-000000000001",
        format = "uuid",
        type = "string",
        required = true,
    ) val organizationId: UUID? =
        null,
    @field:Schema(description = "Organization capability 목록", example = "[\"SELLER\"]", required = true)
    val organizationCapabilities: Set<String> = emptySet(),
)

@Schema(description = "OperationOrganizationProfileResponse API 데이터 모델")
data class OperationOrganizationProfileResponse(
    @field:Schema(description = "사업자 상호명", example = "예시 값", type = "string", required = true) val businessName: String,
    @field:Schema(description = "사업자 등록 번호", example = "예시 값", type = "string", required = true) val businessRegistrationNumber: String?,
    @field:Schema(description = "사업자 대표자 이름", example = "예시 값", type = "string", required = true) val representativeName: String?,
    @field:Schema(description = "사업자 연락처", example = "예시 값", type = "string", required = true) val businessPhone: String?,
    @field:Schema(description = "우편번호", example = "예시 값", type = "string", required = true) val postalCode: String?,
    @field:Schema(description = "기본 주소", example = "예시 값", type = "string", required = true) val address1: String?,
    @field:Schema(description = "상세 주소", example = "예시 값", type = "string", required = true) val address2: String?,
    @field:Schema(description = "현재 상태 코드", example = "ACTIVE", type = "string", required = true) val status: String,
)

@Schema(description = "OperationConsentResponse API 데이터 모델")
data class OperationConsentResponse(
    @field:Schema(description = "Consent Type 정보", example = "예시 값", type = "string", required = true) val consentType: String,
    @field:Schema(description = "Document Version 정보", example = "예시 값", type = "string", required = true) val documentVersion: String,
    @field:Schema(description = "Consent Method 정보", example = "예시 값", type = "string", required = true) val consentMethod: String,
    @field:Schema(description = "Evidence Reference 정보", example = "예시 값", type = "string", required = true) val evidenceReference: String?,
    @field:Schema(description = "Processed By 정보", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true) val processedBy: UUID?,
    @field:Schema(description = "Consented At 정보", example = "2026-10-04T09:00:00Z", format = "date-time", type = "string", required = true) val consentedAt: Instant,
)

@Schema(description = "OperationRoleResponse API 데이터 모델")
data class OperationRoleResponse(
    @field:Schema(description = "고유 코드", example = "예시 값", type = "string", required = true) val code: String,
    @field:Schema(description = "구매자 그룹 표시 이름", example = "예시 그룹", type = "string", required = true) val name: String,
)
