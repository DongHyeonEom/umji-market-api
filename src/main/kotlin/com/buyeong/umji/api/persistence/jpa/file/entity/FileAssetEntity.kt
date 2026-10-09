package com.buyeong.umji.api.persistence.jpa.file.entity

import com.buyeong.umji.api.persistence.jpa.account.entity.AccountEntity
import com.buyeong.umji.api.persistence.jpa.entity.backbone.DomainPublicEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "file_asset")
class FileAssetEntity : DomainPublicEntity() {
    @Column(name = "file_type", nullable = false, length = 40)
    lateinit var fileType: String

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_account_id")
    var ownerAccount: AccountEntity? = null

    @Column(name = "original_file_name", nullable = false, length = 255)
    lateinit var originalFileName: String

    @Column(name = "storage_key", nullable = false, unique = true, length = 500)
    lateinit var storageKey: String

    @Column(name = "content_type", nullable = false, length = 100)
    lateinit var contentType: String

    @Column(name = "byte_size", nullable = false)
    var byteSize: Long = 0

    @Column(name = "upload_token_hash", nullable = false, length = 64)
    lateinit var uploadTokenHash: String

    @Column(nullable = false, length = 30)
    lateinit var status: String

    @Column(name = "upload_expires_at", nullable = false)
    lateinit var uploadExpiresAt: Instant

    @Column(name = "uploaded_at")
    var uploadedAt: Instant? = null
}