package com.buyeong.umji.api.persistence.jpa.repository.cdi

import com.buyeong.umji.api.persistence.jpa.entity.cdi.TestJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TestJpaRepository : JpaRepository<TestJpaEntity, Int>