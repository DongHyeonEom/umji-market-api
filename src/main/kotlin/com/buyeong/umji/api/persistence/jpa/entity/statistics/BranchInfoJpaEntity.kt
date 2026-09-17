package com.buyeong.umji.api.persistence.jpa.entity.statistics

import com.buyeong.umji.api.enums.SchoolTypeEnum
import com.buyeong.umji.api.persistence.jpa.entity.backbone.BaseCreateAuditedEntity
import com.buyeong.umji.api.persistence.jpa.entity.converter.SchoolTypeConverter
import jakarta.persistence.AttributeOverride
import jakarta.persistence.AttributeOverrides
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "stat_branch_info")
@AttributeOverrides(
    AttributeOverride(name = "id", column = Column(name = "seq")),
    AttributeOverride(name = "createdAt", column = Column(name = "createdate")),
)
class BranchInfoJpaEntity(
    @Column(name = "crs_createdate", nullable = false)
    val crsCreatedAt: LocalDateTime,
    @Convert(converter = SchoolTypeConverter::class)
    @Column(name = "brch_schl_type", nullable = false, length = 20)
    val schoolType: SchoolTypeEnum,
    @Column(name = "brch_sub_type", nullable = false, length = 20)
    val subType: String,
    @Column(name = "brch_id", nullable = false)
    val branchId: Int,
    @Column(name = "brch_name", nullable = false, length = 50)
    val name: String,
    @Column(name = "brch_zipcode", length = 50)
    val zipcode: String?,
    @Column(name = "brch_address", length = 1000)
    val address: String?,
    @Column(name = "brch_iscdi", nullable = false)
    val isCdi: Boolean,
    @Column(name = "brch_isactive", nullable = false)
    val isActive: Boolean,
    @Column(name = "intg_type", length = 20)
    val integrationType: String?,
    @Column(name = "intg_brand", length = 200)
    val integrationBrand: String?,
    @Column(name = "intg_branch", length = 1000)
    val integrationBranch: String?,
    @Column(name = "intg_brch_id", length = 200)
    val integrationBranchId: String?,
    @Column(name = "adr_si", nullable = false, length = 100)
    val state: String,
    @Column(name = "adr_gu", nullable = false, length = 100)
    val city: String,
    @Column(name = "adr_dong", nullable = false, length = 100)
    val town: String,
    @Column(name = "own1_stf_id")
    val ownerStaffId1: Int?,
    @Column(name = "own2_stf_id")
    val ownerStaffId2: Int?,
    @Column(name = "own1_stf_name", length = 100)
    val ownerStaffName1: String?,
    @Column(name = "own2_stf_name", length = 100)
    val ownerStaffName2: String?,
) : BaseCreateAuditedEntity()