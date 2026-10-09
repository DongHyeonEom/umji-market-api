package com.buyeong.umji.api.operation.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

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