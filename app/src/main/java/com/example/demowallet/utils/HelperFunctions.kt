package com.example.demowallet.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import com.coinbase.wallet.core.util.JSON
import com.coinbase.wallet.crypto.extensions.decryptUsingAES256GCM
import com.coinbase.wallet.crypto.extensions.encryptUsingAES256GCM
import com.example.demowallet.web.Web3ResponseDTO


class HelperFunctions {

    companion object {
        fun getTextInput(tag: String, editText: EditText): String {
            var textInput = editText.text.toString();
            Log.v(tag, textInput);
            return textInput
        }

        fun toastAsync(context: Context, message: String?) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }

        fun encryptData(data: String, secret: String) : String {
            return data.encryptUsingAES256GCM(secret)
        }

        @ExperimentalUnsignedTypes
        fun decryptData(data : String, secret: String): Web3ResponseDTO? {
            val jsonString = data.decryptUsingAES256GCM(secret).toString(Charsets.UTF_8)
            return JSON.fromJsonString(jsonString)
        }
    }

    val helperFunctions: HelperFunctions = HelperFunctions()
}