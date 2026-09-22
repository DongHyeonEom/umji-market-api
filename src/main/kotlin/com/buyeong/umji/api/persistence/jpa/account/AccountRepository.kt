package com.buyeong.umji.api.persistence.jpa.account

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AccountRepository : JpaRepository<AccountEntity, Long> {
    fun findByPublicId(publicId: UUID): AccountEntity?
    fun findByPhoneNormalized(phoneNormalized: String): AccountEntity?
    fun findAllByStatus(status: String, pageable: Pageable): Page<AccountEntity>
}
