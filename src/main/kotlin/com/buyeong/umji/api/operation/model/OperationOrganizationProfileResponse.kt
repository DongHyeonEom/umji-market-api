package com.buyeong.umji.api.operation.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "OperationOrganizationProfileResponse API 데이터 모델")
data class OperationOrganizationProfileResponse(
    @field:Schema(description = "사업자 상호명", example = "예시 값", type = "string", required = true)
    val businessName: String,

    @field:Schema(description = "사업자 등록 번호", example = "예시 값", type = "string", required = true)
    val businessRegistrationNumber: String?,

    @field:Schema(description = "사업자 대표자 이름", example = "예시 값", type = "string", required = true)
    val representativeName: String?,

    @field:Schema(description = "사업자 연락처", example = "예시 값", type = "string", required = true)
    val businessPhone: String?,

    @field:Schema(description = "우편번호", example = "예시 값", type = "string", required = true)
    val postalCode: String?,

    @field:Schema(description = "기본 주소", example = "예시 값", type = "string", required = true)
    val address1: String?,

    @field:Schema(description = "상세 주소", example = "예시 값", type = "string", required = true)
    val address2: String?,

    @field:Schema(description = "현재 상태 코드", example = "ACTIVE", type = "string", required = true)
    val status: String,
)