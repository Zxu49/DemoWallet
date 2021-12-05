package com.example.demowallet.utils

import com.coinbase.wallet.core.extensions.base64EncodedString
import com.coinbase.wallet.crypto.extensions.sha256

data class Authorization(val sessionId: String, val secret: String) {

    /**
     *  Fix salt coming from server
     */
    val salt : String
        get () {
            return "WalletLink"
        }

    /**
     *  Parameters coming from Dapp for dapp identified wallet,
     *  wallet will used it to identified dapp in later signature
     */
    val sessionKey : String
        get() {
            return "$sessionId, $secret $salt".sha256()
        }

    /**
     * Password is actual sessionKey in wallet link, show here in case for misleading
     */
    val password : String
        get() {
            return sessionKey
        }

    /**
     * Properly encoded and formatted HTTP basic authentication header value
     */
    val basicAuth: String
        get() {
            val credentialString = "$sessionId:$password"
            val data = credentialString.toByteArray(Charsets.UTF_8)

            return "Basic ${data.base64EncodedString()}"
        }

    companion object
}