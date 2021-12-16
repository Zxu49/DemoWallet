package com.example.demowallet

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.coinbase.wallet.core.extensions.asHexEncodedData
import com.coinbase.wallet.core.extensions.unwrap
import com.coinbase.wallet.core.util.JSON
import com.coinbase.wallet.core.util.Optional
import com.coinbase.wallet.crypto.extensions.decryptUsingAES256GCM
import com.coinbase.wallet.crypto.extensions.encryptUsingAES256GCM
import com.coinbase.wallet.http.connectivity.Internet
import com.coinbase.walletlink.WalletLink
import com.coinbase.walletlink.exceptions.WalletLinkException
import com.coinbase.walletlink.models.*
import com.example.demowallet.dialogs.ReadContractDialog
import com.example.demowallet.utils.Authorization
import com.example.demowallet.utils.HelperFunctions
import com.example.demowallet.utils.ScanResult
import com.example.demowallet.web.Web3ResponseDTO
import io.github.g00fy2.quickie.QRResult
import io.github.g00fy2.quickie.ScanQRCode
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

@SuppressLint("StaticFieldLeak")
private var readContractbuilder: ReadContractDialog.Builder? = null
private var readContractDialog: ReadContractDialog? = null

class MainActivity : AppCompatActivity() {
    // Call back url
    private val notificationUrl = URL("https://walletlink.herokuapp.com")

    // Hardcode Const val for test related parameters
    private val testAddress = "0x03F6f282373900C2F6CE53B5A9f595b92aC5f5E5"
    private val testServerURL = URL("https://www.walletlink.org")
    private val testSessionID = "25836791149028100067249263733745"
    private val testSecret = "8247670093311641772795663185614455254637038084190153387156342357"
    private val testUserId = "123"
    private val testVersion = "1.1"
    private var ADDRESS: String? = "0x03F6f282373900C2F6CE53B5A9f595b92aC5f5E5"

    // Object that GC all observable convert into destroyable once it finish event
    private var disposeBag = CompositeDisposable()

    // Frontend related parameters
    private var root_layout: LinearLayout? = null
    private var btn: Button? = null
    private var unlinkBtn: Button? = null
    private var testBtn: Button? = null
    private var sendTradingbtn: Button? = null
    private var addressField: TextView? = null
    private var messageToEth : String? = null

    // Wallet related parameters
    private var walletLink : WalletLink? = null
    private var session : Session? = null
    private var dapp : Dapp? = null

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val scanQrCode = (this as ComponentActivity).registerForActivityResult(ScanQRCode(), ::handleResult)

        addressField = findViewById<View>(R.id.addressTitle) as TextView
        addressField!!.setText("Public Address: $ADDRESS")
        initWalletLink(notificationUrl)

        btn = findViewById<View>(R.id.btn) as Button
        unlinkBtn = findViewById<View>(R.id.unlink) as Button
        testBtn = findViewById<View>(R.id.test) as Button
        unlinkBtn!!.visibility = View.INVISIBLE
        btn!!.setOnClickListener {
            scanQrCode.launch(null)
            listenRequestsFromSocket()
        }
        unlinkBtn!!.setOnClickListener{
            session?.let { it1 -> walletLink!!.unlink(it1) }
            walletLink!!.disconnect()
            toastAsync("Disconnect the DApp")
            btn!!.visibility = View.VISIBLE
            testBtn!!.visibility = View.VISIBLE
            unlinkBtn!!.visibility = View.INVISIBLE
        }
        testBtn!!.setOnClickListener{
            unlinkBtn!!.visibility = View.VISIBLE
            testBtn!!.visibility = View.INVISIBLE
            linkToServer()
            listenRequestsFromSocket()
        }

