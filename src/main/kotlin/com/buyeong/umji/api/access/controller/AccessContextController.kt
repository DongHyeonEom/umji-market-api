package com.buyeong.umji.api.access.controller

import com.buyeong.umji.api.access.model.AccessAudience
import com.buyeong.umji.api.access.model.AccessContextResponse
import com.buyeong.umji.api.access.service.AccessContextService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/access-context")
@Tag(name = "화면 접근 권한", description = "현재 계정의 audience별 permission과 허용 화면 정보")
class AccessContextController(
    private val accessContexts: AccessContextService,
) {
    @Operation(
        summary = "현재 화면 접근 context 조회",
        description = "화면 표시용 role·permission·허용 screen 정보를 반환하며 업무 API는 별도 권한 검사를 수행",
    )
    @GetMapping
    fun get(
        @Parameter(description = "화면 audience. ADMIN 또는 BUYER")
        @RequestParam audience: AccessAudience,
    ): AccessContextResponse = accessContexts.get(audience)
}
