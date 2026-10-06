package com.buyeong.umji.api.operation.model

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.util.UUID

@Schema(description = "CreateCategoryRequest API 데이터 모델")
data class CreateCategoryRequest(
    @field:NotBlank @field:Size(max = 100) @field:Schema(description = "이름 또는 표시 이름", example = "홍길동", type = "string", required = true) val name: String,
    @field:Schema(description = "Parent Id 정보", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = false) val parentId: UUID? = null,
    @field:Min(
        0,
    ) @field:Schema(description = "화면 표시 순서", example = "1", format = "int32", type = "integer", required = false, implementation = Int::class) val displayOrder: Int = 0,
    @field:NotBlank @field:Schema(description = "화면 노출 상태 코드", example = "예시 값", type = "string", required = false) val displayStatus: String = "HIDDEN",
)

@Schema(description = "CreateBrandRequest API 데이터 모델")
data class CreateBrandRequest(
    @field:NotBlank @field:Size(max = 100) @field:Schema(description = "이름 또는 표시 이름", example = "홍길동", type = "string", required = true) val name: String,
    @field:NotBlank @field:Schema(description = "화면 노출 상태 코드", example = "예시 값", type = "string", required = false) val displayStatus: String = "HIDDEN",
)

@Schema(description = "CreateProductSkuRequest API 데이터 모델")
data class CreateProductSkuRequest(
    @field:NotBlank @field:Size(max = 64) @field:Schema(description = "상품 옵션 코드", example = "예시 값", type = "string", required = true) val skuCode: String,
    @field:NotBlank @field:Size(max = 200) @field:Schema(description = "이름 또는 표시 이름", example = "홍길동", type = "string", required = true) val name: String,
    @field:Min(0) @field:Schema(description = "판매 가격(원)", example = "1", format = "int64", type = "integer", required = true, implementation = Long::class) val salePrice: Long,
    @field:Min(
        0,
    ) @field:Schema(description = "정가(원)", example = "1", format = "int64", type = "integer", required = false, implementation = Long::class) val listPrice: Long? = null,
    @field:NotBlank @field:Schema(description = "판매 상태 코드", example = "예시 값", type = "string", required = false) val salesStatus: String = "ON_SALE",
    @field:ArraySchema(
        schema = Schema(implementation = UUID::class),
    ) @field:Schema(description = "SKU에 연결할 상품 옵션 값 공개 식별자(UUID) 목록", example = "[]", type = "array", required = false) val optionValueIds: Set<UUID> = emptySet(),
)

@Schema(description = "CreateProductImageRequest API 데이터 모델")
data class CreateProductImageRequest(
    @field:NotBlank @field:Size(max = 500) @field:Schema(description = "상품 이미지 저장소 키", example = "예시 값", type = "string", required = true) val storageKey: String,
    @field:Size(max = 200) @field:Schema(description = "상품 이미지 대체 텍스트", example = "예시 값", type = "string", required = false) val altText: String? = null,
    @field:Min(
        0,
    ) @field:Schema(description = "화면 표시 순서", example = "1", format = "int32", type = "integer", required = false, implementation = Int::class) val displayOrder: Int = 0,
)

@Schema(description = "CreateProductOptionValueRequest API 데이터 모델")
data class CreateProductOptionValueRequest(
    @field:NotBlank @field:Size(max = 100) @field:Schema(description = "값", example = "예시 값", type = "string", required = true) val value: String,
    @field:Min(
        0,
    ) @field:Schema(description = "화면 표시 순서", example = "1", format = "int32", type = "integer", required = false, implementation = Int::class) val displayOrder: Int = 0,
)

@Schema(description = "CreateProductOptionRequest API 데이터 모델")
data class CreateProductOptionRequest(
    @field:NotBlank @field:Size(max = 100) @field:Schema(description = "이름 또는 표시 이름", example = "홍길동", type = "string", required = true) val name: String,
    @field:Min(
        0,
    ) @field:Schema(description = "화면 표시 순서", example = "1", format = "int32", type = "integer", required = false, implementation = Int::class) val displayOrder: Int = 0,
    @field:NotEmpty @field:ArraySchema(
        schema = Schema(implementation = CreateProductOptionValueRequest::class),
    ) @field:Schema(description = "상품 옵션 값 목록", example = "[]", type = "array", required = true) val values: List<@Valid CreateProductOptionValueRequest>,
)

