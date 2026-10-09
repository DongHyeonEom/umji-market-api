package com.buyeong.umji.api.account.dto

import java.util.UUID

data class OrganizationSearchResultDto(val id: UUID, val type: String, val name: String, val capabilities: Set<String> = setOf("BUYER"))