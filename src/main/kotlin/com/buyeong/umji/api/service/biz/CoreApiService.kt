package com.buyeong.umji.api.service.biz

import com.buyeong.umji.api.service.biz.client.CoreApi
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional(readOnly = true)
@Service
class CoreApiService(
    private val coreApi: CoreApi,
) {
    fun getStaff(staffId: Int) = coreApi.getStaff(staffId)
}