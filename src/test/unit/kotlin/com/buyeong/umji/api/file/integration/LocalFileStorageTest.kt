package com.buyeong.umji.api.file.integration

import com.buyeong.umji.api.exception.ClientBadRequestException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.io.ByteArrayInputStream
import java.nio.file.Files

class LocalFileStorageTest : DescribeSpec({
    describe("local file storage size enforcement") {
        it("stores a file whose actual size is at the limit") {
            val root = Files.createTempDirectory("umji-file-storage-")
            try {
                val storage = LocalFileStorage(LocalFileStorageProperties(root.toString()))
                val contents = byteArrayOf(1, 2, 3, 4)

                storage.write("PRODUCT_IMAGE", "00000000-0000-0000-0000-000000000001", ByteArrayInputStream(contents), 4) shouldBe 4
                Files.readAllBytes(root.resolve("public/product-images/00000000-0000-0000-0000-000000000001"))
                    .contentEquals(contents) shouldBe true
            } finally {
                root.toFile().deleteRecursively()
            }
        }

        it("rejects an actual file size above the limit and removes its temporary file") {
            val root = Files.createTempDirectory("umji-file-storage-")
            try {
                val storage = LocalFileStorage(LocalFileStorageProperties(root.toString()))
                val key = "00000000-0000-0000-0000-000000000002"

                shouldThrow<ClientBadRequestException> {
                    storage.write("PRODUCT_IMAGE", key, ByteArrayInputStream(byteArrayOf(1, 2, 3, 4)), 3)
                }
                Files.list(root.resolve("public/product-images")).use { it.count() } shouldBe 0
            } finally {
                root.toFile().deleteRecursively()
            }
        }
    }
})