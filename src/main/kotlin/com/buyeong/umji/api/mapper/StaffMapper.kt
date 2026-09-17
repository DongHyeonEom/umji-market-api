package com.buyeong.umji.api.mapper

import com.buyeong.umji.api.config.MapstructConfig
import com.buyeong.umji.api.dto.StaffDto
import com.buyeong.umji.api.model.StaffModel
import com.buyeong.umji.api.persistence.jdbc.entity.cdi.StaffJdbcEntity
import com.buyeong.umji.api.persistence.jpa.entity.cdi.StaffJpaEntity
import com.buyeong.umji.api.service.biz.client.response.StaffResponse
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = MapstructConfig::class)
interface StaffMapper {
    fun toDto(entity: StaffJpaEntity?): StaffDto?

    fun toDto(entity: StaffJdbcEntity?): StaffDto?

    fun toModel(dto: StaffDto): StaffModel

    fun toDto(model: StaffModel): StaffDto

    fun toEntity(dto: StaffDto): StaffJpaEntity

    fun toDtoList(entityList: List<StaffJpaEntity>): List<StaffDto>

    fun toModelList(dtoList: List<StaffDto>): List<StaffModel>

    @Mapping(source = "middleName", target = "middlename")
    @Mapping(source = "staffStudentId", target = "studentId")
    fun toModel(response: StaffResponse): StaffModel
}