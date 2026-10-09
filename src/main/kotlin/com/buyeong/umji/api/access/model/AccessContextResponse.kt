package com.buyeong.umji.api.access.model

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "현재 계정의 audience별 화면 접근 context")
data class AccessContextResponse(
    @field:Schema(description = "context audience", example = "BUYER", required = true)
    val audience: AccessAudience,

    @field:Schema(description = "현재 계정의 role 또는 활성 Organization 구성원 역할", example = "REPRESENTATIVE", required = true)
    val roles: List<String>,

    @field:Schema(description = "현재 역할에서 사용할 수 있는 permission code 목록", example = "[BUYER_GROUP_ORDER_READ, BUYER_GROUP_ORDER_CREATE]", required = true)
    val permissions: List<String>,

    @field:Schema(
        description = "현재 활성 BUYER Organization 공개 UUID. 미소속이면 null",
        example = "00000000-0000-0000-0000-000000000001",
        format = "uuid",
        type = "string",
        required = false,
        nullable = true
    )
    val organizationId: UUID?,

    @field:Schema(description = "현재 활성 구매자 구성원 역할. BUYER audience에서만 반환", example = "MEMBER", type = "string", required = false, nullable = true)
    val membershipRole: String?,

    @field:Schema(description = "permission mapping을 통과한 화면 code와 React route key 목록", example = "[]", type = "array", required = true)
    val screens: List<AccessScreenResponse>,
)