package com.buyeong.umji.api.persistence.jpa.file.repository

import com.buyeong.umji.api.persistence.jpa.file.entity.FileAssetEntity
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface FileAssetRepository : JpaRepository<FileAssetEntity, Long> {
    fun findByPublicId(publicId: UUID): FileAssetEntity?
    fun existsByStorageKeyAndFileTypeAndStatus(storageKey: String, fileType: String, status: String): Boolean
}