@Schema(description = "CreateProductRequest API 데이터 모델")
data class CreateProductRequest(
    @field:NotNull @field:Schema(
        description = "카테고리 공개 식별자(UUID)",
        example = "00000000-0000-0000-0000-000000000001",
        format = "uuid",
        type = "string",
        required = true,
    ) val categoryId: UUID,
    @field:Schema(description = "브랜드 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = false) val brandId: UUID? = null,
    @field:NotBlank @field:Size(max = 200) @field:Schema(description = "이름 또는 표시 이름", example = "홍길동", type = "string", required = true) val name: String,
    @field:Schema(description = "상세 설명", example = "예시 값", type = "string", required = false) val description: String? = null,
    @field:NotBlank @field:Schema(description = "화면 노출 상태 코드", example = "예시 값", type = "string", required = false) val displayStatus: String = "HIDDEN",
    @field:NotBlank @field:Schema(description = "판매 상태 코드", example = "예시 값", type = "string", required = false) val salesStatus: String = "ON_SALE",
    @field:Min(
        0,
    ) @field:Schema(description = "화면 표시 순서", example = "1", format = "int32", type = "integer", required = false, implementation = Int::class) val displayOrder: Int = 0,
    @field:ArraySchema(
        schema = Schema(implementation = CreateProductImageRequest::class),
    ) @field:Schema(description = "상품 이미지 목록", example = "[]", type = "array", required = false) val images: List<@Valid CreateProductImageRequest> = emptyList(),
    @field:ArraySchema(
        schema = Schema(implementation = CreateProductOptionRequest::class),
    ) @field:Schema(description = "상품 옵션 목록", example = "[]", type = "array", required = false) val options: List<@Valid CreateProductOptionRequest> = emptyList(),
    @field:NotEmpty @field:ArraySchema(
        schema = Schema(implementation = CreateProductSkuRequest::class),
    ) @field:Schema(description = "상품 SKU 목록", example = "[]", type = "array", required = true) val skus: List<@Valid CreateProductSkuRequest>,
)

@Schema(description = "UpdateProductRequest API 데이터 모델")
data class UpdateProductRequest(
    @field:NotNull @field:Schema(
        description = "카테고리 공개 식별자(UUID)",
        example = "00000000-0000-0000-0000-000000000001",
        format = "uuid",
        type = "string",
        required = true,
    ) val categoryId: UUID,
    @field:Schema(description = "브랜드 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = false) val brandId: UUID? = null,
    @field:NotBlank @field:Size(max = 200) @field:Schema(description = "이름 또는 표시 이름", example = "홍길동", type = "string", required = true) val name: String,
    @field:Schema(description = "상세 설명", example = "예시 값", type = "string", required = false) val description: String? = null,
    @field:NotBlank @field:Schema(description = "화면 노출 상태 코드", example = "예시 값", type = "string", required = true) val displayStatus: String,
    @field:NotBlank @field:Schema(description = "판매 상태 코드", example = "예시 값", type = "string", required = true) val salesStatus: String,
    @field:Min(
        0,
    ) @field:Schema(description = "화면 표시 순서", example = "1", format = "int32", type = "integer", required = false, implementation = Int::class) val displayOrder: Int = 0,
)

@Schema(description = "UpdateProductStatusRequest API 데이터 모델")
data class UpdateProductStatusRequest(
    @field:NotBlank @field:Schema(description = "화면 노출 상태 코드", example = "예시 값", type = "string", required = true) val displayStatus: String,
    @field:NotBlank @field:Schema(description = "판매 상태 코드", example = "예시 값", type = "string", required = true) val salesStatus: String,
)

@Schema(description = "판매 채널 전용 카테고리 등록 요청")
data class CreateChannelCategoryRequest(
    @field:NotBlank @field:Size(max = 100) @field:Schema(description = "채널 카테고리명", example = "공구", type = "string", required = true) val name: String,
    @field:Schema(description = "같은 채널의 상위 카테고리 UUID", format = "uuid", type = "string", required = false) val parentId: UUID? = null,
    @field:Min(0) @field:Schema(description = "채널 내 표시 순서", example = "0", type = "integer", required = false) val displayOrder: Int = 0,
    @field:NotBlank @field:Schema(description = "노출 상태: DISPLAYED 또는 HIDDEN", example = "HIDDEN", type = "string", required = true) val displayStatus: String = "HIDDEN",
)

@Schema(description = "판매 채널 상품 listing 수정 요청")
data class UpdateChannelListingRequest(
    @field:NotNull @field:Schema(description = "대상 채널 카테고리 UUID", format = "uuid", type = "string", required = true) val categoryId: UUID,
    @field:NotBlank @field:Schema(description = "노출 상태: DISPLAYED 또는 HIDDEN", example = "DISPLAYED", type = "string", required = true) val displayStatus: String,
    @field:Min(0) @field:Schema(description = "채널 내 상품 표시 순서", example = "0", type = "integer", required = true) val displayOrder: Int,
)

@Schema(description = "채널별 SKU 판매 오퍼 수정 요청")
data class UpdateSalesOfferRequest(
    @field:Min(0) @field:Schema(description = "채널 판매 단위 1개의 가격(원)", example = "12000", type = "integer", required = true) val salePrice: Long,
    @field:Min(0) @field:Schema(description = "채널 정가(원)", example = "1200", type = "integer", required = false) val listPrice: Long? = null,
    @field:NotBlank @field:Schema(description = "판매 상태: ON_SALE 또는 STOPPED", example = "ON_SALE", type = "string", required = true) val salesStatus: String,
    @field:Min(
        1,
    ) @field:Schema(
        description = "판매 단위당 기준 SKU 입수 수량. WHOLESALE은 박스 입수 수량, RETAIL은 1",
        example = "12",
        type = "integer",
        required = false,
        implementation = Int::class,
    ) val unitsPerSale: Int? = null,
)
