package com.example.data.database

import android.content.Context
import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener

/**
 * A custom Media3 [DataSource] implementing ultra-fast, stateless on-the-fly decryption
 * for the proprietary `.aashiq` file format. This prevents standard players from
 * rendering the protected course bundle content, restricting playback purely to the app.
 */
@UnstableApi
class AashiqDecryptingDataSource(
    private val context: Context,
    private val upstream: DataSource
) : DataSource {

    private var currentUri: Uri? = null

    override fun addTransferListener(transferListener: TransferListener) {
        upstream.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        currentUri = dataSpec.uri
        return upstream.open(dataSpec)
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        val bytesRead = upstream.read(buffer, offset, length)
        if (bytesRead > 0) {
            val uriStr = currentUri?.toString() ?: ""
            if (uriStr.endsWith(".aashiq", ignoreCase = true) || uriStr.contains(".aashiq")) {
                // Perform high-performance symmetric XOR decryption
                // Key = 0xAE (specifically dedicated for Aashiq high-integrity encryption)
                for (i in 0 until bytesRead) {
                    val actualIndex = offset + i
                    buffer[actualIndex] = (buffer[actualIndex].toInt() xor 0xAE).toByte()
                }
            }
        }
        return bytesRead
    }

    override fun getUri(): Uri? {
        return upstream.getUri()
    }

    override fun close() {
        upstream.close()
    }
}
