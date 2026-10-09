package com.buyeong.umji.api.account.model

import com.buyeong.umji.api.account.dto.OrganizationInvitationDto
import com.buyeong.umji.api.account.dto.OrganizationSearchResultDto
import com.buyeong.umji.api.account.dto.OrganizationSummaryDto
import com.buyeong.umji.api.account.model.OrganizationJoinRequest
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Organization 가입 화면에 필요한 현재 그룹과 초대 목록")
data class OrganizationOnboardingResponse(
    @field:Schema(
        description = "현재 가입된 Organization 정보. 미가입이면 null",
        example = "{\"id\":\"00000000-0000-0000-0000-000000000001\",\"type\":\"BUSINESS\",\"name\":\"엄지상회\",\"representative\":true}",
        type = "object",
        required = true,
        implementation = OrganizationResponse::class,
    )
    val currentOrganization: OrganizationResponse?,

    @field:ArraySchema(
        schema = Schema(implementation = OrganizationInvitationResponse::class),
    ) @field:Schema(description = "응답 대기 중인 Organization 초대 목록", example = "[]", type = "array", required = true)
    val invitations: List<OrganizationInvitationResponse>,
)

fun OrganizationSummaryDto.toResponse() = OrganizationResponse(id, type, name, representative, capabilities)
fun OrganizationSearchResultDto.toResponse() = SearchOrganizationResponse(id, type, name, capabilities)
fun OrganizationInvitationDto.toResponse() = OrganizationInvitationResponse(id, organizationId, organizationName, invitedPhone)
fun OrganizationJoinRequest.toResponse() = OrganizationJoinRequestResponse(id, organizationId, organizationName, requesterName, requesterPhone, requestedAt)