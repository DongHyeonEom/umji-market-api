package com.buyeong.umji.api.account.model

import com.buyeong.umji.api.account.dto.OrganizationTaxInvoiceProfileDto
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

@Schema(description = "현재 Organization의 공급받는자 세금계산서 정보")
data class OrganizationTaxInvoiceProfileResponse(
    @field:Schema(description = "Organization 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true)
    val organizationId: UUID,

    @field:Schema(description = "Organization 유형", example = "BUSINESS", type = "string", required = true)
    val organizationType: String,

    @field:Schema(description = "공급받는자 사업자등록번호", example = "123-45-67890", type = "string", required = true)
    val businessRegistrationNumber: String?,

    @field:Schema(description = "공급받는자 상호", example = "엄지상사", type = "string", required = true)
    val businessName: String?,

    @field:Schema(description = "공급받는자 성명", example = "홍길동", type = "string", required = true)
    val representativeName: String?,

    @field:Schema(description = "공급받는자 우편번호", example = "06234", type = "string", required = true)
    val postalCode: String?,

    @field:Schema(description = "공급받는자 사업자주소 기본 주소", example = "서울특별시 강남구 테헤란로 1", type = "string", required = true)
    val address1: String?,

    @field:Schema(description = "공급받는자 사업자주소 상세 주소", example = "101호", type = "string", required = true)
    val address2: String?,

    @field:Schema(description = "공급받는자 업태", example = "도소매업", type = "string", required = true)
    val businessIndustry: String?,

    @field:Schema(description = "공급받는자 종목", example = "철물·공구", type = "string", required = true)
    val businessItem: String?,

    @field:Schema(description = "공급받는자 세금계산서 이메일", example = "billing@example.com", type = "string", required = true)
    val email: String?,

    @field:Schema(description = "세금계산서 발행 필수 정보 완성 여부", example = "true", type = "boolean", required = true, implementation = Boolean::class)
    val complete: Boolean,

    @field:Schema(
        description = "사업자등록 상태조회 결과: PENDING, ACTIVE, TEMPORARILY_CLOSED, CLOSED, UNKNOWN, ERROR, NOT_REQUIRED",
        example = "ACTIVE",
        type = "string",
        required = true,
    )
    val businessRegistrationVerificationStatus: String,

    @field:Schema(description = "사업자등록 상태조회 완료 시각", type = "string", format = "date-time", required = false)
    val businessRegistrationVerifiedAt: Instant?,

    @field:Schema(description = "대표자의 사업자정보 확인 시각", type = "string", format = "date-time", required = false)
    val businessRegistrationConfirmedAt: Instant?,
)

fun OrganizationTaxInvoiceProfileDto.toResponse() = OrganizationTaxInvoiceProfileResponse(
    organizationId,
    organizationType,
    businessRegistrationNumber,
    businessName,
    representativeName,
    postalCode,
    address1,
    address2,
    businessIndustry,
    businessItem,
    email,
    complete,
    businessRegistrationVerificationStatus,
    businessRegistrationVerifiedAt,
    businessRegistrationConfirmedAt,
)