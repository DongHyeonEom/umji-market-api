package com.buyeong.umji.api.controller

import com.buyeong.umji.api.model.StaffModel
import com.buyeong.umji.api.service.mapping.CoreApiMappingService
import com.buyeong.umji.api.service.mapping.StaffMappingService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Staff 정보", description = "Staff 조회 Apis")
@RequestMapping(path = ["/staffs"])
@RestController
class StaffRestController(
    private val mappingService: StaffMappingService,
    private val coreMappingService: CoreApiMappingService,
) {
    @Operation(
        summary = "Staff : 직원(강사 포함) 조회",
        description = "Staff Id 조회",
    )
    @GetMapping("/{id}")
    fun getStaff(
        @PathVariable id: Int,
    ): ResponseEntity<StaffModel> = ResponseEntity.ok(mappingService.getStaff(id))

    @Operation(
        summary = "Core API Staff : 직원(강사 포함) 조회",
        description = "Staff Id 조회",
    )
    @GetMapping("/{id}/core")
    fun getCoreStaff(
        @PathVariable id: Int,
    ): ResponseEntity<StaffModel> = ResponseEntity.ok(coreMappingService.getStaff(id))
}