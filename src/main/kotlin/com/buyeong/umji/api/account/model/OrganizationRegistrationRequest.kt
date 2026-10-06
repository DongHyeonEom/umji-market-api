package com.buyeong.umji.api.account.model

import com.buyeong.umji.api.account.model.BusinessGroupRegistration
import com.buyeong.umji.api.account.model.OrganizationRegistrationCommand
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

@Schema(description = "사용자 Organization 최초 등록 요청")
data class OrganizationRegistrationRequest(
    @field:Pattern(regexp = "INDIVIDUAL|BUSINESS")
    @field:Schema(description = "Organization 유형", example = "BUSINESS", required = true)
    val type: String,
    @field:Pattern(regexp = "BUYER|SELLER")
    @field:Schema(description = "Organization capability", example = "SELLER", required = false)
    val capability: String = "BUYER",
    @field:Valid
    @field:Schema(
        description = "사업자 그룹 선택 시 입력할 사업자등록 내용",
        example = "{\"businessRegistrationNumber\":\"123-45-67890\",\"businessName\":\"엄지상사\",\"representativeName\":\"홍길동\",\"postalCode\":\"06234\",\"address1\":\"서울특별시 강남구 테헤란로 1\",\"businessIndustry\":\"도소매업\",\"businessItem\":\"철물·공구\",\"confirmed\":true}",
        required = false,
    )
    val business: BusinessGroupRegistrationRequest? = null,
) {
    fun toCommand() = OrganizationRegistrationCommand(type, business?.toCommand(), capability)
}

@Schema(description = "사업자 그룹 등록을 위한 사업자등록 내용")
data class BusinessGroupRegistrationRequest(
    @field:NotBlank @field:Size(max = 30)
    @field:Schema(description = "사업자등록번호", example = "123-45-67890", required = true)
    val businessRegistrationNumber: String,
    @field:NotBlank @field:Size(max = 200)
    @field:Schema(description = "상호", example = "엄지상사", required = true)
    val businessName: String,
    @field:NotBlank @field:Size(max = 100)
    @field:Schema(description = "대표자 성명", example = "홍길동", required = true)
    val representativeName: String,
    @field:NotBlank @field:Size(max = 20)
    @field:Schema(description = "사업자등록 주소 우편번호", example = "06234", required = true)
    val postalCode: String,
    @field:NotBlank @field:Size(max = 255)
    @field:Schema(description = "사업자등록 주소", example = "서울특별시 강남구 테헤란로 1", required = true)
    val address1: String,
    @field:Size(max = 255)
    @field:Schema(description = "사업자등록 상세 주소", example = "101호", required = false)
    val address2: String? = null,
    @field:NotBlank @field:Size(max = 100)
    @field:Schema(description = "업태", example = "도소매업", required = true)
    val businessIndustry: String,
    @field:NotBlank @field:Size(max = 100)
    @field:Schema(description = "종목", example = "철물·공구", required = true)
    val businessItem: String,
    @field:Email @field:Size(max = 255)
    @field:Schema(description = "세금계산서 이메일", example = "billing@example.com", required = false)
    val email: String? = null,
    @field:Schema(description = "사업자등록 내용을 확인했는지 여부", example = "true", required = true)
    val confirmed: Boolean,
) {
    fun toCommand() = BusinessGroupRegistration(
        businessRegistrationNumber,
        businessName,
        representativeName,
        postalCode,
        address1,
        address2,
        businessIndustry,
        businessItem,
        email,
        confirmed,
    )
}
