package com.buyeong.umji.api.account.model

import com.buyeong.umji.api.account.model.OrganizationInvitation
import com.buyeong.umji.api.account.model.OrganizationJoinRequest
import com.buyeong.umji.api.account.model.OrganizationSearchResult
import com.buyeong.umji.api.account.model.OrganizationSummary
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.Instant
import java.util.UUID

@Schema(description = "개인 Organization 생성 요청")
data class CreateIndividualGroupRequest(
    @field:NotBlank @field:Schema(description = "개인 Organization 표시 이름", example = "홍길동", type = "string", required = true) val name: String,
)

@Schema(description = "Organization 검색 결과")
data class SearchOrganizationResponse(
    @field:Schema(description = "Organization 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true) val id: UUID,
    @field:Schema(description = "Organization 유형 코드", example = "BUSINESS", type = "string", required = true) val type: String,
    @field:Schema(description = "Organization 표시 이름", example = "엄지상회", type = "string", required = true) val name: String,
    @field:Schema(description = "Organization capability 목록", example = "[\"BUYER\"]", required = true) val capabilities: Set<String>,
)

@Schema(description = "현재 계정의 Organization 정보")
data class OrganizationResponse(
    @field:Schema(description = "Organization 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true) val id: UUID,
    @field:Schema(description = "Organization 유형 코드", example = "BUSINESS", type = "string", required = true) val type: String,
    @field:Schema(description = "Organization 표시 이름", example = "엄지상회", type = "string", required = true) val name: String,
    @field:Schema(description = "현재 계정의 그룹 대표자 여부", example = "true", type = "boolean", required = true, implementation = Boolean::class) val representative: Boolean,
    @field:Schema(description = "Organization capability 목록", example = "[\"BUYER\"]", required = true) val capabilities: Set<String>,
)

@Schema(description = "사용자에게 전달된 Organization 초대 정보")
data class OrganizationInvitationResponse(
    @field:Schema(description = "초대 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true) val id: UUID,
    @field:Schema(description = "Organization 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true) val organizationId: UUID,
    @field:Schema(description = "Organization 표시 이름", example = "엄지상회", type = "string", required = true) val organizationName: String,
    @field:Schema(description = "초대 대상 휴대폰 번호", example = "01012345678", type = "string", required = true) val invitedPhone: String,
)

@Schema(description = "Organization 가입 요청 항목")
data class OrganizationJoinRequestResponse(
    @field:Schema(description = "가입 요청 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true) val id: UUID,
    @field:Schema(description = "Organization 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true) val organizationId: UUID,
    @field:Schema(description = "Organization 표시 이름", example = "엄지상회", type = "string", required = true) val organizationName: String,
    @field:Schema(description = "가입을 요청한 사용자 이름", example = "홍길동", type = "string", required = true) val requesterName: String,
    @field:Schema(description = "가입을 요청한 사용자 휴대폰 번호", example = "01012345678", type = "string", required = true) val requesterPhone: String,
    @field:Schema(description = "가입 요청 시각(ISO-8601)", example = "2026-10-04T09:00:00Z", format = "date-time", type = "string", required = true) val requestedAt: Instant,
)

@Schema(description = "Organization 구성원 초대 요청")
data class InviteOrganizationMemberRequest(
    @field:NotBlank @field:Schema(description = "초대할 사용자의 휴대폰 번호", example = "01012345678", type = "string", required = true) val phone: String,
)

@Schema(description = "Organization 가입 요청")
data class RequestOrganizationJoinRequest(
    @field:NotNull @field:Schema(
        description = "가입을 요청할 Organization 공개 식별자(UUID)",
        example = "00000000-0000-0000-0000-000000000001",
        format = "uuid",
        type = "string",
        required = true,
    ) val organizationId: UUID,
)

@Schema(description = "Organization 가입 요청 처리 결과")
data class DecideOrganizationJoinRequest(
    @field:NotNull @field:Schema(description = "가입 요청 승인 여부", example = "true", type = "boolean", required = true, implementation = Boolean::class) val approved: Boolean,
)

@Schema(description = "Organization 대표자 지정 요청")
data class SetOrganizationRepresentativeRequest(
    @field:NotNull @field:Schema(
        description = "대표자로 지정할 계정 공개 식별자(UUID)",
        example = "00000000-0000-0000-0000-000000000001",
        format = "uuid",
        type = "string",
        required = true,
    ) val accountId: UUID,
)

@Schema(description = "Organization 가입 화면에 필요한 현재 그룹과 초대 목록")
data class OrganizationOnboardingResponse(
    @field:Schema(
        description = "현재 가입된 Organization 정보. 미가입이면 null",
        example = "{\"id\":\"00000000-0000-0000-0000-000000000001\",\"type\":\"BUSINESS\",\"name\":\"엄지상회\",\"representative\":true}",
        type = "object",
        required = true,
        implementation = OrganizationResponse::class,
    ) val currentOrganization: OrganizationResponse?,
    @field:ArraySchema(
        schema = Schema(implementation = OrganizationInvitationResponse::class),
    ) @field:Schema(description = "응답 대기 중인 Organization 초대 목록", example = "[]", type = "array", required = true) val invitations: List<OrganizationInvitationResponse>,
)

fun OrganizationSummary.toResponse() = OrganizationResponse(id, type, name, representative, capabilities)
fun OrganizationSearchResult.toResponse() = SearchOrganizationResponse(id, type, name, capabilities)
fun OrganizationInvitation.toResponse() = OrganizationInvitationResponse(id, organizationId, organizationName, invitedPhone)
fun OrganizationJoinRequest.toResponse() = OrganizationJoinRequestResponse(id, organizationId, organizationName, requesterName, requesterPhone, requestedAt)
