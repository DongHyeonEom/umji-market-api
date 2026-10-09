package com.buyeong.umji.api.account.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "개인 Organization 생성 요청")
data class CreateIndividualGroupRequest(
    @field:NotBlank
    @field:Schema(description = "개인 Organization 표시 이름", example = "홍길동", type = "string", required = true)
    val name: String,
)