package com.buyeong.umji.api.domain.auth.dto

import java.time.Instant

data class RefreshSessionRecordDto(
    val tokenHash: ByteArray,
    val account: AccountRecordDto,
    val deviceId: String?,
    val revokedAt: Instant?,
    val lastUsedAt: Instant?,
    val expiresAt: Instant?,
    val mfaVerified: Boolean = false,
)