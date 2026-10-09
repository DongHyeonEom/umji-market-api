package com.buyeong.umji.api.operation.model

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "OperationCategoryResponse API 데이터 모델")
data class OperationCategoryResponse(
    @field:Schema(description = "리소스 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true)
    val id: UUID,

    @field:Schema(description = "Parent Id 정보", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true)
    val parentId: UUID?,

    @field:Schema(description = "이름 또는 표시 이름", example = "홍길동", type = "string", required = true)
    val name: String,

    @field:Schema(description = "Path 정보", example = "예시 값", type = "string", required = true)
    val path: String,

    @field:Schema(description = "Depth 정보", example = "1", format = "int32", type = "integer", required = true, implementation = Int::class)
    val depth: Int,

    @field:Schema(description = "화면 표시 순서", example = "1", format = "int32", type = "integer", required = true, implementation = Int::class)
    val displayOrder: Int,

    @field:Schema(description = "화면 노출 상태 코드", example = "예시 값", type = "string", required = true)
    val displayStatus: String,
)