package com.buyeong.umji.api.persistence.jpa.entity.cdi

import com.buyeong.umji.api.enums.StaffStatusEnum
import com.buyeong.umji.api.persistence.jpa.entity.backbone.BaseAuditedEntity
import com.buyeong.umji.api.persistence.jpa.entity.converter.StaffStatusConverter
import jakarta.persistence.AttributeOverride
import jakarta.persistence.AttributeOverrides
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.annotations.ColumnDefault
import java.time.LocalDateTime

@Entity
@AttributeOverrides(
    AttributeOverride(name = "id", column = Column(name = "stf_id")),
    AttributeOverride(name = "createdBy", column = Column(name = "stf_createby")),
    AttributeOverride(name = "createdAt", column = Column(name = "stf_createdate")),
    AttributeOverride(name = "updatedBy", column = Column(name = "stf_lastupdateby")),
    AttributeOverride(name = "updatedAt", column = Column(name = "stf_lastupdate")),
)
@Table(name = "cdi_staff")
class StaffJpaEntity(
    @Convert(converter = StaffStatusConverter::class)
    @Column(name = "stf_status", nullable = false, length = 50)
    var status: StaffStatusEnum,
    @Column(name = "stf_recruit_status", length = 50)
    var recruitStatus: String?,
    @Column(name = "stf_lastname", nullable = false, length = 50)
    var lastname: String,
    @Column(name = "stf_firstname", length = 50)
    var firstname: String?,
    @Column(name = "stf_middlename", length = 50)
    var middlename: String?,
    @Column(name = "stf_nickname", length = 50)
    var nickname: String?,
    @Column(name = "stf_national_id", length = 50)
    var nationalId: String?,
    @Column(name = "stf_image", length = 500)
    var image: String?,
    @ColumnDefault("0")
    @Column(name = "stf_isopen", nullable = false)
    var open: Boolean = false,
    @Column(name = "stf_open_order")
    var openOrder: Short?,
    @Column(name = "stf_open_group", columnDefinition = "CHAR")
    var openGroup: String?,
    @Column(name = "stf_email", length = 250)
    var email: String?,
    @Column(name = "stf_home_phone", length = 20)
    var homePhone: String?,
    @Column(name = "stf_hand_phone", length = 20)
    var handPhone: String?,
    @Column(name = "stf_birthdate", length = 20)
    var birthdate: String?,
    @Column(name = "stf_gender", nullable = false, length = 1, columnDefinition = "NCHAR")
    var gender: String,
    @Column(name = "stf_nationality", length = 50)
    var nationality: String?,
    @Column(name = "stf_bank_account", length = 50)
    var bankAccount: String?,
    @Column(name = "stf_bank", length = 50)
    var bank: String?,
    @Column(name = "stf_login_id", length = 25)
    var loginId: String?,
    @Column(name = "stf_password", length = 50)
    var password: String?,
    @Column(name = "stf_ismarried")
    var married: Boolean?,
    @Column(name = "stf_birth_place", length = 500)
    var birthPlace: String?,
    @Column(name = "stf_homepage", length = 500)
    var homepage: String?,
    @Column(name = "stf_inkorea", length = 1, columnDefinition = "NCHAR")
    var inKorea: String?,
    @Column(name = "stf_education_level", length = 50)
    var educationLevel: String?,
    @Column(name = "stf_note", length = 4000)
    var note: String?,
    @Column(name = "stf_isteacher")
    var teacher: Boolean?,
    @Column(name = "stf_isonline_teacher_only")
    var onlineTeacherOnly: Boolean?,
    @Column(name = "stf_iswmsupervisor")
    var wmSupervisor: Boolean?,
    @Column(name = "stf_online_time_type", length = 1, columnDefinition = "NCHAR")
    var onlineTimeType: String?,
    @Column(name = "stf_ibt_time_type", length = 1, columnDefinition = "NCHAR")
    var ibtTimeType: String?,
    @Column(name = "stf_rc_contact_point")
    var rcContactPoint: Boolean?,
    @Column(name = "brch_id")
    var branchId: Int?,
    @Column(name = "org_app_id")
    var orgAppId: Int?,
    @Column(name = "new_login", length = 50)
    var newLogin: String?,
    @Column(name = "login_backup", length = 50)
    var loginBackup: String?,
    @Column(name = "temp_type", length = 20)
    var tempType: String?,
    @Column(name = "eip_empid", length = 20)
    var eipEmployeeId: String?,
    @Column(name = "stf_noncdi")
    var noneCdi: Boolean?,
    @Column(name = "stf_noncdi_code")
    var noneCdiCode: Int?,
    @Column(name = "stf_ismsupervisor")
    var supervisor: Boolean?,
    @Column(name = "stf_ismmanager")
    var manager: Boolean?,
    @ColumnDefault("0")
    @Column(name = "stf_isunrestaccess")
    var unrestAccess: Boolean?,
    @Column(name = "stf_password_encryption", length = 1000)
    var passwordEncryption: String?,
    @Column(name = "stf_password_change_date")
    var passwordChangeDate: LocalDateTime?,
    @ColumnDefault("0")
    @Column(name = "stf_is_freelancer")
    var freelancer: Boolean?,
    @ColumnDefault("0")
    @Column(name = "stf_isadmin")
    var admin: Boolean?,
    @Column(name = "web_loginid", length = 100)
    var webLoginId: String?,
    @Column(name = "stf_std_id")
    var studentId: Int?,
    @Column(name = "stf_calling_number", length = 20)
    var callingNumber: String?,
    @Column(name = "allim_push_id")
    var allimPushId: Int?,
    @Column(name = "sams_login_id", length = 20)
    var samsLoginId: String?,
    @Column(name = "stf_work_type", length = 10)
    var workType: String?,
) : BaseAuditedEntity()