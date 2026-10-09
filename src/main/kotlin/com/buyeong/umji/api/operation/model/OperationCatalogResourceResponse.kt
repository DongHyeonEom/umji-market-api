package com.buyeong.umji.api.operation.model

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "OperationCatalogResourceResponse API 데이터 모델")
data class OperationCatalogResourceResponse(
    @field:Schema(description = "리소스 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true)
    val id: UUID,
)