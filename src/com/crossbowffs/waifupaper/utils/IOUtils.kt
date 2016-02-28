package com.crossbowffs.waifupaper.utils

import java.io.*

object IOUtils {
    fun copyStream(input: InputStream, output: OutputStream, bufferSize: Int = 4096) {
        val buffer = ByteArray(bufferSize)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            output.write(buffer, 0, count)
        }
    }

    fun streamToByteArray(input: InputStream, bufferSize: Int = 4096): ByteArray {
        val output = ByteArrayOutputStream()
        copyStream(input, output, bufferSize)
        return output.toByteArray()
    }

    fun streamToString(input: InputStream, encoding: String = "UTF-8", bufferSize: Int = 2048): String {
        val buffer = CharArray(bufferSize)
        val sb = StringBuilder()
        InputStreamReader(input, encoding).use {
            while (true) {
                val count = it.read(buffer, 0, bufferSize)
                if (count < 0) break
                sb.append(buffer, 0, count)
            }
        }
        return sb.toString()
    }

    fun writeStreamToFile(input: InputStream, outputFile: File, bufferSize: Int = 4096) {
        FileOutputStream(outputFile).use { output ->
            copyStream(input, output, bufferSize)
        }
    }

    fun joinPath(basePath: File, vararg subPaths: String): File {
        return subPaths.fold(basePath, { f, p -> File(f, p) })
    }

    fun closeSilently(closeable: Closeable) {
        try {
            closeable.close()
        } catch (e: IOException) {
            // Ignore
        }
    }
}

fun InputStream.copyTo(output: OutputStream, buffersize: Int = 4096) = IOUtils.copyStream(this, output, buffersize)
fun InputStream.toByteArray(buffersize: Int = 4096) = IOUtils.streamToByteArray(this, buffersize)
fun InputStream.toString(encoding: String = "UTF-8", buffersize: Int = 4096) = IOUtils.streamToString(this, encoding, buffersize)
fun InputStream.writeToFile(file: File, buffersize: Int = 4096) = IOUtils.writeStreamToFile(this, file, buffersize)
fun File.join(vararg subPaths: String) = IOUtils.joinPath(this, *subPaths)
fun Closeable.closeSilently() = IOUtils.closeSilently(this)
