package com.buyeong.umji.api.account.model

import com.buyeong.umji.api.account.dto.OrganizationTaxInvoiceProfileCommandDto
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "Organization 공급받는자 세금계산서 정보 수정 요청")
data class OrganizationTaxInvoiceProfileRequest(
    @field:Size(max = 30,)
    @field:Schema(description = "공급받는자 사업자등록번호. 세금계산서 발행 전 필수", example = "123-45-67890", type = "string", required = false)
    val businessRegistrationNumber: String? = null,

    @field:NotBlank @field:Size(max = 200,)
    @field:Schema(description = "공급받는자 상호. 세금계산서 발행 시 필수", example = "엄지상사", type = "string", required = true)
    val businessName: String,

    @field:Size(max = 100,)
    @field:Schema(description = "공급받는자 성명. 세금계산서 발행 전 필수", example = "홍길동", type = "string", required = false)
    val representativeName: String? = null,

    @field:Size(max = 20,)
    @field:Schema(description = "공급받는자 우편번호. 세금계산서 발행 전 사업자주소와 함께 필수", example = "06234", type = "string", required = false)
    val postalCode: String? = null,

    @field:Size(max = 255,)
    @field:Schema(description = "공급받는자 사업자주소 기본 주소. 세금계산서 발행 전 필수", example = "서울특별시 강남구 테헤란로 1", type = "string", required = false)
    val address1: String? = null,

    @field:Size(max = 255,)
    @field:Schema(description = "공급받는자 사업자주소 상세 주소", example = "101호", type = "string", required = false)
    val address2: String? = null,

    @field:Size(max = 100,)
    @field:Schema(description = "공급받는자 업태. 세금계산서 발행 전 필수", example = "도소매업", type = "string", required = false)
    val businessIndustry: String? = null,

    @field:Size(max = 100,)
    @field:Schema(description = "공급받는자 종목. 세금계산서 발행 전 필수", example = "철물·공구", type = "string", required = false)
    val businessItem: String? = null,

    @field:Email @field:Size(max = 255,)
    @field:Schema(description = "공급받는자 세금계산서 수신 이메일. 선택 항목", example = "billing@example.com", type = "string", required = false)
    val email: String? = null,
) {
    fun toCommand() = OrganizationTaxInvoiceProfileCommandDto(
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