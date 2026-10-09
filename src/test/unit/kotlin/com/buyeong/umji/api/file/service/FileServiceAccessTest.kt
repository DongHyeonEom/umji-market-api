package com.buyeong.umji.api.file.service

import com.buyeong.umji.api.exception.ForbiddenOperationException
import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.file.integration.LocalFileStorage
import com.buyeong.umji.api.file.integration.LocalFileStorageProperties
import com.buyeong.umji.api.persistence.jpa.account.entity.AccountEntity
import com.buyeong.umji.api.persistence.jpa.file.entity.FileAssetEntity
import com.buyeong.umji.api.persistence.jpa.file.service.FileAssetJpaEntityService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import java.nio.file.Path
import java.util.UUID

class FileServiceAccessTest : DescribeSpec({
    val files = mockk<FileAssetJpaEntityService>()
    val storage = mockk<LocalFileStorage>()
    val service = FileService(files, storage, LocalFileStorageProperties())
    val fileId = UUID.randomUUID()
    val ownerId = UUID.randomUUID()

    describe("file access boundaries") {
        it("allows a public product image only after it is linked to a visible product") {
            clearMocks(files, storage)
            every { files.find(fileId) } returns uploadedAsset("PRODUCT_IMAGE")
            every { files.existsUploadedProductImage("storage-key") } returns true
            every { storage.read("PRODUCT_IMAGE", "storage-key") } returns Path.of("product.webp")

            service.publicDownload(fileId).contentType shouldBe "image/webp"
        }

        it("hides an unlinked product image from public access") {
            clearMocks(files, storage)
            every { files.find(fileId) } returns uploadedAsset("PRODUCT_IMAGE")
            every { files.existsUploadedProductImage("storage-key") } returns false

            shouldThrow<ItemNotFoundException> { service.publicDownload(fileId) }
        }

        it("allows private business evidence only to its owning account") {
            clearMocks(files, storage)
            val owner = AccountEntity().apply { publicId = ownerId }
            every { files.find(fileId) } returns uploadedAsset("BUSINESS_EVIDENCE", owner)
            every { storage.read("BUSINESS_EVIDENCE", "storage-key") } returns Path.of("evidence.pdf")

            service.privateDownload(fileId, ownerId).contentType shouldBe "image/png"
        }

        it("rejects access to private evidence by another account") {
            clearMocks(files, storage)
            val owner = AccountEntity().apply { publicId = ownerId }
            every { files.find(fileId) } returns uploadedAsset("BUSINESS_EVIDENCE", owner)

            shouldThrow<ForbiddenOperationException> { service.privateDownload(fileId, UUID.randomUUID()) }
        }
    }
})

private fun uploadedAsset(fileType: String, owner: AccountEntity? = null) = FileAssetEntity().apply {
    this.fileType = fileType
    ownerAccount = owner
    originalFileName = "upload.bin"
    storageKey = "storage-key"
    contentType = if (fileType == "PRODUCT_IMAGE") "image/webp" else "image/png"
    byteSize = 12
    uploadTokenHash = "0".repeat(64)
    status = "UPLOADED"
    uploadExpiresAt = java.time.Instant.now().plusSeconds(60)
    uploadedAt = java.time.Instant.now()
}