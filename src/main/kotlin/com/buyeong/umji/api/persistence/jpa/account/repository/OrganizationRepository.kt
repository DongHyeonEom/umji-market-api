package com.buyeong.umji.api.persistence.jpa.account.repository

import com.buyeong.umji.api.persistence.jpa.account.entity.OrganizationAddressEntity
import com.buyeong.umji.api.persistence.jpa.account.entity.OrganizationBusinessProfileEntity
import com.buyeong.umji.api.persistence.jpa.account.entity.OrganizationCapabilityEntity
import com.buyeong.umji.api.persistence.jpa.account.entity.OrganizationEntity
import com.buyeong.umji.api.persistence.jpa.account.entity.OrganizationInvitationEntity
import com.buyeong.umji.api.persistence.jpa.account.entity.OrganizationJoinRequestEntity
import com.buyeong.umji.api.persistence.jpa.account.entity.OrganizationMemberEntity
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface OrganizationRepository : JpaRepository<OrganizationEntity, Long> {
    fun findByPublicId(publicId: UUID): OrganizationEntity?

    @Query(
        value = """
            select distinct organization.*
            from organization
            left join organization_member on organization_member.organization_id = organization.id and organization_member.status = 'ACTIVE'
            left join account on account.id = organization_member.account_id
            left join organization_business_profile on organization_business_profile.organization_id = organization.id
            where organization.status = 'ACTIVE'
              and (
                account.phone_normalized = :phone
                or case
                    when regexp_replace(coalesce(organization_business_profile.business_phone, ''), '[^0-9]', '') like '82%'
                    then concat('0', substring(regexp_replace(coalesce(organization_business_profile.business_phone, ''), '[^0-9]', ''), 3))
                    else regexp_replace(coalesce(organization_business_profile.business_phone, ''), '[^0-9]', '')
                end = :phone
              )
            order by organization.display_name
        """,
        nativeQuery = true,
    )
    fun searchByPhone(@Param("phone") phone: String): List<OrganizationEntity>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select organization from OrganizationEntity organization where organization.id = :id")
    fun findLockedById(@Param("id") id: Long): OrganizationEntity?
}

interface OrganizationMemberRepository : JpaRepository<OrganizationMemberEntity, Long> {
    fun findFirstByAccount_IdAndStatus(accountId: Long, status: String): OrganizationMemberEntity?
    fun findFirstByAccount_IdAndStatusOrderByJoinedAtDesc(accountId: Long, status: String): OrganizationMemberEntity?
    fun findFirstByOrganization_IdAndAccount_Id(organizationId: Long, accountId: Long): OrganizationMemberEntity?
    fun findAllByOrganization_IdAndStatus(organizationId: Long, status: String): List<OrganizationMemberEntity>
}

interface OrganizationInvitationRepository : JpaRepository<OrganizationInvitationEntity, Long> {
    fun findFirstByPublicIdAndStatus(publicId: UUID, status: String): OrganizationInvitationEntity?
    fun findAllByPhoneNormalizedAndStatus(phoneNormalized: String, status: String): List<OrganizationInvitationEntity>
    fun findFirstByOrganization_IdAndPhoneNormalizedAndStatus(organizationId: Long, phoneNormalized: String, status: String): OrganizationInvitationEntity?
}

interface OrganizationJoinRequestRepository : JpaRepository<OrganizationJoinRequestEntity, Long> {
    fun findFirstByPublicIdAndStatus(publicId: UUID, status: String): OrganizationJoinRequestEntity?
    fun findAllByOrganization_IdAndStatusOrderByRequestedAtAsc(organizationId: Long, status: String): List<OrganizationJoinRequestEntity>
    fun findFirstByOrganization_IdAndAccount_IdAndStatus(organizationId: Long, accountId: Long, status: String): OrganizationJoinRequestEntity?
}

interface OrganizationBusinessProfileRepository : JpaRepository<OrganizationBusinessProfileEntity, Long> {
    fun findByOrganization_Id(organizationId: Long): OrganizationBusinessProfileEntity?
    fun findTop100ByBusinessRegistrationVerificationStatusOrderByIdAsc(status: String): List<OrganizationBusinessProfileEntity>
}

interface OrganizationCapabilityRepository : JpaRepository<OrganizationCapabilityEntity, Long> {
    fun findAllByOrganization_Id(organizationId: Long): List<OrganizationCapabilityEntity>
    fun existsByOrganization_IdAndCapabilityCode(organizationId: Long, capabilityCode: String): Boolean
}

interface OrganizationAddressRepository : JpaRepository<OrganizationAddressEntity, Long> {
    @Query(
        "select address from OrganizationAddressEntity address where address.organization.id = :organizationId order by address.isDefault desc, address.id asc",
    )
    fun findAllByOrganizationId(@Param("organizationId") organizationId: Long): List<OrganizationAddressEntity>

    fun findByPublicIdAndOrganization_Id(publicId: UUID, organizationId: Long): OrganizationAddressEntity?
}