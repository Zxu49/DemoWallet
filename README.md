# DemoWallet

This repo is sample app for Wallet Link SDK. 

WalletLink is an open protocol that lets users connect their mobile wallets to your DApp.

With the WalletLink SDK, your mobile wallet will be able to interact with DApps on the desktop and be able to sign web3 transactions and messages.

For more info check [SDK](https://github.com/Zxu49/walletlink-mobile-sdk/tree/master/android)

The origin [repo](https://github.com/walletlink/walletlink-mobile-sdk)

## Usage

### Wallet Side

1. Monitoring the network and Init the walletlink instance

```
    val intentFilter = IntentFilter().apply { addAction(ConnectivityManager.CONNECTIVITY_ACTION) }
    this.registerReceiver(Internet, intentFilter)
    Internet.startMonitoring()
```

```
    val walletLink = WalletLink(notificationUrl, context)
```

2. Link to bridge server, the session id and secret come from Dapp will generate a dapp instance and be subscribed on walletlink instance.

```kotlin
    // To pair the device with a browser after scanning WalletLink QR code
    walletLink.link(
        sessionId = sessionId,
        secret = secret,
        url = serverUrl,
        userId = userId,
        metadata = mapOf(ClientMetadataKey.EthereumAddress to wallet.primaryAddress)
    )
        .subscribeBy(onSuccess = {
    // New WalletLink connection was established
        }, onError = { error ->
    // Error while connecting to WalletLink server (walletlinkd)
        })
        .addTo(disposeBag)
```

3. Listen on incoming requests from the subscribed Dapp

```kotlin
    /*
    Listen on incoming requests, it should be requestsObservable not requests,
    the origin sdk readme is incorrect
    */
    walletLink.requestsObservable
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeBy(onNext = { request ->
    // New unseen request
        })
        .addTo(disposeBag)
```

4. The function is not provided in original branch

```kotlin
    // Approve DApp permission request (EIP-1102)
    walletLink.approveDappPermission(request.hostRequestId)
        .subscribeBy(onSuccess = {
    // Dapp received EIP-1102 approval
        }, onError = { error ->
    // Dapp failed to receive EIP-1102 approval
        })
        .addTo(disposeBag)
```

5. Approve or reject the request come from Dapp

```kotlin
    // Approve a given transaction/message signing request
    walletLink.approve(request.hostRequestId, signedData)
        .subscribeBy(onSuccess = {
    // Dapp received request approval
        }, onError = { error ->
    // Dapp failed to receive request approval
        })
        .addTo(disposeBag)


    // Reject transaction/message/EIP-1102 request
    walletLink.reject(request.hostRequestId)
        .subscribeBy(onSuccess = {
    // Dapp received request rejection
        }, onError = { error ->
    // Dapp failed to receive request rejection
        })
        .addTo(disposeBag)
```

## Use Cases and User Interface Flow
Users may expect to use our library if they want to build a mobile Dapp

1. To connect to the wallet, dapp users can click the ‘start connection’ button. Then it will generate a QR code with the Session ID and Session Key parameters (Figure 3.3.1)

     ![Figure 3.3.1](readme_images/figure_3.3.1.PNG)
    
2. Wallet users can click the ‘Connect to app’ button to scan the QR code(Figure 3.3.2)

    ![Figure 3.3.2](readme_images/figure_3.3.2.PNG) 

3. After that, the wallet user can click ‘link’ in the new pop-up to send a connection request to Dapp (Figure 3.3.3)

    ![Figure 3.3.3](readme_images/figure_3.3.3.PNG)
   
4. When the Dapp receives the connection request, it will send a new permission request to the wallet again (Figure 3.3.4)

    ![Figure 3.3.4](readme_images/figure_3.3.4.PNG)
   
5. The wallet user can select ‘approve’ in the incoming dialog to connect to dapp and dapp will show the fetched wallet address (Figure 3.3.5)

    ![Figure 3.3.5](readme_images/figure_3.3.5.PNG)
   
6. The dapp right now can send encrypted signed personal data string to the wallet through the bridge server, the wallet will decrypt the string and the user can choose whether to Approve or Reject it (Figure 3.3.6)

    ![Figure 3.3.6](readme_images/figure_3.3.6.PNG)
   
7. Once the wallet approves or rejects the payload requests, a notification message will send back to the dapp (Figure 3.3.7)

    ![Figure 3.3.7](readme_images/figure_3.3.7.PNG)

8. The ‘sign typed data’ performs similar functionalities except that it sends encrypted structural data instead of the data string. When dapp users click the ‘sign typed data’ button, a form dialog will pop out and users can customize the parameters (Figure 3.3.8)

    ![Figure 3.3.8](readme_images/figure_3.3.8.PNG)
   
9. Then dapp will send the signed transaction data from the last step, along with the blockchain network information to the wallet and the wallet can process the transaction (Figure 3.3.9)

    ![Figure 3.3.9](readme_images/figure_3.3.9.PNG)
    
10. The dapp can send a greeting message to a smart contract on the Rinkeby testnet according to the contract address the user input. Then the testnet will send a status back indicating whether the transaction is successful, and the gas used (Figure 3.3.10)

    ![Figure 3.3.10](readme_images/figure_3.3.10.PNG)
    
11. The wallet users can click ‘Read Smart Contract’ to read the greeting message of the smart contract by the contract’s address (Figure 3.3.11)

    ![Figure 3.3.11](readme_images/figure_3.3.11.PNG)
    
12. Lastly, the dapp user can click the “Cancel” button to send a disconnection request to the wallet to terminate the connection with the wallet (Figure 3.3.12)

    ![Figure 3.3.12](readme_images/figure_3.3.12.PNG)
    
13. A new dialog will pop out on the wallet side and the user can select ok in the dialog and click the ‘Unlink’ button to disconnect with dapp (Figure 3.3.13)

    ![Figure 3.3.13](readme_images/figure_3.3.13.PNG)

### For the Coinbase Wallet users:

14. Coinbase Wallet users can click the ‘start coinbase’ button on the dapp and it will pop a new QR code that allows Coinbase Wallet to scan for connecting with it (Figure 3.3.14)

    ![Figure 3.3.14](readme_images/figure_3.3.14.PNG)
    
15. Users of coinbase wallet can scan the QR code and connect with our dapp and dapp will also show the wallet address it fetched (Figure 3.3.15)

    ![Figure 3.3.15](readme_images/figure_3.3.15.PNG)
    
16. Then we can send a new Sign Personal data message to the coinbase wallet. When the payload is sent to the coinbase wallet successfully, a new signature request prompt will pop out in the Coinbase wallet and users can sign this message (Figure 3.3.16)

    ![Figure 3.3.16](readme_images/figure_3.3.16.PNG)
    
17. After sending the sign personal data, users can send sign typed data to coinbase. After receiving the sign typed data successfully and the Confirm Payment prompt pops out, the user can click the confirm button to confirm this sign typed data payment (Figure 3.3.17)

    ![Figure 3.3.17](readme_images/figure_3.3.17.PNG)
