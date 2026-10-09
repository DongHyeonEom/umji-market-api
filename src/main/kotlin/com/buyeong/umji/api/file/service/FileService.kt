package com.buyeong.umji.api.file.service

import com.buyeong.umji.api.exception.ClientBadRequestException
import com.buyeong.umji.api.exception.ForbiddenOperationException
import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.file.dto.FileDownloadDto
import com.buyeong.umji.api.file.integration.LocalFileStorage
import com.buyeong.umji.api.file.integration.LocalFileStorageProperties
import com.buyeong.umji.api.file.model.CreateFileUploadRequest
import com.buyeong.umji.api.file.model.FileUploadResponse
import com.buyeong.umji.api.persistence.jpa.file.entity.FileAssetEntity
import com.buyeong.umji.api.persistence.jpa.file.service.FileAssetJpaEntityService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.InputStream
import java.io.PushbackInputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64
import java.util.UUID

@Service
class FileService(
    private val files: FileAssetJpaEntityService,
    private val storage: LocalFileStorage,
    private val properties: LocalFileStorageProperties,
) {
    @Transactional
    fun createUpload(request: CreateFileUploadRequest): FileUploadResponse {
        val fileType = request.fileType.trim().uppercase()
        val contentType = request.contentType.trim().lowercase()
        val fileName = normalizeFileName(request.fileName)
        val maxBytes = maxBytes(fileType)
        require(contentType in allowedContentTypes(fileType)) { "허용하지 않는 파일 형식입니다." }
        require(request.byteSize in 1..maxBytes) { "파일 크기 제한을 초과했거나 올바르지 않습니다." }
        require(
            (fileType == PRODUCT_IMAGE && request.ownerAccountId == null) ||
                (fileType == BUSINESS_EVIDENCE && request.ownerAccountId != null)
        ) { "파일 분류와 소유 계정이 일치하지 않습니다." }

        val token = newToken()
        val expiresAt = Instant.now().plus(properties.uploadTokenTtlSeconds, ChronoUnit.SECONDS)
        val entity = files.create(
            fileType,
            request.ownerAccountId,
            fileName,
            UUID.randomUUID().toString(),
            contentType,
            request.byteSize,
            sha256(token),
            expiresAt,
        )
        return entity.toResponse(token)
    }

    @Transactional
    fun upload(fileId: UUID, token: String, contentType: String, content: InputStream): FileUploadResponse {
        val file = files.find(fileId) ?: throw ItemNotFoundException("업로드 파일을 찾을 수 없습니다.")
        require(file.status == UPLOAD_PENDING) { "이미 업로드했거나 업로드할 수 없는 파일입니다." }
        require(Instant.now().isBefore(file.uploadExpiresAt)) { "업로드 URL이 만료되었습니다." }
        require(MessageDigest.isEqual(sha256(token).toByteArray(), file.uploadTokenHash.toByteArray())) { "업로드 token이 올바르지 않습니다." }
        require(contentType.substringBefore(';').trim().lowercase() == file.contentType) { "요청 MIME 형식이 발급된 metadata와 다릅니다." }
        val verifiedContent = verifySignature(file.contentType, content)
        val writtenBytes = storage.write(file.fileType, file.storageKey, verifiedContent, minOf(file.byteSize, maxBytes(file.fileType)))
        if (writtenBytes != file.byteSize) {
            storage.delete(file.fileType, file.storageKey)
            throw ClientBadRequestException("요청 파일 크기가 발급된 metadata와 다릅니다.")
        }
        return try {
            files.markUploaded(file).toResponse(null)
        } catch (exception: Exception) {
            storage.delete(file.fileType, file.storageKey)
            throw exception
        }
    }

    @Transactional(readOnly = true)
    fun publicDownload(fileId: UUID): FileDownloadDto {
        val file = uploaded(fileId)
        if (file.fileType != PRODUCT_IMAGE || !files.existsUploadedProductImage(file.storageKey)) {
            throw ItemNotFoundException("공개된 상품 이미지 파일을 찾을 수 없습니다.")
        }
        return file.toDownload(storage)
    }

    @Transactional(readOnly = true)
    fun privateDownload(fileId: UUID, ownerAccountId: UUID): FileDownloadDto {
        val file = uploaded(fileId)
        if (file.fileType != BUSINESS_EVIDENCE || file.ownerAccount?.publicId != ownerAccountId) {
            throw ForbiddenOperationException("요청 계정의 사업자 증빙 파일에 접근할 수 없습니다.")
        }
        return file.toDownload(storage)
    }

    private fun uploaded(fileId: UUID): FileAssetEntity {
        val file = files.find(fileId) ?: throw ItemNotFoundException("파일을 찾을 수 없습니다.")
        if (file.status != UPLOADED) throw ItemNotFoundException("업로드가 완료된 파일을 찾을 수 없습니다.")
        return file
    }

    private fun normalizeFileName(value: String): String {
        val name = value.trim().replace('\\', '/').substringAfterLast('/')
        require(name.isNotBlank() && name !in setOf(".", "..") && name.length <= 255 && name.none(Char::isISOControl)) {
            "파일명이 올바르지 않습니다."
        }
        return name
    }

    private fun verifySignature(contentType: String, input: InputStream): InputStream {
        val stream = PushbackInputStream(input, 12)
        val header = stream.readNBytes(12)
        stream.unread(header)
        val valid = when (contentType) {
            "image/jpeg" -> header.startsWithBytes(0xFF, 0xD8, 0xFF)
            "image/png" -> header.startsWithBytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
            "image/webp" -> header.size >= 12 && header.startsWithBytes(0x52, 0x49, 0x46, 0x46) && header.copyOfRange(8, 12).contentEquals(byteArrayOf(0x57, 0x45, 0x42, 0x50))
            "application/pdf" -> header.startsWithBytes(0x25, 0x50, 0x44, 0x46, 0x2D)
            else -> false
        }
        require(valid) { "파일 내용이 선언한 MIME 형식과 일치하지 않습니다." }
        return stream
    }

    private fun ByteArray.startsWithBytes(vararg bytes: Int): Boolean = size >= bytes.size &&
        bytes.indices.all { (this[it].toInt() and 0xFF) == bytes[it] }

    private fun maxBytes(fileType: String): Long = when (fileType) {
        PRODUCT_IMAGE -> PRODUCT_IMAGE_MAX_BYTES
        BUSINESS_EVIDENCE -> BUSINESS_EVIDENCE_MAX_BYTES
        else -> throw ClientBadRequestException("파일 분류가 올바르지 않습니다.")
    }

    private fun allowedContentTypes(fileType: String): Set<String> = when (fileType) {
        PRODUCT_IMAGE -> setOf("image/jpeg", "image/png", "image/webp")
        BUSINESS_EVIDENCE -> setOf("application/pdf", "image/jpeg", "image/png")
        else -> throw ClientBadRequestException("파일 분류가 올바르지 않습니다.")
    }

    private fun newToken(): String = ByteArray(32).also(SECURE_RANDOM::nextBytes).let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray())
        .joinToString("") { "%02x".format(it) }

    private fun FileAssetEntity.toResponse(token: String?) = FileUploadResponse(
        fileId = requireNotNull(publicId),
        fileType = fileType,
        storageKey = if (fileType == PRODUCT_IMAGE) storageKey else null,
        fileName = originalFileName,
        contentType = contentType,
        byteSize = byteSize,
        uploaded = status == UPLOADED,
        uploadUrl = if (token == null) null else "/api/files/$publicId/content",
        uploadToken = token,
        uploadExpiresAt = uploadExpiresAt,
        publicUrl = if (fileType == PRODUCT_IMAGE && status == UPLOADED) "/api/files/public/$publicId" else null,
    )

    private fun FileAssetEntity.toDownload(storage: LocalFileStorage) =
        FileDownloadDto(storage.read(fileType, storageKey), contentType, byteSize, originalFileName)

    private companion object {
        const val PRODUCT_IMAGE = "PRODUCT_IMAGE"
        const val BUSINESS_EVIDENCE = "BUSINESS_EVIDENCE"
        const val UPLOAD_PENDING = "UPLOAD_PENDING"
        const val UPLOADED = "UPLOADED"
        const val PRODUCT_IMAGE_MAX_BYTES = 10L * 1024 * 1024
        const val BUSINESS_EVIDENCE_MAX_BYTES = 20L * 1024 * 1024
        val SECURE_RANDOM = SecureRandom()
    }
}