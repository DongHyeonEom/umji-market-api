package com.buyeong.umji.api.persistence.jpa.order.entity

import com.buyeong.umji.api.persistence.jpa.account.entity.AccountEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(name = "shipping_holiday")
class ShippingHolidayEntity {
    @Id
    @Column(name = "holiday_date")
    lateinit var holidayDate: LocalDate

    @Column(length = 200)
    var description: String? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    var creator: AccountEntity? = null

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()
}