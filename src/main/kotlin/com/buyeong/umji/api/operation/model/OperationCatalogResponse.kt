package com.buyeong.umji.api.operation.model

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "OperationCatalogResourceResponse API 데이터 모델")
data class OperationCatalogResourceResponse(
    @field:Schema(description = "리소스 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true) val id: UUID,
)

@Schema(description = "OperationCategoryResponse API 데이터 모델")
data class OperationCategoryResponse(
    @field:Schema(description = "리소스 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true) val id: UUID,
    @field:Schema(description = "Parent Id 정보", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true) val parentId: UUID?,
    @field:Schema(description = "이름 또는 표시 이름", example = "홍길동", type = "string", required = true) val name: String,
    @field:Schema(description = "Path 정보", example = "예시 값", type = "string", required = true) val path: String,
    @field:Schema(description = "Depth 정보", example = "1", format = "int32", type = "integer", required = true, implementation = Int::class) val depth: Int,
    @field:Schema(description = "화면 표시 순서", example = "1", format = "int32", type = "integer", required = true, implementation = Int::class) val displayOrder: Int,
    @field:Schema(description = "화면 노출 상태 코드", example = "예시 값", type = "string", required = true) val displayStatus: String,
)

@Schema(description = "OperationBrandResponse API 데이터 모델")
data class OperationBrandResponse(
    @field:Schema(description = "리소스 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true) val id: UUID,
    @field:Schema(description = "이름 또는 표시 이름", example = "홍길동", type = "string", required = true) val name: String,
    @field:Schema(description = "화면 노출 상태 코드", example = "예시 값", type = "string", required = true) val displayStatus: String,
)

@Schema(description = "OperationProductSkuResponse API 데이터 모델")
data class OperationProductSkuResponse(
    @field:Schema(description = "리소스 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true) val id: UUID,
    @field:Schema(description = "상품 옵션 코드", example = "예시 값", type = "string", required = true) val skuCode: String,
    @field:Schema(description = "이름 또는 표시 이름", example = "홍길동", type = "string", required = true) val name: String,
    @field:Schema(description = "판매 가격(원)", example = "1", format = "int64", type = "integer", required = true, implementation = Long::class) val salePrice: Long,
    @field:Schema(description = "정가(원)", example = "1", format = "int64", type = "integer", required = true, implementation = Long::class) val listPrice: Long?,
    @field:Schema(description = "판매 상태 코드", example = "예시 값", type = "string", required = true) val salesStatus: String,
    @field:ArraySchema(
        schema = Schema(implementation = UUID::class),
    ) @field:Schema(description = "SKU에 연결할 상품 옵션 값 공개 식별자(UUID) 목록", example = "[]", type = "array", required = true) val optionValueIds: Set<UUID> = emptySet(),
)

@Schema(description = "OperationProductImageResponse API 데이터 모델")
data class OperationProductImageResponse(
    @field:Schema(description = "리소스 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true) val id: UUID,
    @field:Schema(description = "상품 이미지 저장소 키", example = "예시 값", type = "string", required = true) val storageKey: String,
    @field:Schema(description = "상품 이미지 대체 텍스트", example = "예시 값", type = "string", required = true) val altText: String?,
    @field:Schema(description = "화면 표시 순서", example = "1", format = "int32", type = "integer", required = true, implementation = Int::class) val displayOrder: Int,
)

@Schema(description = "OperationProductOptionValueResponse API 데이터 모델")
data class OperationProductOptionValueResponse(
    @field:Schema(description = "리소스 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true) val id: UUID,
    @field:Schema(description = "값", example = "예시 값", type = "string", required = true) val value: String,
    @field:Schema(description = "화면 표시 순서", example = "1", format = "int32", type = "integer", required = true, implementation = Int::class) val displayOrder: Int,
)

@Schema(description = "OperationProductOptionResponse API 데이터 모델")
data class OperationProductOptionResponse(
    @field:Schema(description = "리소스 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true) val id: UUID,
    @field:Schema(description = "이름 또는 표시 이름", example = "홍길동", type = "string", required = true) val name: String,
    @field:Schema(description = "화면 표시 순서", example = "1", format = "int32", type = "integer", required = true, implementation = Int::class) val displayOrder: Int,
    @field:ArraySchema(
        schema = Schema(implementation = OperationProductOptionValueResponse::class),
    ) @field:Schema(description = "상품 옵션 값 목록", example = "[]", type = "array", required = true) val values: List<OperationProductOptionValueResponse>,
)

@Schema(description = "OperationProductResponse API 데이터 모델")
data class OperationProductResponse(
    @field:Schema(description = "리소스 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true) val id: UUID,
    @field:Schema(description = "카테고리 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true) val categoryId: UUID,
    @field:Schema(description = "브랜드 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true) val brandId: UUID?,
    @field:Schema(description = "이름 또는 표시 이름", example = "홍길동", type = "string", required = true) val name: String,
    @field:Schema(description = "상세 설명", example = "예시 값", type = "string", required = true) val description: String?,
    @field:Schema(description = "화면 노출 상태 코드", example = "예시 값", type = "string", required = true) val displayStatus: String,
    @field:Schema(description = "판매 상태 코드", example = "예시 값", type = "string", required = true) val salesStatus: String,
    @field:Schema(description = "화면 표시 순서", example = "1", format = "int32", type = "integer", required = true, implementation = Int::class) val displayOrder: Int,
    @field:ArraySchema(
        schema = Schema(implementation = OperationProductImageResponse::class),
    ) @field:Schema(description = "상품 이미지 목록", example = "[]", type = "array", required = true) val images: List<OperationProductImageResponse> = emptyList(),
    @field:ArraySchema(
        schema = Schema(implementation = OperationProductOptionResponse::class),
    ) @field:Schema(description = "상품 옵션 목록", example = "[]", type = "array", required = true) val options: List<OperationProductOptionResponse> = emptyList(),
    @field:ArraySchema(
        schema = Schema(implementation = OperationProductSkuResponse::class),
    ) @field:Schema(description = "상품 SKU 목록", example = "[]", type = "array", required = true) val skus: List<OperationProductSkuResponse> = emptyList(),
)

@Schema(description = "OperationProductPageResponse API 데이터 모델")
data class OperationProductPageResponse(
    @field:ArraySchema(
        schema = Schema(implementation = OperationProductResponse::class),
    ) @field:Schema(description = "현재 페이지의 항목 목록", example = "[]", type = "array", required = true) val items: List<OperationProductResponse>,
    @field:Schema(description = "페이지 번호(0부터 시작)", example = "1", format = "int32", type = "integer", required = true, implementation = Int::class) val page: Int,
    @field:Schema(description = "페이지당 항목 수", example = "1", format = "int32", type = "integer", required = true, implementation = Int::class) val size: Int,
    @field:Schema(description = "전체 검색 결과 수", example = "1", format = "int64", type = "integer", required = true, implementation = Long::class) val totalElements: Long,
    @field:Schema(description = "전체 페이지 수", example = "1", format = "int32", type = "integer", required = true, implementation = Int::class) val totalPages: Int,
)

@Schema(description = "채널별 실물 SKU 판매 오퍼")
data class OperationSalesOfferResponse(
    @field:Schema(description = "오퍼 공개 UUID", format = "uuid", type = "string", required = true) val id: UUID,
    @field:Schema(description = "판매 채널 코드", example = "WHOLESALE", type = "string", required = true) val channelCode: String,
    @field:Schema(description = "공용 실물 SKU UUID", format = "uuid", type = "string", required = true) val skuId: UUID,
    @field:Schema(description = "채널 판매 단위 1개의 가격(원)", example = "12000", type = "integer", required = true) val salePrice: Long,
    @field:Schema(description = "채널 정가(원)", example = "1200", type = "integer", required = false) val listPrice: Long?,
    @field:Schema(description = "판매 상태", example = "ON_SALE", type = "string", required = true) val salesStatus: String,
    @field:Schema(description = "판매 단위당 기준 SKU 입수 수량", example = "12", type = "integer", required = true, implementation = Int::class) val unitsPerSale: Int = 1,
)
