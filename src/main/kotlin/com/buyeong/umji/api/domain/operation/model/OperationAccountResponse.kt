package com.buyeong.umji.api.domain.operation.model

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "OperationAccountResponse API 데이터 모델")
data class OperationAccountResponse(
    @field:Schema(description = "리소스 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true)
    val id: UUID,

    @field:Schema(description = "이름 또는 표시 이름", example = "홍길동", type = "string", required = true)
    val name: String,

    @field:Schema(description = "휴대폰 번호", example = "01012345678", type = "string", required = true)
    val phone: String?,

    @field:Schema(description = "이메일 주소", example = "user@example.com", type = "string", required = true)
    val email: String?,

    @field:Schema(description = "현재 상태 코드", example = "ACTIVE", type = "string", required = true)
    val status: String,

    @field:Schema(description = "Token Version 정보", example = "1", format = "int64", type = "integer", required = true, implementation = Long::class)
    val tokenVersion: Long,

    @field:Schema(description = "Organization 공통 사업자 프로필", example = "예시 값", type = "object", required = true)
    val organizationProfile: OperationOrganizationProfileResponse?,

    @field:ArraySchema(
        schema = Schema(implementation = OperationConsentResponse::class),
    ) @field:Schema(description = "Consents 정보", example = "[]", type = "array", required = true)
    val consents: List<OperationConsentResponse> = emptyList(),

    @field:Schema(
        description = "Organization 공개 식별자(UUID)",
        example = "00000000-0000-0000-0000-000000000001",
        format = "uuid",
        type = "string",
        required = true,
    )
    val organizationId: UUID? =
        null,
    @field:Schema(description = "Organization capability 목록", example = "[\"SELLER\"]", required = true)
    val organizationCapabilities: Set<String> = emptySet(),
)