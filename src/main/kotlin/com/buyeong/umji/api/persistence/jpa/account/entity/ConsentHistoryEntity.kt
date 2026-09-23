package com.buyeong.umji.api.persistence.jpa.account

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "consent_history")
class ConsentHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    lateinit var account: AccountEntity

    @Column(name = "consent_type", nullable = false)
    lateinit var consentType: String

    @Column(name = "document_version", nullable = false)
    lateinit var documentVersion: String

    @Column(name = "consent_method", nullable = false)
    lateinit var consentMethod: String

    @Column(name = "evidence_reference")
    var evidenceReference: String? = null

    @Column(name = "consented_at", nullable = false)
    lateinit var consentedAt: Instant
}
