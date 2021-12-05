package com.example.demowallet.web

import com.coinbase.wallet.core.util.JSON

data class Web3ResponseDTO(
        val type: String = "WEB3_RESPONSE",
        val id: String,
        val response: Web3Response
) {
    companion object {
        fun fromJson(json: ByteArray): Web3ResponseDTO? = JSON.fromJsonString(String(json, Charsets.UTF_8))
    }
}

data class Web3Response(val method: String, val result: String?, val errorMessage: String?) {
    companion object {
        fun fromJson(json: ByteArray): Web3ResponseDTO? = JSON.fromJsonString(String(json, Charsets.UTF_8))
    }
}