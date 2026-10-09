package com.buyeong.umji.api.file.dto

import java.nio.file.Path

data class FileDownloadDto(val path: Path, val contentType: String, val byteSize: Long, val fileName: String)