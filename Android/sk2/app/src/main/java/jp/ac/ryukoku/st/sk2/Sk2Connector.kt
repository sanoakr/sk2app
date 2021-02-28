package jp.ac.ryukoku.st.sk2

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import java.io.*
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.SocketFactory
import javax.net.ssl.SSLSocketFactory
import kotlin.concurrent.thread

class Sk2Connector(): AnkoLogger {
    companion object {
        const val SK2HOST = "sk2.st.ryukoku.ac.jp"
        const val SK2PORT = 4440
        const val TIMEOUT_MILLISEC = 5000

        const val COMMAND_AUTH = "AUTH"
        const val REPLY_SUCCESS = "success"
        const val REPLY_AUTH_FAIL = "authfail"
        const val REPLY_FAIL = "fail"
    }

    // SSL Socket
    private var sslSocketFactory: SocketFactory = SSLSocketFactory.getDefault()
    private var sslSocket: Socket
    // IO Buffers
    private lateinit var input: InputStream
    private lateinit var output: OutputStream
    private lateinit var bufReader: BufferedReader
    private lateinit var bufWriter: BufferedWriter

    init {
        // Create SSL Socket
        sslSocket = sslSocketFactory.createSocket()
    }

    suspend fun connect(timeout: Int = TIMEOUT_MILLISEC, port: Int = SK2PORT, host: String = SK2HOST): Boolean
            = withContext(Dispatchers.IO) {
        try {
            info("start to try connect: $host $port $timeout")
            sslSocket.connect(InetSocketAddress(host, port), timeout)
            info("connect without exception")
            // Create IO Buffers
            input = sslSocket.inputStream
            output = sslSocket.outputStream
            bufReader = BufferedReader(InputStreamReader(input, "UTF-8"))
            bufWriter = BufferedWriter(OutputStreamWriter(output, "UTF-8"))
        } catch (e: Exception) {
            info(e)
            info("connection fail")
            return@withContext false
        }
        info("connected to $SK2HOST")
        return@withContext true
    }

    suspend fun apply(message: String): String = withContext(Dispatchers.IO) {
        val result: String
        try {
            // Send
            bufWriter.write(message)
            bufWriter.flush()
            // Receive
            result = bufReader.use { it.readText() }
            info(result)
        } catch (e: Exception) {
            return@withContext REPLY_FAIL
        }
        return@withContext result
    }

    suspend fun close() {
        if (sslSocket.isConnected)
            sslSocket.close()
        while (!sslSocket.isClosed)
            delay(100)
    }
}