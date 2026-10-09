package com.buyeong.umji.api.persistence.jpa.file.repository

import com.buyeong.umji.api.persistence.jpa.file.entity.FileAssetEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface FileAssetRepository : JpaRepository<FileAssetEntity, Long> {
    fun findByPublicId(publicId: UUID): FileAssetEntity?
    fun existsByStorageKeyAndFileTypeAndStatus(storageKey: String, fileType: String, status: String): Boolean
}