package com.buyeong.umji.api.service.mapping

import com.buyeong.umji.api.mapper.StaffMapper
import com.buyeong.umji.api.service.biz.CoreApiService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional(readOnly = true)
@Service
class CoreApiMappingService(
    private val service: CoreApiService,
    private val mapper: StaffMapper,
) {
    fun getStaff(staffId: Int) =
        service.getStaff(staffId).let {
            mapper.toModel(it)
        }
}