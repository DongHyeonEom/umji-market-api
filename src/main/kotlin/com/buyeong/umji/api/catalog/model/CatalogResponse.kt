package com.buyeong.umji.api.catalog.model

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "CategoryResponse API 데이터 모델")
data class CategoryResponse(
    @field:Schema(description = "리소스 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true) val id: UUID,
    @field:Schema(description = "이름 또는 표시 이름", example = "홍길동", type = "string", required = true) val name: String,
    @field:Schema(description = "Path 정보", example = "예시 값", type = "string", required = true) val path: String,
    @field:Schema(description = "Depth 정보", example = "1", format = "int32", type = "integer", required = true, implementation = Int::class) val depth: Int,
    @field:Schema(description = "판매 채널 코드", example = "WHOLESALE", type = "string", required = true) val channelCode: String = "WHOLESALE",
)

@Schema(description = "ProductSummaryResponse API 데이터 모델")
data class ProductSummaryResponse(
    @field:Schema(description = "리소스 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true) val id: UUID,
    @field:Schema(description = "이름 또는 표시 이름", example = "홍길동", type = "string", required = true) val name: String,
    @field:Schema(description = "Brand Name 정보", example = "예시 값", type = "string", required = true) val brandName: String?,
    @field:Schema(description = "판매 채널 코드", example = "WHOLESALE", type = "string", required = true) val channelCode: String = "WHOLESALE",
    @field:Schema(description = "채널 최저 판매 단위 가격(원). WHOLESALE은 박스당 가격", example = "12000", type = "integer", required = false) val startingPrice: Long? = null,
    @field:Schema(description = "시작 판매가 오퍼의 판매 단위당 입수 수량", example = "12", type = "integer", required = true, implementation = Int::class) val startingUnitsPerSale: Int = 1,
)

@Schema(description = "ProductSkuResponse API 데이터 모델")
data class ProductSkuResponse(
    @field:Schema(description = "리소스 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true) val id: UUID,
    @field:Schema(description = "상품 옵션 코드", example = "예시 값", type = "string", required = true) val skuCode: String,
    @field:Schema(description = "이름 또는 표시 이름", example = "홍길동", type = "string", required = true) val name: String,
    @field:Schema(description = "판매 단위 1개의 가격(원). WHOLESALE은 박스당 가격", example = "12000", format = "int64", type = "integer", required = true, implementation = Long::class) val salePrice: Long,
    @field:Schema(description = "정가(원)", example = "1", format = "int64", type = "integer", required = true, implementation = Long::class) val listPrice: Long?,
    @field:Schema(description = "채널별 판매 오퍼 공개 UUID", format = "uuid", type = "string", required = true) val salesOfferId: UUID? = null,
    @field:Schema(
        description = "판매 단위당 기준 SKU 입수 수량. WHOLESALE은 박스 입수 수량",
        example = "12",
        type = "integer",
        required = true,
        implementation = Int::class,
    ) val unitsPerSale: Int = 1,
)

@Schema(description = "ProductDetailResponse API 데이터 모델")
data class ProductDetailResponse(
    @field:Schema(description = "리소스 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true) val id: UUID,
    @field:Schema(description = "이름 또는 표시 이름", example = "홍길동", type = "string", required = true) val name: String,
    @field:Schema(description = "상세 설명", example = "예시 값", type = "string", required = true) val description: String?,
    @field:Schema(description = "Category Name 정보", example = "예시 값", type = "string", required = true) val categoryName: String,
    @field:Schema(description = "Brand Name 정보", example = "예시 값", type = "string", required = true) val brandName: String?,
    @field:ArraySchema(
        schema = Schema(implementation = ProductSkuResponse::class),
    ) @field:Schema(description = "상품 SKU 목록", example = "[]", type = "array", required = true) val skus: List<ProductSkuResponse>,
    @field:Schema(description = "판매 채널 코드", example = "WHOLESALE", type = "string", required = true) val channelCode: String = "WHOLESALE",
)

@Schema(description = "상품 검색 결과와 페이지 정보")
data class ProductPageResponse(
    @field:ArraySchema(
        schema = Schema(implementation = ProductSummaryResponse::class),
    ) @field:Schema(description = "현재 페이지의 항목 목록", example = "[]", type = "array", required = true) val items: List<ProductSummaryResponse>,
    @field:Schema(description = "페이지 번호(0부터 시작)", example = "1", format = "int32", type = "integer", required = true, implementation = Int::class) val page: Int,
    @field:Schema(description = "페이지당 항목 수", example = "1", format = "int32", type = "integer", required = true, implementation = Int::class) val size: Int,
    @field:Schema(description = "전체 검색 결과 수", example = "1", format = "int64", type = "integer", required = true, implementation = Long::class) val totalElements: Long,
    @field:Schema(description = "전체 페이지 수", example = "1", format = "int32", type = "integer", required = true, implementation = Int::class) val totalPages: Int,
)
