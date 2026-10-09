package com.buyeong.umji.api.domain.operation.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID

@Schema(description = "CreateCategoryRequest API 데이터 모델")
data class CreateCategoryRequest(
    @field:NotBlank @field:Size(max = 100,)
    @field:Schema(description = "이름 또는 표시 이름", example = "홍길동", type = "string", required = true)
    val name: String,

    @field:Schema(description = "Parent Id 정보", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = false)
    val parentId: UUID? = null,

    @field:Min(
        0,
    ) @field:Schema(description = "화면 표시 순서", example = "1", format = "int32", type = "integer", required = false, implementation = Int::class)
    val displayOrder: Int = 0,

    @field:NotBlank
    @field:Schema(description = "화면 노출 상태 코드", example = "예시 값", type = "string", required = false)
    val displayStatus: String = "HIDDEN",
)