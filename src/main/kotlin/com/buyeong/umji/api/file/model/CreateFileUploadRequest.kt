package com.buyeong.umji.api.file.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.util.UUID

@Schema(description = "파일 업로드 URL 발급 요청")
data class CreateFileUploadRequest(
    @field:NotBlank
    @field:Schema(description = "파일 분류. PRODUCT_IMAGE 또는 BUSINESS_EVIDENCE", example = "PRODUCT_IMAGE", type = "string", required = true)
    val fileType: String,

    @field:Schema(description = "사업자 증빙 파일 소유 계정 공개 UUID. 공개 상품 이미지는 null", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = false)
    val ownerAccountId: UUID?,

    @field:NotBlank
    @field:Schema(description = "원본 파일명. 경로 구성요소는 서버에서 제거", example = "business-license.pdf", type = "string", required = true)
    val fileName: String,

    @field:NotBlank
    @field:Schema(description = "허용 MIME 형식", example = "image/webp", type = "string", required = true)
    val contentType: String,

    @field:NotNull
    @field:Positive
    @field:Schema(description = "업로드할 원본 크기(byte). 상품 이미지 최대 10 MiB, 사업자 증빙 최대 20 MiB", example = "524288", format = "int64", type = "integer", required = true)
    val byteSize: Long,
)