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
