package com.buyeong.umji.api.persistence.jpa.account

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface BuyerGroupRepository : JpaRepository<BuyerGroupEntity, Long> {
    fun findByPublicId(publicId: UUID): BuyerGroupEntity?

    @Query(
        value = """
            select distinct buyer_group.*
            from buyer_group
            left join buyer_group_member on buyer_group_member.buyer_group_id = buyer_group.id and buyer_group_member.status = 'ACTIVE'
            left join account on account.id = buyer_group_member.account_id
            left join buyer_group_business_profile on buyer_group_business_profile.buyer_group_id = buyer_group.id
            where buyer_group.status = 'ACTIVE'
              and (
                account.phone_normalized = :phone
                or case
                    when regexp_replace(coalesce(buyer_group_business_profile.business_phone, ''), '[^0-9]', '') like '82%'
                    then concat('0', substring(regexp_replace(coalesce(buyer_group_business_profile.business_phone, ''), '[^0-9]', ''), 3))
                    else regexp_replace(coalesce(buyer_group_business_profile.business_phone, ''), '[^0-9]', '')
                end = :phone
              )
            order by buyer_group.display_name
        """,
        nativeQuery = true,
    )
    fun searchByPhone(@Param("phone") phone: String): List<BuyerGroupEntity>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select buyerGroup from BuyerGroupEntity buyerGroup where buyerGroup.id = :id")
    fun findLockedById(@Param("id") id: Long): BuyerGroupEntity?
}

interface BuyerGroupMemberRepository : JpaRepository<BuyerGroupMemberEntity, Long> {
    fun findFirstByAccount_IdAndStatus(accountId: Long, status: String): BuyerGroupMemberEntity?
    fun findFirstByAccount_IdAndStatusOrderByJoinedAtDesc(accountId: Long, status: String): BuyerGroupMemberEntity?
    fun findFirstByBuyerGroup_IdAndAccount_Id(buyerGroupId: Long, accountId: Long): BuyerGroupMemberEntity?
    fun findAllByBuyerGroup_IdAndStatus(buyerGroupId: Long, status: String): List<BuyerGroupMemberEntity>
}

interface BuyerGroupInvitationRepository : JpaRepository<BuyerGroupInvitationEntity, Long> {
    fun findFirstByPublicIdAndStatus(publicId: UUID, status: String): BuyerGroupInvitationEntity?
    fun findAllByPhoneNormalizedAndStatus(phoneNormalized: String, status: String): List<BuyerGroupInvitationEntity>
    fun findFirstByBuyerGroup_IdAndPhoneNormalizedAndStatus(buyerGroupId: Long, phoneNormalized: String, status: String): BuyerGroupInvitationEntity?
}

interface BuyerGroupJoinRequestRepository : JpaRepository<BuyerGroupJoinRequestEntity, Long> {
    fun findFirstByPublicIdAndStatus(publicId: UUID, status: String): BuyerGroupJoinRequestEntity?
    fun findAllByBuyerGroup_IdAndStatusOrderByRequestedAtAsc(buyerGroupId: Long, status: String): List<BuyerGroupJoinRequestEntity>
    fun findFirstByBuyerGroup_IdAndAccount_IdAndStatus(buyerGroupId: Long, accountId: Long, status: String): BuyerGroupJoinRequestEntity?
}

interface BuyerGroupBusinessProfileRepository : JpaRepository<BuyerGroupBusinessProfileEntity, Long> {
    fun findByBuyerGroup_Id(buyerGroupId: Long): BuyerGroupBusinessProfileEntity?
}

interface BuyerGroupAddressRepository : JpaRepository<BuyerGroupAddressEntity, Long> {
    @Query(
        "select address from BuyerGroupAddressEntity address where address.buyerGroup.id = :buyerGroupId order by address.isDefault desc, address.id asc",
    )
    fun findAllByBuyerGroupId(@Param("buyerGroupId") buyerGroupId: Long): List<BuyerGroupAddressEntity>

    fun findByPublicIdAndBuyerGroup_Id(publicId: UUID, buyerGroupId: Long): BuyerGroupAddressEntity?
}
