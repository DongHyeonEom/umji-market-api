package com.buyeong.umji.api.persistence.jdbc.service.cdi

import com.buyeong.umji.api.dto.StaffDto
import com.buyeong.umji.api.mapper.StaffMapper
import com.buyeong.umji.api.persistence.jdbc.repository.cdi.StaffJdbcRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations
import org.springframework.jdbc.core.namedparam.SqlParameterSource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional(readOnly = true)
@Service
class StaffJdbcEntityService(
    @param:Qualifier("defaultNamedParameterJdbcOperations")
    private val namedParameterJdbcOperations: NamedParameterJdbcOperations,
    private val staffJdbcRepository: StaffJdbcRepository,
    private val mapper: StaffMapper,
) {
    fun get(id: Int): StaffDto? =
        staffJdbcRepository.findById(id).get().let {
            mapper.toDto(it)
        }

    // #region getAllStaffIdByBranchId
    fun getAllStaffIdByBranchId(branchId: Int): List<Int> {
        val namedParameters: SqlParameterSource = MapSqlParameterSource(
            "BRANCH_ID",
            branchId,
        )

        return namedParameterJdbcOperations.queryForList(
            """
SELECT  stf.stf_id
FROM    cdi_staff AS stf
INNER JOIN cdi_staff_branch AS stf_brch
ON      stf.stf_id = stf_brch.stf_id
WHERE   stf_brch.brch_id = :BRANCH_ID
AND     stf.stf_status = 'Active'
;
            """.trimIndent(),
            namedParameters,
            Int::class.javaObjectType,
        ).filterNotNull()
    }
    // #endregion getAllStaffIdByBranchId
}