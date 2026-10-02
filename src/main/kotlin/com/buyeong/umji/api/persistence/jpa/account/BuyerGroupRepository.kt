package com.buyeong.umji.api.persistence.jpa.account

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface BuyerGroupRepository : JpaRepository<BuyerGroupEntity, Long> {
    fun findByPublicId(publicId: UUID): BuyerGroupEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select buyerGroup from BuyerGroupEntity buyerGroup where buyerGroup.id = :id")
    fun findLockedById(@Param("id") id: Long): BuyerGroupEntity?
}

interface BuyerGroupMemberRepository : JpaRepository<BuyerGroupMemberEntity, Long> {
    fun findFirstByAccount_Id(accountId: Long): BuyerGroupMemberEntity?
    fun findFirstByAccount_IdAndStatus(accountId: Long, status: String): BuyerGroupMemberEntity?
}

interface BuyerGroupBusinessProfileRepository : JpaRepository<BuyerGroupBusinessProfileEntity, Long>

interface BuyerGroupAddressRepository : JpaRepository<BuyerGroupAddressEntity, Long> {
    @Query(
        "select address from BuyerGroupAddressEntity address where address.buyerGroup.id = :buyerGroupId order by address.isDefault desc, address.id asc",
    )
    fun findAllByBuyerGroupId(@Param("buyerGroupId") buyerGroupId: Long): List<BuyerGroupAddressEntity>

    fun findByPublicIdAndBuyerGroup_Id(publicId: UUID, buyerGroupId: Long): BuyerGroupAddressEntity?
}