        readContractbuilder = ReadContractDialog.Builder(this)
        sendTradingbtn = findViewById<View>(R.id.read_smart_contract) as Button
        // Set a click listener for button widget
        sendTradingbtn!!.setOnClickListener {
            showReadContractDialog(){
                readContractDialog!!.dismiss()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        session?.let { walletLink?.unlink(it) }
        walletLink?.disconnect()
    }

    /**
     *  Init wallet-link instance - should be singleton
     */
    private fun initWalletLink(notificationUrl : URL) {
        val intentFilter = IntentFilter().apply { addAction(ConnectivityManager.CONNECTIVITY_ACTION) }
        this.registerReceiver(Internet, intentFilter)
        Internet.startMonitoring()

        walletLink = WalletLink(
            notificationUrl = notificationUrl,
            context = this
        )
    }

    /**
     *  Helper function for handle host request from Dapp generated QR code
     */
    private fun handleResult(result: QRResult) {
        val alertDialogBuilder = AlertDialog.Builder(this)
        var message = "Click the link means you allow this dapp access your public key"
        alertDialogBuilder.setTitle("Connect this site")

        when (result) {
            is QRResult.QRSuccess -> {
                alertDialogBuilder.setPositiveButton("Link") { dialog, which ->
                    println("result.content.rawValue : ${result.content.rawValue}")
                    linkToServer(ScanResult(result.content.rawValue))
                    toastAsync("Connect Success")
                    btn!!.visibility = View.INVISIBLE
                    unlinkBtn!!.visibility = View.VISIBLE
                }
            }
            is QRResult.QRUserCanceled -> message = "User canceled scanning"
            is QRResult.QRMissingPermission -> message = "Missing permission"
            is QRResult.QRError -> message = "${result.exception.javaClass.simpleName}: ${result.exception.localizedMessage}"
        }
        alertDialogBuilder.setMessage(message)
        alertDialogBuilder.setNegativeButton("Cancel") { dialog, which ->
            toastAsync("Connect cancel")
        }
        alertDialogBuilder.show()
    }

    /**
     *  Helper function for crypto wallet link to walletlink server
     */
    private fun linkToServer(scanResult: ScanResult) {
        // Generated from hardcode
        val authObject = Authorization(scanResult.sessionId, scanResult.secret)
        session = Session(id = authObject.sessionId, secret = authObject.secret, url = scanResult.url)

        println(
            "\nSessionId  : ${authObject.sessionId}" +
                    "\nSecret     : ${authObject.secret}" +
                    "\nSessionKey : ${authObject.sessionKey}" +
                    "\nAuth       : ${authObject.basicAuth}"
        )
        try {
            link(authObject, scanResult.userId, scanResult.url)
        } catch (e: WalletLinkException){
            toastAsync(e.message)
        }
    }

    /**
     * Hardcode function for crypto wallet link to walletlink server
     */
    private fun linkToServer() {
        // Generated from DApp
        val authObject = Authorization(testSessionID, testSecret)
        session = Session(id = authObject.sessionId, secret = authObject.secret, url = testServerURL)

        println(
            "\nSessionId  : ${authObject.sessionId}" +
                    "\nSecret     : ${authObject.secret}" +
                    "\nSessionKey : ${authObject.sessionKey}" +
                    "\nAuth       : ${authObject.basicAuth}"
        )

        try {
            link(authObject, testUserId, testServerURL)
        } catch (e: WalletLinkException){
            toastAsync(e.message)
        }
    }

    /**
     *  Helper function for crypto link to walletlink server
     */
    private fun link(authObject : Authorization, userId : String, url : URL) {
        walletLink!!.link(
            sessionId = authObject.sessionId,
            secret = authObject.secret,
            version = testVersion,
            url = url,
            userId = userId,
            metadata = mapOf(ClientMetadataKey.EthereumAddress to testAddress)
        ).subscribeBy(onSuccess = {
            toastAsync("New WalletLink connection was established, Waiting for incoming requests")
        }, onError = {
            toastAsync(it.message)
        }).addTo(disposeBag)
    }

    /**
     *  Approve a given transaction/message signing request
     */
    private fun approveRequest(hostRequestId : HostRequestId, signedData : ByteArray) {
        walletLink!!.approve(hostRequestId, signedData)
            .subscribeBy(onSuccess = {
                print("Received request approval")
            }, onError = { error ->
                error("Failed to receive request approval")
            })
            .addTo(disposeBag)
    }

    // Reject transaction/message/EIP-1102 request
    private fun rejectRequest(hostRequestId : HostRequestId) {
        walletLink!!.reject(hostRequestId)
            .subscribeBy(onSuccess = {
                print("Reject request approval")
            }, onError = { error ->
                print("Failed to receive request approval")
            })
            .addTo(disposeBag)
    }

    private fun connectWalletLink() {
        val metadata : ConcurrentHashMap<ClientMetadataKey, String> = ConcurrentHashMap()
        metadata[ClientMetadataKey.EthereumAddress] = testAddress

        walletLink!!.connect(
            userId = testUserId,
            metadata = metadata
        )
    }


    /**
     * Web3 functions
     */

    /**
     * Build ReadContract Dialog from ReadContract Dialog Builer and set up
     * relevant parameters and listeners. Then show it to the current activity
     *
     * params
     *  onClickListener - OnClickListener that will close the sendTransaction Dialog
     */
    @RequiresApi(Build.VERSION_CODES.N)
    private fun showReadContractDialog(onClickListener:View.OnClickListener) {
        readContractDialog = readContractbuilder!!
            .setCloseButton(onClickListener)
            .createDialog()
        readContractDialog!!.show()
    }


    private fun toastAsync(message: String?) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Helper function for listen dapp request from wallet link server
     */
    private fun listenRequestsFromSocket() {
        var requestTitle : String? = null
        var requestContext : String? = null
        val signData = "[\"0x03F6f282373900C2F6CE53B5A9f595b92aC5f5E5\"]".toByteArray(Charsets.UTF_8)
        var dialogBuilder = AlertDialog.Builder(this)

        walletLink!!.requestsObservable
            .observeOn(AndroidSchedulers.mainThread())
            .map { Optional(it) }
            .onErrorReturn { Optional(null) }
            .unwrap()
            .subscribe { request ->
                when (request) {
                    is HostRequest.DappPermission -> {
                        requestTitle = "A Dapp Permission coming"
                        requestContext = "${request.dappName} want to connect your wallet"
                    }

                    is HostRequest.SignMessage -> {
                        requestTitle = "Message from ${request.dappName}"
                        requestContext = request.message
                        messageToEth = request.message
                    }

                    is HostRequest.SignAndSubmitTx -> {
                        requestTitle = "Sign the Transaction on ${request.dappName}?"
                        requestContext =
                            "Detail of Transaction\n" +
                                    "from: ${request.fromAddress},\n" +
                                    "to: ${request.toAddress},\n" +
                                    "data: ${request.data.toString(Charsets.UTF_8)},\n" +
                                    "gasPrice: ${request.gasPrice},\n" +
                                    "gasLimit: ${request.gasLimit},\n" +
                                    "chainId : ${request.chainId} \n"
                    }

                    is HostRequest.SubmitSignedTx -> {
                        requestTitle = "Submit the Transaction on ${request.dappName}?"
                        requestContext =
                            "tx : ${request.signedTx.toString(Charsets.UTF_8)} \n" +
                                    "chainId : ${request.chainId} \n"
                    }

                    is HostRequest.RequestCanceled -> {
                        requestTitle = "Cancel the Transaction on ${request.dappName}?"
                        requestContext = "Transaction Request Cancel"
                    }
                }

                dialogBuilder
                    .setTitle(requestTitle)
                    .setMessage(requestContext)

                AlertDialog.Builder(this)
                    .setTitle(requestTitle)
                    .setMessage(requestContext)
                if (request is HostRequest.RequestCanceled) {
                    dialogBuilder
                        .setPositiveButton("OK"){ dialog, which ->
                            walletLink!!.markAsSeen(requestIds = listOf(request.hostRequestId))
                                .subscribeBy(onSuccess = {
                                    toastAsync("The request had been canceled")
                                }, onError = {
                                    toastAsync("The request fail to be canceled")
                                }).addTo(disposeBag)
                        }.setNegativeButton("Don't cancel") { dialog, which ->
                            rejectRequest(request.hostRequestId)
                        }
                } else {
                    dialogBuilder
                        .setPositiveButton("Approve") { dialog, which ->
                            approveRequest(request.hostRequestId, signData!!)
                        }.setNegativeButton("Reject") { dialog, which ->
                            rejectRequest(request.hostRequestId)
                        }
                }
                dialogBuilder.show()
            }
            .addTo(disposeBag)
    }
}

