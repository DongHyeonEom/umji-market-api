package com.buyeong.umji.api.auth.application.port.out

import com.buyeong.umji.api.auth.application.model.AccountRecord
import com.buyeong.umji.api.auth.application.model.RefreshSessionRecord
import java.time.Instant
import java.util.UUID

interface AccountAuthenticationPort {
    fun findByPublicId(id: UUID): AccountRecord?
    fun findByNormalizedPhone(phone: String): AccountRecord?
    fun recordLogin(accountId: UUID, at: Instant)
}

interface RefreshSessionPort {
    fun findLockedByHash(tokenHash: ByteArray): RefreshSessionRecord?
    fun save(session: RefreshSessionRecord)
}

interface AccessTokenIssuerPort {
    fun issue(account: AccountRecord, now: Instant, expiresAt: Instant): String
}
