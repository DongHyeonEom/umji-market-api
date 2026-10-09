package com.buyeong.umji.api.domain.operation.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "OperationRoleResponse API 데이터 모델")
data class OperationRoleResponse(
    @field:Schema(description = "고유 코드", example = "예시 값", type = "string", required = true)
    val code: String,

    @field:Schema(description = "구매자 그룹 표시 이름", example = "예시 그룹", type = "string", required = true)
    val name: String,
)