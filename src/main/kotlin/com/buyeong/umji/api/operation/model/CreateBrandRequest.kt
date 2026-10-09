package com.buyeong.umji.api.operation.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "CreateBrandRequest API 데이터 모델")
data class CreateBrandRequest(
    @field:NotBlank @field:Size(max = 100,)
    @field:Schema(description = "이름 또는 표시 이름", example = "홍길동", type = "string", required = true)
    val name: String,

    @field:NotBlank
    @field:Schema(description = "화면 노출 상태 코드", example = "예시 값", type = "string", required = false)
    val displayStatus: String = "HIDDEN",
)