// Copyright (c) 2018-2019 Coinbase, Inc. <https://coinbase.com/>
// Licensed under the Apache License, version 2.0

package com.example.demowallet

import android.content.Context
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.coinbase.wallet.core.extensions.base64EncodedString
import com.coinbase.wallet.crypto.ciphers.AES256GCM
import com.coinbase.wallet.http.connectivity.Internet
import com.coinbase.walletlink.WalletLink
import com.coinbase.walletlink.models.ClientMetadataKey
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class WalletTests {
    private val sessionId = "35245454007279607403463727553499"
    private val secret = "6211949012973140237169099175835859749177568667191566915960143749"
    private val signData = "[\"0x03F6f282373900C2F6CE53B5A9f595b92aC5f5E5\"]".toByteArray(Charsets.UTF_8)
    private val timeOut = 5L

    // For test the method, you need to use DApp provide sessionId and secret to register on the server first
    @Test
    fun testWalletLinkConnect() {
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        val intentFilter = IntentFilter().apply { addAction(ConnectivityManager.CONNECTIVITY_ACTION) }

        appContext.registerReceiver(Internet, intentFilter)
        Internet.startMonitoring()

        val walletLink = WalletLink(
            notificationUrl = URL("https://walletlink.herokuapp.com"),
            context = appContext
        )

        val latch = CountDownLatch(3)
        GlobalScope.launch {
            val metadata = mutableMapOf<ClientMetadataKey, String>()
            metadata[ClientMetadataKey.EthereumAddress] = "0x03F6f282373900C2F6CE53B5A9f595b92aC5f5E5"

            walletLink.link(
                sessionId = sessionId,
                userId = "1",
                version = "1",
                secret = secret,
                url = URL("https://www.walletlink.org"),
                metadata = metadata
            )   .timeout(timeOut, TimeUnit.SECONDS)
                .subscribe(
                    {
                        print("wallet link connected!!")
                        latch.countDown()
                    },
                    {
                        Assert.fail("Unable to connect due to $it")
                        latch.countDown()
                    }
                )
        }
        val requests = walletLink.requestsObservable.blockingFirst()
        Assert.assertNotNull(requests)
        walletLink.approve(requests.hostRequestId, signData).timeout(timeOut, TimeUnit.SECONDS)
            .subscribeBy(onSuccess = {
                print("Approve request approval")
                latch.countDown()
            }, onError = { error ->
                print("Failed to receive request approval")
                latch.countDown()
            }
            )
        walletLink.reject(requests.hostRequestId).timeout(timeOut, TimeUnit.SECONDS)
            .subscribeBy(onSuccess = {
                print("Reject request approval")
                latch.countDown()
            }, onError = { error ->
                print("Failed to receive request approval")
                latch.countDown()
            })
        latch.await()
    }

    @Test
    fun encryptionDecryption() {
        val data = "Needs encryption".toByteArray()
        val key = "c9db0147e942b2675045e3f61b247692".toByteArray()
        val iv = "123456789012".toByteArray()
        val (encryptedData, authTag) = AES256GCM.encrypt(data, key, iv)

        print(Base64.encodeToString(encryptedData, Base64.NO_WRAP))
        val decryptedData = AES256GCM.decrypt(encryptedData, key, iv, authTag)

        Assert.assertEquals(data.base64EncodedString(), decryptedData.base64EncodedString())
    }
}
