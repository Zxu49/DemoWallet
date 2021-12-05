package com.example.demowallet.web

import android.util.Log
import com.coinbase.wallet.crypto.extensions.sha256
import okhttp3.*
import okio.ByteString

object WebsocketClient {
    private const val NORMAL_CLOSURE_STATUS = 1000
    private var sClient: OkHttpClient? = null
    private var sWebSocket: WebSocket? = null
    var listener : EchoWebSocketListener? = null
    lateinit var message : String

    @Synchronized
    fun startRequest() {
        if (sClient == null) {
            sClient = OkHttpClient()
        }
        if (sWebSocket == null) {
            val request = Request.Builder().url("wss://www.walletlink.org/rpc").build()
            listener = EchoWebSocketListener()
            sWebSocket = sClient!!.newWebSocket(request, listener!!)
        }
    }

    private fun joinSession(webSocket: WebSocket, sessionID: String, secret: String) {
        val sessionKey = "$sessionID, $secret WalletLink".sha256()
        println("sessionKey: $sessionKey")
        webSocket.send("\t{\"type\":\"JoinSession\",\"id\":1,\"sessionId\":\"$sessionID\",\"sessionKey\":\"$sessionKey\"}")
    }

    private fun sendMessage(webSocket: WebSocket, sessionID: String, secret: String) {
        val sessionKey = "$sessionID, $secret WalletLink".sha256()
        println("sessionKey: $sessionKey")
        webSocket.send("\t{\"type\":\"HostSession\",\"id\":1,\"sessionId\":\"$sessionID\",\"sessionKey\":\"$sessionKey\"}")
    }

    private fun sendMessage1(webSocket: WebSocket, sessionID: String) {
        webSocket.send("{\"type\":\"IsLinked\",\"id\":2,\"sessionId\":\"$sessionID\"}")
    }

    private fun sendMessage2(webSocket: WebSocket,sessionID: String) {
        webSocket.send("{\"type\":\"GetSessionConfig\",\"id\":3,\"sessionId\":\"$sessionID\"}\n")
    }

    private fun sendMessage3(webSocket: WebSocket, sessionID: String, data: String) {
        webSocket.send("{\"type\":\"PublishEvent\",\"id\":4,\"sessionId\":\"$sessionID\",\"event\":\"Web3Request\", \"data\":\"$data\",\"callWebhook\":false}")
    }

    private fun sendMessage4(webSocket: WebSocket, sessionID: String, data: String) {
        webSocket.send("{\"type\":\"PublishEvent\",\"id\":5,\"sessionId\":\"$sessionID\",\"event\":\"Web3Request\", \"data\":\"$data\",\"callWebhook\":false}")
    }

    private fun sendMessage5(webSocket: WebSocket, sessionID: String, data: String) {
        webSocket.send("{\"type\":\"PublishEvent\",\"id\":6,\"sessionId\":\"$sessionID\",\"event\":\"Web3Request\", \"data\":\"$data\",\"callWebhook\":false}")
    }

    fun joinSession(sessionID:String, secret: String) {
        if (sWebSocket != null) {
            joinSession(sWebSocket!!,sessionID, secret)
        }
    }

    fun sendMessage(sessionID:String, secret: String) {
        if (sWebSocket != null) {
            sendMessage(sWebSocket!!,sessionID, secret)
        }
    }

    fun sendMessage1(sessionID:String) {
        if (sWebSocket != null) {
            sendMessage1(sWebSocket!!, sessionID)
        }
    }

    fun sendMessage2(sessionID:String) {
        if (sWebSocket != null) {
            sendMessage2(sWebSocket!!, sessionID)
        }
    }

    fun sendMessage3(sessionID : String, data : String) {
        if (sWebSocket != null) {
            sendMessage3(sWebSocket!!, sessionID, data)
        }
    }

    fun sendMessage4(sessionID:String, data : String) {
        if (sWebSocket != null) {
            sendMessage4(sWebSocket!!, sessionID, data)
        }
    }

    fun sendMessage5(sessionID : String, data : String) {
        if (sWebSocket != null) {
            sendMessage5(sWebSocket!!, sessionID, data)
        }
    }

    @Synchronized
    fun closeWebSocket() {
        if (sWebSocket != null) {
            sWebSocket!!.close(NORMAL_CLOSURE_STATUS, "Goodbye!")
            sWebSocket = null
        }
    }

    @Synchronized
    fun destroy() {
        if (sClient != null) {
            sClient!!.dispatcher.executorService.shutdown()
            sClient = null
        }
    }

    private fun resetWebSocket() {
        synchronized(WebsocketClient::class.java) { sWebSocket = null }
    }

    class EchoWebSocketListener : WebSocketListener() {
        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.i(TAG, "Receiving: $text")
            message = text
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            Log.i(TAG, "Receiving: " + bytes.hex())
            println("Receiving: " + bytes.hex())
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(NORMAL_CLOSURE_STATUS, null)
            Log.i(TAG, "Closing: $code $reason")
            resetWebSocket()
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.i(TAG, "Closed: $code $reason")
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            t.printStackTrace()
            resetWebSocket()
        }

        companion object {
            private const val TAG = "EchoWebSocketListener"
        }
    }
}