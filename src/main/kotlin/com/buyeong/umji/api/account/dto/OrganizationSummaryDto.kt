package com.buyeong.umji.api.account.dto

import java.util.UUID

data class OrganizationSummaryDto(
    val id: UUID,
    val type: String,
    val name: String,
    val representative: Boolean,
    val capabilities: Set<String> = setOf("BUYER"),
)