package com.buyeong.umji.api.service.mapping

import com.buyeong.umji.api.mapper.StaffMapper
import com.buyeong.umji.api.model.StaffModel
import com.buyeong.umji.api.service.biz.StaffBizService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional(readOnly = true)
@Service
class StaffMappingService(
    private val bizService: StaffBizService,
    private val mapper: StaffMapper,
) {
    fun getStaff(id: Int): StaffModel =
        bizService.find(id)!!.let {
            mapper.toModel(it)
        }
}