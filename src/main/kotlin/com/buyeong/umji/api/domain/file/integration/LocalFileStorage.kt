package com.buyeong.umji.api.domain.file.integration

import com.buyeong.umji.api.exception.ClientBadRequestException
import org.springframework.stereotype.Component
import java.io.InputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption

@Component
class LocalFileStorage(
    private val properties: LocalFileStorageProperties,
) {
    private val root: Path = Path.of(properties.root).toAbsolutePath().normalize()

    fun write(fileType: String, storageKey: String, input: InputStream, maxBytes: Long): Long {
        val target = path(fileType, storageKey)
        val directory = target.parent
        Files.createDirectories(directory)
        val temporary = Files.createTempFile(directory, ".upload-", ".tmp")
        try {
            val writtenBytes = Files.newOutputStream(temporary, StandardOpenOption.TRUNCATE_EXISTING).use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var total = 0L
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > maxBytes) throw ClientBadRequestException("파일 크기 제한을 초과했습니다.")
                    output.write(buffer, 0, count)
                }
                total
            }
            if (writtenBytes == 0L) throw ClientBadRequestException("빈 파일은 업로드할 수 없습니다.")
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporary, target)
            }
            return writtenBytes
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    fun read(fileType: String, storageKey: String): Path {
        val target = path(fileType, storageKey)
        if (!Files.isRegularFile(target)) throw java.io.FileNotFoundException("파일을 찾을 수 없습니다.")
        return target
    }

    fun delete(fileType: String, storageKey: String) {
        Files.deleteIfExists(path(fileType, storageKey))
    }

    private fun path(fileType: String, storageKey: String): Path {
        val directory = when (fileType) {
            PRODUCT_IMAGE -> "public/product-images"
            BUSINESS_EVIDENCE -> "private/business-evidence"
            else -> throw IllegalArgumentException("파일 분류가 올바르지 않습니다.")
        }
        require(storageKey.matches(Regex("[0-9a-f-]{36}"))) { "저장 key가 올바르지 않습니다." }
        val target = root.resolve(directory).resolve(storageKey).normalize()
        require(target.startsWith(root)) { "저장 경로가 올바르지 않습니다." }
        return target
    }

    private companion object {
        const val PRODUCT_IMAGE = "PRODUCT_IMAGE"
        const val BUSINESS_EVIDENCE = "BUSINESS_EVIDENCE"
    }
}