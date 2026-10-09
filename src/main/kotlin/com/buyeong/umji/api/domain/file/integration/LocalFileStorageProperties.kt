package com.buyeong.umji.api.domain.file.integration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("umji.file.storage")
data class LocalFileStorageProperties(
    val root: String = "E:/buyeong_dev/umji-market/data/files",
    val uploadTokenTtlSeconds: Long = 900,
)