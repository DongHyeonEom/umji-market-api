package com.buyeong.umji.api.domain.operation.account.dto

import java.util.UUID

data class AccountDataDto(
    val id: UUID,
    val name: String,
    val phone: String?,
    val email: String?,
    val status: String,
    val tokenVersion: Long,
    val profile: OrganizationProfileDataDto? = null,
    val consents: List<ConsentDataDto> = emptyList(),
    val organizationId: UUID? = null,
    val organizationCapabilities: Set<String> = emptySet(),
)