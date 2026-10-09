package com.buyeong.umji.api.auth.dto

import java.util.UUID

data class AccountRecordDto(
    val id: UUID,
    val name: String,
    val status: String,
    val tokenVersion: Long,
    val permissions: Set<String> = emptySet(),
    val roles: Set<String> = emptySet(),
    val mfaVerified: Boolean = false,
)