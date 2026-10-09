package com.buyeong.umji.api.operation.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

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