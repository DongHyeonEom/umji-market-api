package com.buyeong.umji.api.account.model

import com.buyeong.umji.api.account.dto.OrganizationRegistrationCommandDto
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Pattern

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
        example = "{\"businessRegistrationNumber\":\"123-45-67890\",\"businessName\":\"엄지상사\"," +
            "\"representativeName\":\"홍길동\",\"postalCode\":\"06234\",\"address1\":\"서울특별시 강남구 테헤란로 1\"," +
            "\"businessIndustry\":\"도소매업\",\"businessItem\":\"철물·공구\",\"confirmed\":true}",
        required = false,
    )
    val business: BusinessGroupRegistrationRequest? = null,
) {
    fun toCommand() = OrganizationRegistrationCommandDto(type, business?.toCommand(), capability)
}