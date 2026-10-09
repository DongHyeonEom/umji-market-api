package com.buyeong.umji.api.domain.operation.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.util.UUID

@Schema(description = "UpdateProductRequest API 데이터 모델")
data class UpdateProductRequest(
    @field:NotNull @field:Schema(
        description = "카테고리 공개 식별자(UUID)",
        example = "00000000-0000-0000-0000-000000000001",
        format = "uuid",
        type = "string",
        required = true,
    )
    val categoryId: UUID,

    @field:Schema(description = "브랜드 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = false)
    val brandId: UUID? = null,

    @field:NotBlank @field:Size(max = 200,)
    @field:Schema(description = "이름 또는 표시 이름", example = "홍길동", type = "string", required = true)
    val name: String,

    @field:Schema(description = "상세 설명", example = "예시 값", type = "string", required = false)
    val description: String? = null,

    @field:NotBlank
    @field:Schema(description = "화면 노출 상태 코드", example = "예시 값", type = "string", required = true)
    val displayStatus: String,

    @field:NotBlank
    @field:Schema(description = "판매 상태 코드", example = "예시 값", type = "string", required = true)
    val salesStatus: String,

    @field:Min(
        0,
    ) @field:Schema(description = "화면 표시 순서", example = "1", format = "int32", type = "integer", required = false, implementation = Int::class)
    val displayOrder: Int = 0,
)