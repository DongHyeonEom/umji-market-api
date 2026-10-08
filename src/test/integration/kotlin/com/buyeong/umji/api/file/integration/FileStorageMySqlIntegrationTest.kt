package com.buyeong.umji.api.file.integration

import com.buyeong.umji.api.file.model.CreateFileUploadRequest
import com.buyeong.umji.api.file.service.FileService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.Rollback
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path

@SpringBootTest(
    properties = [
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=none",
        "umji.file.storage.root=build/test-file-storage",
    ],
)
@ActiveProfiles("local")
@Transactional
@Rollback
class FileStorageMySqlIntegrationTest {
    @Autowired
    private lateinit var jdbc: JdbcTemplate

    @Autowired
    private lateinit var files: FileService

    @Autowired
    private lateinit var storage: LocalFileStorage

    private var storedKey: String? = null

    @AfterEach
    fun removeStoredFile() {
        storedKey?.let { storage.delete("PRODUCT_IMAGE", it) }
    }

    @Test
    fun `flyway stores file metadata in mysql and original bytes in local storage`() {
        val asset = files.createUpload(
            CreateFileUploadRequest("PRODUCT_IMAGE", null, "catalog/photo.png", "image/png", pngBytes.size.toLong()),
        )
        val key = requireNotNull(asset.storageKey)
        storedKey = key
        val uploaded = files.upload(asset.fileId, requireNotNull(asset.uploadToken), "image/png", ByteArrayInputStream(pngBytes))

        val metadata = jdbc.queryForMap(
            "SELECT file_type, content_type, byte_size, upload_token_hash, status FROM file_asset WHERE storage_key = ?",
            key,
        )
        assertThat(metadata["file_type"]).isEqualTo("PRODUCT_IMAGE")
        assertThat(metadata["content_type"]).isEqualTo("image/png")
        assertThat((metadata["byte_size"] as Number).toLong()).isEqualTo(pngBytes.size.toLong())
        assertThat(metadata["status"]).isEqualTo("UPLOADED")
        assertThat(metadata["upload_token_hash"]).isNotEqualTo(asset.uploadToken)
        assertThat((metadata["upload_token_hash"] as String)).hasSize(64)
        assertThat(uploaded.uploaded).isTrue()

        val contentColumns = jdbc.queryForObject(
            """SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'file_asset'
                AND data_type IN ('tinyblob', 'blob', 'mediumblob', 'longblob')""".trimIndent(),
            Int::class.java,
        )
        assertThat(contentColumns).isZero()

        val savedPath = storage.read("PRODUCT_IMAGE", key)
        assertThat(Files.isRegularFile(savedPath)).isTrue()
        assertThat(Files.readAllBytes(savedPath)).containsExactly(*pngBytes)
        assertThat(savedPath.toAbsolutePath().normalize()).startsWith(Path.of("build/test-file-storage").toAbsolutePath().normalize())
    }

    private companion object {
        val pngBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3, 4)
    }
}
