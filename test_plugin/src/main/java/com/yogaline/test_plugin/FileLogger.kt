package com.yogaline.test_plugin

import java.io.File
import java.io.FileOutputStream

class FileLogger(
    logFile: File,
) {

    private val file = if (logFile.isDirectory) {
        File(logFile, "compiler-plugin-logs.txt")
    } else {
        logFile
    }

    private val writer by lazy {
        FileOutputStream(file, true)
    }

    init {
        file.delete()
        file.createNewFile()
    }

    fun logMsg(msg: String) {
        writer.write(msg.toByteArray())
        writer.write("\n".toByteArray())
    }
}
