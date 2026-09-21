package com.buyeong.umji.api.auth.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import jakarta.persistence.LockModeType

interface RefreshTokenRepository : JpaRepository<RefreshTokenEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from RefreshTokenEntity token join fetch token.account where token.tokenHash = :tokenHash")
    fun findLockedByTokenHash(@Param("tokenHash") tokenHash: ByteArray): RefreshTokenEntity?
}
