package com.buyeong.umji.api.domain.file.model

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

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