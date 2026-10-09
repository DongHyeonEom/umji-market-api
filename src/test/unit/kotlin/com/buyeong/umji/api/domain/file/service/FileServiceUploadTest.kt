package com.buyeong.umji.api.domain.file.service

import com.buyeong.umji.api.exception.ClientBadRequestException
import com.buyeong.umji.api.domain.file.integration.LocalFileStorage
import com.buyeong.umji.api.domain.file.integration.LocalFileStorageProperties
import com.buyeong.umji.api.domain.file.model.CreateFileUploadRequest
import com.buyeong.umji.api.persistence.jpa.file.entity.FileAssetEntity
import com.buyeong.umji.api.persistence.jpa.file.service.FileAssetJpaEntityService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID

class FileServiceUploadTest : DescribeSpec({
    val files = mockk<FileAssetJpaEntityService>()
    val storage = mockk<LocalFileStorage>()
    val service = FileService(files, storage, LocalFileStorageProperties())
    val fileId = UUID.randomUUID()

    describe("file upload restrictions") {
        it("rejects a MIME type that is not allowed for product images") {
            clearMocks(files, storage)
            shouldThrow<IllegalArgumentException> {
                service.createUpload(CreateFileUploadRequest("PRODUCT_IMAGE", null, "image.pdf", "application/pdf", 10))
            }
            verify(exactly = 0) { files.create(any(), any(), any(), any(), any(), any(), any(), any()) }
        }

        it("rejects a file size above the product image limit") {
            clearMocks(files, storage)
            shouldThrow<IllegalArgumentException> {
                service.createUpload(CreateFileUploadRequest("PRODUCT_IMAGE", null, "image.png", "image/png", 10L * 1024 * 1024 + 1))
            }
            verify(exactly = 0) { files.create(any(), any(), any(), any(), any(), any(), any(), any()) }
        }

        it("accepts an image at the exact size limit") {
            clearMocks(files, storage)
            val asset = pendingAsset().apply { byteSize = 10L * 1024 * 1024 }
            every { files.create(any(), any(), any(), any(), any(), any(), any(), any()) } returns asset

            val response = service.createUpload(
                CreateFileUploadRequest("PRODUCT_IMAGE", null, "folder\\image.png", "image/png", 10L * 1024 * 1024),
            )

            response.fileName shouldBe "image.png"
            response.uploadToken.isNullOrBlank() shouldBe false
            verify(exactly = 1) { files.create("PRODUCT_IMAGE", null, "image.png", any(), "image/png", 10L * 1024 * 1024, any(), any()) }
        }

        it("rejects an upload whose request MIME differs from the issued metadata") {
            clearMocks(files, storage)
            val asset = pendingAsset()
            every { files.find(fileId) } returns asset

            shouldThrow<IllegalArgumentException> {
                service.upload(fileId, "secret-token", "image/jpeg", ByteArrayInputStream(pngHeader))
            }
            verify(exactly = 0) { storage.write(any(), any(), any(), any()) }
        }

        it("removes the stored file when the uploaded byte count differs from metadata") {
            clearMocks(files, storage)
            val asset = pendingAsset()
            every { files.find(fileId) } returns asset
            every { storage.write(any(), any(), any(), any()) } returns (asset.byteSize + 1)
            every { storage.delete(any(), any()) } returns Unit

            shouldThrow<ClientBadRequestException> {
                service.upload(fileId, "secret-token", "image/png", ByteArrayInputStream(pngHeader))
            }
            verify(exactly = 1) { storage.delete("PRODUCT_IMAGE", "storage-key") }
            verify(exactly = 0) { files.markUploaded(any()) }
        }

        it("rejects content whose signature does not match its declared image MIME") {
            clearMocks(files, storage)
            every { files.find(fileId) } returns pendingAsset()

            shouldThrow<IllegalArgumentException> {
                service.upload(fileId, "secret-token", "image/png", ByteArrayInputStream("not a png".toByteArray()))
            }
            verify(exactly = 0) { storage.write(any(), any(), any(), any()) }
        }

        it("rejects an expired upload token") {
            clearMocks(files, storage)
            every { files.find(fileId) } returns pendingAsset().apply { uploadExpiresAt = Instant.now().minusSeconds(1) }

            shouldThrow<IllegalArgumentException> {
                service.upload(fileId, "secret-token", "image/png", ByteArrayInputStream(pngHeader))
            }
            verify(exactly = 0) { storage.write(any(), any(), any(), any()) }
        }

        it("consumes the upload token after one successful upload") {
            clearMocks(files, storage)
            val asset = pendingAsset()
            every { files.find(fileId) } returns asset
            every { storage.write("PRODUCT_IMAGE", "storage-key", any(), any()) } returns pngHeader.size.toLong()
            every { files.markUploaded(asset) } answers {
                asset.status = "UPLOADED"
                asset
            }

            service.upload(fileId, "secret-token", "image/png", ByteArrayInputStream(pngHeader)).uploaded shouldBe true
            shouldThrow<IllegalArgumentException> {
                service.upload(fileId, "secret-token", "image/png", ByteArrayInputStream(pngHeader))
            }
            verify(exactly = 1) { storage.write("PRODUCT_IMAGE", "storage-key", any(), any()) }
        }
    }
})

private fun pendingAsset() = FileAssetEntity().apply {
    val publicIdField = FileAssetEntity::class.java.superclass.getDeclaredField("publicId")
    publicIdField.isAccessible = true
    publicIdField.set(this, UUID.randomUUID())
    fileType = "PRODUCT_IMAGE"
    originalFileName = "image.png"
    storageKey = "storage-key"
    contentType = "image/png"
    byteSize = pngHeader.size.toLong()
    uploadTokenHash = sha256("secret-token")
    status = "UPLOAD_PENDING"
    uploadExpiresAt = Instant.now().plusSeconds(300)
}

private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
    .digest(value.toByteArray())
    .joinToString("") { "%02x".format(it) }

private val pngHeader = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)