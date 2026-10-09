package com.buyeong.umji.api.persistence.jpa.auth.service

import com.buyeong.umji.api.persistence.jpa.auth.entity.RefreshTokenEntity
import com.buyeong.umji.api.persistence.jpa.auth.repository.RefreshTokenRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class RefreshTokenJpaEntityService(
    private val refreshTokens: RefreshTokenRepository,
) {
    @Transactional fun save(token: RefreshTokenEntity): RefreshTokenEntity = refreshTokens.save(token)

    @Transactional fun findLockedByTokenHash(tokenHash: ByteArray): RefreshTokenEntity? = refreshTokens.findLockedByTokenHash(tokenHash)
}