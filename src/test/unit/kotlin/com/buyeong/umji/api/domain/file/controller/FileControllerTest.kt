package com.buyeong.umji.api.domain.file.controller

import com.buyeong.umji.api.domain.auth.security.OperationAuthorization
import com.buyeong.umji.api.exception.ForbiddenOperationException
import com.buyeong.umji.api.domain.file.model.CreateFileUploadRequest
import com.buyeong.umji.api.domain.file.model.FileUploadResponse
import com.buyeong.umji.api.domain.file.service.FileService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.security.core.Authentication
import java.time.Instant
import java.util.UUID

class FileControllerTest : DescribeSpec({
    val service = mockk<FileService>()
    val authorization = mockk<OperationAuthorization>()
    val controller = FileController(service, authorization)
    val authentication = mockk<Authentication>()

    describe("file upload authorization") {
        it("requires PRODUCT_WRITE for a product image upload") {
            clearMocks(authorization, service)
            val request = CreateFileUploadRequest("PRODUCT_IMAGE", null, "product.webp", "image/webp", 12)
            val response = FileUploadResponse(
                UUID.randomUUID(), "PRODUCT_IMAGE", "opaque-key", "product.webp", "image/webp", 12,
                false, "/api/files/id/content", "token", Instant.now().plusSeconds(900), null,
            )
            every { authorization.hasPermission(authentication, "PRODUCT_WRITE") } returns true
            every { service.createUpload(request) } returns response

            controller.createUpload(request, authentication)

            verify(exactly = 1) { authorization.hasPermission(authentication, "PRODUCT_WRITE") }
            verify(exactly = 1) { service.createUpload(request) }
        }

        it("rejects product image upload when PRODUCT_WRITE is missing") {
            clearMocks(authorization, service)
            val request = CreateFileUploadRequest("PRODUCT_IMAGE", null, "product.webp", "image/webp", 12)
            every { authorization.hasPermission(authentication, "PRODUCT_WRITE") } returns false

            shouldThrow<ForbiddenOperationException> { controller.createUpload(request, authentication) }

            verify(exactly = 0) { service.createUpload(any()) }
        }

        it("requires ADMIN_ACCOUNT_MANAGE for business evidence uploads") {
            clearMocks(authorization, service)
            val request = CreateFileUploadRequest(
                "BUSINESS_EVIDENCE",
                UUID.randomUUID(),
                "license.pdf",
                "application/pdf",
                12,
            )
            val response = FileUploadResponse(
                UUID.randomUUID(), "BUSINESS_EVIDENCE", null, "license.pdf", "application/pdf", 12,
                false, "/api/files/id/content", "token", Instant.now().plusSeconds(900), null,
            )
            every { authorization.hasPermission(authentication, "ADMIN_ACCOUNT_MANAGE") } returns true
            every { service.createUpload(request) } returns response

            controller.createUpload(request, authentication)

            verify(exactly = 1) { authorization.hasPermission(authentication, "ADMIN_ACCOUNT_MANAGE") }
            verify(exactly = 1) { service.createUpload(request) }
        }
    }
})