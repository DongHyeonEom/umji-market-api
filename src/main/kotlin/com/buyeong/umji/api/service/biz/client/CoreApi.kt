package com.buyeong.umji.api.service.biz.client

import com.buyeong.umji.api.service.biz.client.response.StaffResponse
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange

@HttpExchange
interface CoreApi {
    @GetExchange("/staff/{staffId}")
    fun getStaff(
        @PathVariable staffId: Int,
    ): StaffResponse
}