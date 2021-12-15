package com.example.demowallet

import android.util.Log
import android.widget.EditText
import android.widget.TextView
import com.example.demowallet.models.Greeter
import com.example.demowallet.utils.HelperFunctions
import junit.framework.Assert.assertNotNull
import org.junit.Assert
import org.junit.Test
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.infura.InfuraHttpService
import java.lang.Exception
import java.math.BigInteger
import java.util.concurrent.Future

/**
 * Local ReadContract unit test, which will read greeting message from a smart contract
 *
 */
class ReadContractTest {
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
    private val contractAddress = "0x8394cDf176A4A52DA5889f7a99c4f7AD2BF59088"
    private val TAG = "ReadContractTest"

    @Test
    fun can_read_contract() {


        val thread = Thread {
            try {
                val greeter = Greeter.load(contractAddress, web3j, credentials, gasLimit, gasPrice)
                // read from contract
                val greeting: Future<String>? = greeter.greet().sendAsync()
                val convertToString: String? = greeting?.get()
                Log.d(TAG, "greeting value returned: $convertToString")
                assertNotNull(convertToString)
            } catch (e: Exception) {
                Log.d(TAG, "showSmartContract Error: $e.message")
            }
        }

        thread.start()
    }
}