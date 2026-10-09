package com.buyeong.umji.api.persistence.jpa.file.service

import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.persistence.jpa.account.repository.AccountRepository
import com.buyeong.umji.api.persistence.jpa.catalog.repository.ProductImageRepository
import com.buyeong.umji.api.persistence.jpa.file.entity.FileAssetEntity
import com.buyeong.umji.api.persistence.jpa.file.repository.FileAssetRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class FileAssetJpaEntityService(
    private val files: FileAssetRepository,
    private val accounts: AccountRepository,
    private val productImages: ProductImageRepository,
) {
    fun find(publicId: UUID): FileAssetEntity? = files.findByPublicId(publicId)

    fun existsUploadedProductImage(storageKey: String): Boolean =
        files.existsByStorageKeyAndFileTypeAndStatus(storageKey, PRODUCT_IMAGE, UPLOADED) &&
            productImages.existsPublicImageByStorageKey(storageKey)

    @Transactional
    fun create(
        fileType: String,
        ownerAccountPublicId: UUID?,
        originalFileName: String,
        storageKey: String,
        contentType: String,
        byteSize: Long,
        uploadTokenHash: String,
        uploadExpiresAt: java.time.Instant,
    ): FileAssetEntity {
        val owner = ownerAccountPublicId?.let { accounts.findByPublicId(it) ?: throw ItemNotFoundException("파일 소유 계정을 찾을 수 없습니다.") }
        return files.saveAndFlush(
            FileAssetEntity().apply {
                this.fileType = fileType
                ownerAccount = owner
                this.originalFileName = originalFileName
                this.storageKey = storageKey
                this.contentType = contentType
                this.byteSize = byteSize
                this.uploadTokenHash = uploadTokenHash
                status = UPLOAD_PENDING
                this.uploadExpiresAt = uploadExpiresAt
            },
        )
    }

    @Transactional
    fun markUploaded(file: FileAssetEntity): FileAssetEntity {
        file.status = UPLOADED
        file.uploadedAt = java.time.Instant.now()
        file.uploadTokenHash = CONSUMED_TOKEN_HASH
        return files.saveAndFlush(file)
    }

    private companion object {
        const val PRODUCT_IMAGE = "PRODUCT_IMAGE"
        const val UPLOAD_PENDING = "UPLOAD_PENDING"
        const val UPLOADED = "UPLOADED"
        const val CONSUMED_TOKEN_HASH = "0000000000000000000000000000000000000000000000000000000000000000"
    }
}