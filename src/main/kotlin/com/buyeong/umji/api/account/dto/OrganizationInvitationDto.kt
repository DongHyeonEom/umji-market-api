package com.buyeong.umji.api.account.dto

import java.util.UUID

data class OrganizationInvitationDto(val id: UUID, val organizationId: UUID, val organizationName: String, val invitedPhone: String)