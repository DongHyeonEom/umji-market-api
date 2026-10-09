package com.buyeong.umji.api.domain.file.controller

import com.buyeong.umji.api.domain.auth.security.OperationAuthorization
import com.buyeong.umji.api.exception.ForbiddenOperationException
import com.buyeong.umji.api.domain.file.dto.FileDownloadDto
import com.buyeong.umji.api.domain.file.model.CreateFileUploadRequest
import com.buyeong.umji.api.domain.file.model.FileUploadResponse
import com.buyeong.umji.api.domain.file.service.FileService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.core.io.InputStreamResource
import org.springframework.http.CacheControl
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.nio.file.Files
import java.util.UUID

@RestController
@RequestMapping("/api/files")
class FileController(
    private val files: FileService,
    private val authorization: OperationAuthorization,
) {
    @PostMapping("/uploads")
    fun createUpload(
        @Valid @RequestBody request: CreateFileUploadRequest,
        authentication: Authentication?,
    ): FileUploadResponse {
        val permission = when (request.fileType.trim().uppercase()) {
            "PRODUCT_IMAGE" -> "PRODUCT_WRITE"
            "BUSINESS_EVIDENCE" -> "ADMIN_ACCOUNT_MANAGE"
            else -> null
        }
        if (permission == null || !authorization.hasPermission(authentication, permission)) {
            throw ForbiddenOperationException("파일 업로드 권한이 없습니다.")
        }
        return files.createUpload(request)
    }

    @PutMapping("/{fileId}/content", consumes = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun uploadContent(
        @PathVariable fileId: UUID,
        @RequestHeader("X-Upload-Token") uploadToken: String,
        @RequestHeader(HttpHeaders.CONTENT_TYPE) contentType: String,
        request: HttpServletRequest,
    ): FileUploadResponse = files.upload(fileId, uploadToken, contentType, request.inputStream)

    @GetMapping("/public/{fileId}")
    fun publicImage(@PathVariable fileId: UUID): ResponseEntity<InputStreamResource> = response(files.publicDownload(fileId), true)

    @GetMapping("/operation/{fileId}")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'ADMIN_ACCOUNT_MANAGE')")
    fun privateEvidence(
        @PathVariable fileId: UUID,
        @RequestParam ownerAccountId: UUID,
    ): ResponseEntity<InputStreamResource> = response(files.privateDownload(fileId, ownerAccountId), false)

    private fun response(file: FileDownloadDto, publiclyCacheable: Boolean): ResponseEntity<InputStreamResource> {
        val resource = InputStreamResource(Files.newInputStream(file.path))
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(file.contentType))
            .contentLength(file.byteSize)
            .cacheControl(if (publiclyCacheable) CacheControl.maxAge(java.time.Duration.ofHours(1)).cachePublic() else CacheControl.noStore())
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(file.fileName).build().toString())
            .body(resource)
    }
}