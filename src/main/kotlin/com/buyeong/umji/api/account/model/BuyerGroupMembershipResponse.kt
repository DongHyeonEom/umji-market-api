package com.buyeong.umji.api.account.model

import com.buyeong.umji.api.account.application.model.BuyerGroupInvitation
import com.buyeong.umji.api.account.application.model.BuyerGroupJoinRequest
import com.buyeong.umji.api.account.application.model.BuyerGroupSearchResult
import com.buyeong.umji.api.account.application.model.BuyerGroupSummary
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.Instant
import java.util.UUID

@Schema(description = "개인 구매자 그룹 생성 요청")
data class CreateIndividualGroupRequest(
    @field:NotBlank @field:Schema(description = "개인 구매자 그룹 표시 이름", example = "홍길동", type = "string", required = true) val name: String,
)

@Schema(description = "구매자 그룹 검색 결과")
data class SearchBuyerGroupResponse(
    @field:Schema(description = "그룹 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true) val id: UUID,
    @field:Schema(description = "구매자 그룹 유형 코드", example = "BUSINESS", type = "string", required = true) val type: String,
    @field:Schema(description = "구매자 그룹 표시 이름", example = "엄지상회", type = "string", required = true) val name: String,
)

@Schema(description = "현재 계정의 구매자 그룹 정보")
data class BuyerGroupResponse(
    @field:Schema(description = "그룹 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true) val id: UUID,
    @field:Schema(description = "구매자 그룹 유형 코드", example = "BUSINESS", type = "string", required = true) val type: String,
    @field:Schema(description = "구매자 그룹 표시 이름", example = "엄지상회", type = "string", required = true) val name: String,
    @field:Schema(description = "현재 계정의 그룹 대표자 여부", example = "true", type = "boolean", required = true, implementation = Boolean::class) val representative: Boolean,
)

@Schema(description = "사용자에게 전달된 구매자 그룹 초대 정보")
data class BuyerGroupInvitationResponse(
    @field:Schema(description = "초대 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true) val id: UUID,
    @field:Schema(description = "그룹 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true) val groupId: UUID,
    @field:Schema(description = "구매자 그룹 표시 이름", example = "엄지상회", type = "string", required = true) val groupName: String,
    @field:Schema(description = "초대 대상 휴대폰 번호", example = "01012345678", type = "string", required = true) val invitedPhone: String,
)

@Schema(description = "구매자 그룹 가입 요청 항목")
data class BuyerGroupJoinRequestResponse(
    @field:Schema(description = "가입 요청 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true) val id: UUID,
    @field:Schema(description = "그룹 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true) val groupId: UUID,
    @field:Schema(description = "구매자 그룹 표시 이름", example = "엄지상회", type = "string", required = true) val groupName: String,
    @field:Schema(description = "가입을 요청한 사용자 이름", example = "홍길동", type = "string", required = true) val requesterName: String,
    @field:Schema(description = "가입을 요청한 사용자 휴대폰 번호", example = "01012345678", type = "string", required = true) val requesterPhone: String,
    @field:Schema(description = "가입 요청 시각(ISO-8601)", example = "2026-10-04T09:00:00Z", format = "date-time", type = "string", required = true) val requestedAt: Instant,
)

@Schema(description = "구매자 그룹 구성원 초대 요청")
data class InviteBuyerGroupMemberRequest(
    @field:NotBlank @field:Schema(description = "초대할 사용자의 휴대폰 번호", example = "01012345678", type = "string", required = true) val phone: String,
)

@Schema(description = "구매자 그룹 가입 요청")
data class RequestBuyerGroupJoinRequest(
    @field:NotNull @field:Schema(
        description = "가입을 요청할 그룹 공개 식별자(UUID)",
        example = "00000000-0000-0000-0000-000000000001",
        format = "uuid",
        type = "string",
        required = true,
    ) val groupId: UUID,
)

@Schema(description = "구매자 그룹 가입 요청 처리 결과")
data class DecideBuyerGroupJoinRequest(
    @field:NotNull @field:Schema(description = "가입 요청 승인 여부", example = "true", type = "boolean", required = true, implementation = Boolean::class) val approved: Boolean,
)

@Schema(description = "구매자 그룹 대표자 지정 요청")
data class SetBuyerGroupRepresentativeRequest(
    @field:NotNull @field:Schema(
        description = "대표자로 지정할 계정 공개 식별자(UUID)",
        example = "00000000-0000-0000-0000-000000000001",
        format = "uuid",
        type = "string",
        required = true,
    ) val accountId: UUID,
)

@Schema(description = "구매자 그룹 가입 화면에 필요한 현재 그룹과 초대 목록")
data class BuyerGroupOnboardingResponse(
    @field:Schema(
        description = "현재 가입된 구매자 그룹 정보. 미가입이면 null",
        example = "{\"id\":\"00000000-0000-0000-0000-000000000001\",\"type\":\"BUSINESS\",\"name\":\"엄지상회\",\"representative\":true}",
        type = "object",
        required = true,
        implementation = BuyerGroupResponse::class,
    ) val currentGroup: BuyerGroupResponse?,
    @field:ArraySchema(
        schema = Schema(implementation = BuyerGroupInvitationResponse::class),
    ) @field:Schema(description = "응답 대기 중인 구매자 그룹 초대 목록", example = "[]", type = "array", required = true) val invitations: List<BuyerGroupInvitationResponse>,
)

fun BuyerGroupSummary.toResponse() = BuyerGroupResponse(id, type, name, representative)
fun BuyerGroupSearchResult.toResponse() = SearchBuyerGroupResponse(id, type, name)
fun BuyerGroupInvitation.toResponse() = BuyerGroupInvitationResponse(id, groupId, groupName, invitedPhone)
fun BuyerGroupJoinRequest.toResponse() = BuyerGroupJoinRequestResponse(id, groupId, groupName, requesterName, requesterPhone, requestedAt)