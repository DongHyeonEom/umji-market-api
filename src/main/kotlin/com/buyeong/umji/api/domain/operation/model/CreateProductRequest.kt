package com.buyeong.umji.api.domain.operation.model

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.util.UUID

@Schema(description = "CreateProductRequest API 데이터 모델")
data class CreateProductRequest(
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
    @field:Schema(description = "화면 노출 상태 코드", example = "예시 값", type = "string", required = false)
    val displayStatus: String = "HIDDEN",

    @field:NotBlank
    @field:Schema(description = "판매 상태 코드", example = "예시 값", type = "string", required = false)
    val salesStatus: String = "ON_SALE",

    @field:Min(
        0,
    ) @field:Schema(description = "화면 표시 순서", example = "1", format = "int32", type = "integer", required = false, implementation = Int::class)
    val displayOrder: Int = 0,

    @field:ArraySchema(
        schema = Schema(implementation = CreateProductImageRequest::class),
    ) @field:Schema(description = "상품 이미지 목록", example = "[]", type = "array", required = false)
    val images: List<@Valid CreateProductImageRequest> = emptyList(),

    @field:ArraySchema(
        schema = Schema(implementation = CreateProductOptionRequest::class),
    ) @field:Schema(description = "상품 옵션 목록", example = "[]", type = "array", required = false)
    val options: List<@Valid CreateProductOptionRequest> = emptyList(),

    @field:NotEmpty @field:ArraySchema(
        schema = Schema(implementation = CreateProductSkuRequest::class),
    ) @field:Schema(description = "상품 SKU 목록", example = "[]", type = "array", required = true)
    val skus: List<@Valid CreateProductSkuRequest>,
)