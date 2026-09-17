package com.buyeong.umji.api.persistence.jdbc.repository.cdi

import com.buyeong.umji.api.persistence.jdbc.entity.cdi.StaffJdbcEntity
import org.springframework.data.repository.ListCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface StaffJdbcRepository : ListCrudRepository<StaffJdbcEntity, Int>