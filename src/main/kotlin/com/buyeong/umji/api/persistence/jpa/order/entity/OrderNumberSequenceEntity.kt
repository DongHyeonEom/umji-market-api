package com.buyeong.umji.api.persistence.jpa.order

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(name = "order_number_sequence")
class OrderNumberSequenceEntity {
    @Id
    @Column(name = "order_date", nullable = false)
    lateinit var orderDate: LocalDate

    @Column(name = "last_value", nullable = false)
    var lastValue: Long = 0
}