package com.buyeong.umji.api.persistence.jpa.account.repository

import com.buyeong.umji.api.persistence.jpa.account.entity.AccountEntity
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface AccountRepository : JpaRepository<AccountEntity, Long> {
    fun findByPublicId(publicId: UUID): AccountEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select account from AccountEntity account where account.publicId = :publicId")
    fun findLockedByPublicId(@Param("publicId") publicId: UUID): AccountEntity?
    fun findByPhoneNormalized(phoneNormalized: String): AccountEntity?
    fun findAllByStatus(status: String, pageable: Pageable): Page<AccountEntity>
}