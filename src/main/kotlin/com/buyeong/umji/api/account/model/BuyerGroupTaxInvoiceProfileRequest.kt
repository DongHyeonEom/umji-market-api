package com.buyeong.umji.api.account.model

import com.buyeong.umji.api.account.model.BuyerGroupTaxInvoiceProfile
import com.buyeong.umji.api.account.model.BuyerGroupTaxInvoiceProfileCommand
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

@Schema(description = "구매자 그룹 공급받는자 세금계산서 정보 수정 요청")
data class BuyerGroupTaxInvoiceProfileRequest(
    @field:Size(max = 30) @field:Schema(description = "공급받는자 사업자등록번호. 세금계산서 발행 전 필수", example = "123-45-67890", type = "string", required = false) val businessRegistrationNumber: String? = null,
    @field:NotBlank @field:Size(max = 200) @field:Schema(description = "공급받는자 상호. 세금계산서 발행 시 필수", example = "엄지상사", type = "string", required = true) val businessName: String,
    @field:Size(max = 100) @field:Schema(description = "공급받는자 성명. 세금계산서 발행 전 필수", example = "홍길동", type = "string", required = false) val representativeName: String? = null,
    @field:Size(max = 20) @field:Schema(description = "공급받는자 우편번호. 세금계산서 발행 전 사업자주소와 함께 필수", example = "06234", type = "string", required = false) val postalCode: String? = null,
    @field:Size(max = 255) @field:Schema(description = "공급받는자 사업자주소 기본 주소. 세금계산서 발행 전 필수", example = "서울특별시 강남구 테헤란로 1", type = "string", required = false) val address1: String? = null,
    @field:Size(max = 255) @field:Schema(description = "공급받는자 사업자주소 상세 주소", example = "101호", type = "string", required = false) val address2: String? = null,
    @field:Size(max = 100) @field:Schema(description = "공급받는자 업태. 세금계산서 발행 전 필수", example = "도소매업", type = "string", required = false) val businessIndustry: String? = null,
    @field:Size(max = 100) @field:Schema(description = "공급받는자 종목. 세금계산서 발행 전 필수", example = "철물·공구", type = "string", required = false) val businessItem: String? = null,
    @field:Email @field:Size(max = 255) @field:Schema(description = "공급받는자 세금계산서 수신 이메일. 선택 항목", example = "billing@example.com", type = "string", required = false) val email: String? = null,
) {
    fun toCommand() = BuyerGroupTaxInvoiceProfileCommand(
        businessRegistrationNumber,
        businessName,
        representativeName,
        postalCode,
        address1,
        address2,
        businessIndustry,
        businessItem,
        email,
    )
}

@Schema(description = "현재 구매자 그룹의 공급받는자 세금계산서 정보")
data class BuyerGroupTaxInvoiceProfileResponse(
    @field:Schema(description = "구매자 그룹 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true) val buyerGroupId: UUID,
    @field:Schema(description = "구매자 그룹 유형", example = "BUSINESS", type = "string", required = true) val groupType: String,
    @field:Schema(description = "공급받는자 사업자등록번호", example = "123-45-67890", type = "string", required = true) val businessRegistrationNumber: String?,
    @field:Schema(description = "공급받는자 상호", example = "엄지상사", type = "string", required = true) val businessName: String?,
    @field:Schema(description = "공급받는자 성명", example = "홍길동", type = "string", required = true) val representativeName: String?,
    @field:Schema(description = "공급받는자 우편번호", example = "06234", type = "string", required = true) val postalCode: String?,
    @field:Schema(description = "공급받는자 사업자주소 기본 주소", example = "서울특별시 강남구 테헤란로 1", type = "string", required = true) val address1: String?,
    @field:Schema(description = "공급받는자 사업자주소 상세 주소", example = "101호", type = "string", required = true) val address2: String?,
    @field:Schema(description = "공급받는자 업태", example = "도소매업", type = "string", required = true) val businessIndustry: String?,
    @field:Schema(description = "공급받는자 종목", example = "철물·공구", type = "string", required = true) val businessItem: String?,
    @field:Schema(description = "공급받는자 세금계산서 이메일", example = "billing@example.com", type = "string", required = true) val email: String?,
    @field:Schema(description = "세금계산서 발행 필수 정보 완성 여부", example = "true", type = "boolean", required = true, implementation = Boolean::class) val complete: Boolean,
    @field:Schema(
        description = "사업자등록 상태조회 결과: PENDING, ACTIVE, TEMPORARILY_CLOSED, CLOSED, UNKNOWN, ERROR, NOT_REQUIRED",
        example = "ACTIVE",
        type = "string",
        required = true,
    ) val businessRegistrationVerificationStatus: String,
    @field:Schema(description = "사업자등록 상태조회 완료 시각", type = "string", format = "date-time", required = false)
    val businessRegistrationVerifiedAt: Instant?,
    @field:Schema(description = "대표자의 사업자정보 확인 시각", type = "string", format = "date-time", required = false)
    val businessRegistrationConfirmedAt: Instant?,
)

fun BuyerGroupTaxInvoiceProfile.toResponse() = BuyerGroupTaxInvoiceProfileResponse(
    buyerGroupId,
    groupType,
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
