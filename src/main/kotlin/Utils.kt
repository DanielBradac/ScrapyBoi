package org.example

import java.io.File

fun deleteAllFilesInDirectory(directory: File) {
    if (directory.exists() && directory.isDirectory) {
        directory.listFiles()?.forEach { file ->
            if (file.isFile) {
                file.delete()
            }
        }
    }
}

fun sanitizeFilename(filename: String): String {
    return filename.replace(Regex("[\\\\/:*?\"<>|]"), "_") // Replace invalid characters with "_"
}