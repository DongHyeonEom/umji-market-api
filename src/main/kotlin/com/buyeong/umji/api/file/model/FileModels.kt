package com.buyeong.umji.api.file.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.time.Instant
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

@Schema(description = "파일 업로드 URL 및 metadata 응답")
data class FileUploadResponse(
    @field:Schema(description = "파일 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true)
    val fileId: UUID,
    @field:Schema(description = "파일 분류", example = "PRODUCT_IMAGE", type = "string", required = true)
    val fileType: String,
    @field:Schema(description = "상품 이미지 연결용 불투명 저장 key. 사업자 증빙은 null", example = "a2f7bbd2-2e96-4a79-9068-7c7b35c78548", type = "string", required = false)
    val storageKey: String?,
    @field:Schema(description = "정규화된 원본 파일명", example = "business-license.pdf", type = "string", required = true)
    val fileName: String,
    @field:Schema(description = "검증된 MIME 형식", example = "image/webp", type = "string", required = true)
    val contentType: String,
    @field:Schema(description = "원본 크기(byte)", example = "524288", format = "int64", type = "integer", required = true)
    val byteSize: Long,
    @field:Schema(description = "업로드 완료 여부", example = "false", type = "boolean", required = true)
    val uploaded: Boolean,
    @field:Schema(description = "업로드할 때 사용할 API URL", example = "/api/files/00000000-0000-0000-0000-000000000001/content", type = "string", required = false)
    val uploadUrl: String?,
    @field:Schema(description = "단회 업로드 token. 파일 content 요청 header X-Upload-Token에 전달", example = "token", type = "string", required = false)
    val uploadToken: String?,
    @field:Schema(description = "업로드 token 만료 시각", example = "2026-10-09T00:15:00Z", format = "date-time", type = "string", required = true)
    val uploadExpiresAt: Instant,
    @field:Schema(description = "공개 상품 이미지용 API URL. 사업자 증빙은 null", example = "/api/files/public/00000000-0000-0000-0000-000000000001", type = "string", required = false)
    val publicUrl: String?,
)
