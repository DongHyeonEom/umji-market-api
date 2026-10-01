package com.buyeong.umji.api.persistence.jpa.account

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface BuyerGroupRepository : JpaRepository<BuyerGroupEntity, Long> {
    fun findByPublicId(publicId: UUID): BuyerGroupEntity?
}

interface BuyerGroupMemberRepository : JpaRepository<BuyerGroupMemberEntity, Long> {
    fun findFirstByAccount_Id(accountId: Long): BuyerGroupMemberEntity?
    fun findFirstByAccount_IdAndStatus(accountId: Long, status: String): BuyerGroupMemberEntity?
}

interface BuyerGroupBusinessProfileRepository : JpaRepository<BuyerGroupBusinessProfileEntity, Long>