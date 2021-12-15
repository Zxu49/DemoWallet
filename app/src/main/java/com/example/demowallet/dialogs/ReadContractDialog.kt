package com.example.demowallet.dialogs

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.example.demowallet.R
import com.example.demowallet.models.Greeter
import com.example.demowallet.utils.HelperFunctions
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.infura.InfuraHttpService
import java.lang.Exception
import java.math.BigInteger
import java.util.concurrent.Future

// contract address
// endpoint url provided by infura
private val url = "https://rinkeby.infura.io/v3/01eb8f7b5e514832af8e827c23784d23"
// web3j infura instance
private val web3j = Web3j.build(InfuraHttpService(url))
// gas limit
private val gasLimit: BigInteger = BigInteger.valueOf(20_000_000_000L)
// gas price
private val gasPrice: BigInteger = BigInteger.valueOf(4300000)
// create credentials w/ your private key
private val credentials = Credentials.create("f9319fe162c31947c0ca8fd649a536b7ca311b5f210afdc48b62fd7d18ce53e4")

class ReadContractDialog(context: Context) : Dialog(context) {


    @SuppressLint("InflateParams")
    class Builder(context: Context) {
        private val TAG = "MyActivity ReadContractDialog"

        private var contentView: View? = null
        private var image:Bitmap? = null
        private var singleButtonText: String? = null
        private var closeButtonClickListener: View.OnClickListener? = null
        private var readButtonClickListener: View.OnClickListener? = null

        private val layout: View
        private val dialog: ReadContractDialog = ReadContractDialog(context)

        init {
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            inflater.inflate(R.layout.read_contract_view, null).also { layout = it }
            dialog.addContentView(layout, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }

        fun setImage(bm : Bitmap ): Builder {
            this.image = bm
            return this
        }

        fun setCloseButton(listener: View.OnClickListener): Builder {
            this.closeButtonClickListener = listener
            return this
        }

        fun setSendButton(listener: View.OnClickListener): Builder {
            this.readButtonClickListener = listener
            return this
        }

        @RequiresApi(Build.VERSION_CODES.N)
        fun createDialog(): ReadContractDialog {
            showButtons()
            setSendButton {
                readSmartContract()
            }
            layout.findViewById<View>(R.id.send_trading_close).setOnClickListener(closeButtonClickListener)
            layout.findViewById<View>(R.id.send_ether_button).setOnClickListener(readButtonClickListener)

            create()
            return dialog
        }

        private fun create() {
            if (contentView != null) {
                (layout.findViewById<View>(R.id.content) as LinearLayout).removeAllViews()
                (layout.findViewById<View>(R.id.content) as LinearLayout)
                    .addView(contentView, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            }
            dialog.setContentView(layout)
            dialog.setCancelable(true)
            dialog.setCanceledOnTouchOutside(false)
        }

        private fun showButtons() {
            layout.findViewById<View>(R.id.twoButtonLayout).visibility = View.VISIBLE
        }

        @RequiresApi(Build.VERSION_CODES.N)
        fun readSmartContract() {
            val contractAddress = HelperFunctions.getTextInput("$TAG contractAddress",
                layout.findViewById<EditText>(R.id.address_input))

            val thread = Thread {
                try {
                    val greeter = Greeter.load(contractAddress, web3j, credentials, gasLimit, gasPrice)

                    // read from contract
                    val greeting: Future<String>? = greeter.greet().sendAsync()
                    val convertToString: String? = greeting?.get()
                    layout.findViewById<TextView>(R.id.contract_message).text = convertToString
                    Log.d(TAG, "greeting value returned: $convertToString")
                } catch (e: Exception) {
                    Log.d(TAG, "showSmartContract Error: $e.message")
                }
            }

            thread.start()
        }
    }
}