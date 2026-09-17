package com.buyeong.umji.api.persistence.jdbc.entity.cdi

import com.buyeong.umji.api.enums.StaffStatusEnum
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("cdi_staff")
class StaffJdbcEntity {
    @Id
    @Column("stf_id")
    var id: Int? = null

    @Column("stf_status")
    var status: StaffStatusEnum? = null

    @Column("stf_recruit_status")
    var recruitStatus: String? = null

    @Column("stf_lastname")
    var lastname: String? = null

    @Column("stf_firstname")
    var firstname: String? = null

    @Column("stf_middlename")
    var middlename: String? = null

    @Column("stf_nickname")
    var nickname: String? = null

    @Column("stf_national_id")
    var nationalId: String? = null

    @Column("stf_image")
    var image: String? = null

    @Column("stf_isopen")
    var open: Boolean? = false

    @Column("stf_open_order")
    var openOrder: Short? = null

    @Column("stf_open_group")
    var openGroup: Char? = null

    @Column("stf_email")
    var email: String? = null

    @Column("stf_home_phone")
    var homePhone: String? = null

    @Column("stf_hand_phone")
    var handPhone: String? = null

    @Column("stf_birthdate")
    var birthdate: String? = null

    @Column("stf_gender")
    var gender: String? = null

    @Column("stf_nationality")
    var nationality: String? = null

    @Column("stf_bank_account")
    var bankAccount: String? = null

    @Column("stf_bank")
    var bank: String? = null

    @Column("stf_login_id")
    var loginId: String? = null

    @Column("stf_password")
    var password: String? = null

    @Column("stf_ismarried")
    var married: Boolean? = null

    @Column("stf_birth_place")
    var birthPlace: String? = null

    @Column("stf_homepage")
    var homepage: String? = null

    @Column("stf_inkorea")
    var inKorea: String? = null

    @Column("stf_education_level")
    var educationLevel: String? = null

    @Column("stf_note")
    var note: String? = null

    @Column("stf_isteacher")
    var teacher: Boolean? = null

    @Column("stf_isonline_teacher_only")
    var onlineTeacherOnly: Boolean? = null

    @Column("stf_iswmsupervisor")
    var wmSupervisor: Boolean? = null

    @Column("stf_online_time_type")
    var onlineTimeType: String? = null

    @Column("stf_ibt_time_type")
    var ibtTimeType: String? = null

    @Column("stf_rc_contact_point")
    var rcContactPoint: Boolean? = null

    @Column("stf_createby")
    var createdBy: Int? = null

    @Column("stf_createdate")
    var createdAt: LocalDateTime? = null

    @Column("stf_lastupdateby")
    var updatedBy: Int? = null

    @Column("stf_lastupdate")
    var updatedAt: LocalDateTime? = null

    @Column("brch_id")
    var branchId: Int? = null

    @Column("org_app_id")
    var orgAppId: Int? = null

    @Column("new_login")
    var newLogin: String? = null

    @Column("login_backup")
    var loginBackup: String? = null

    @Column("temp_type")
    var tempType: String? = null

    @Column("eip_empid")
    var eipEmployeeId: String? = null

    @Column("stf_noncdi")
    var noneCdi: Boolean? = null

    @Column("stf_noncdi_code")
    var noneCdiCode: Int? = null

    @Column("stf_ismsupervisor")
    var supervisor: Boolean? = null

    @Column("stf_ismmanager")
    var manager: Boolean? = null

    @Column("stf_isunrestaccess")
    var unrestAccess: Boolean? = null

    @Column("stf_password_encryption")
    var passwordEncryption: String? = null

    @Column("stf_password_change_date")
    var passwordChangeDate: LocalDateTime? = null

    @Column("stf_is_freelancer")
    var freelancer: Boolean? = null

    @Column("stf_isadmin")
    var admin: Boolean? = null

    @Column("web_loginid")
    var webLoginId: String? = null

    @Column("stf_std_id")
    var studentId: Int? = null

    @Column("stf_calling_number")
    var callingNumber: String? = null

    @Column("allim_push_id")
    var allimPushId: Int? = null

    @Column("sams_login_id")
    var samsLoginId: String? = null

    @Column("stf_work_type")
    var workType: String? = null
}