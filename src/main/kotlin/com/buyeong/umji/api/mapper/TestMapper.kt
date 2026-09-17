package com.buyeong.umji.api.mapper

import com.buyeong.umji.api.config.MapstructConfig
import com.buyeong.umji.api.dto.TestDto
import com.buyeong.umji.api.persistence.jpa.entity.cdi.TestJpaEntity
import org.mapstruct.Mapper

@Mapper(config = MapstructConfig::class)
interface TestMapper {
    fun toDto(entity: TestJpaEntity?): TestDto?

    fun toDtoList(entityList: List<TestJpaEntity>): List<TestDto>

    fun toEntity(dto: TestDto): TestJpaEntity
}