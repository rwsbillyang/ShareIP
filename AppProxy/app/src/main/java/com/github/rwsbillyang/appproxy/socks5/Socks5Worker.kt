package com.github.rwsbillyang.appproxy.socks5

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.github.rwsbillyang.proxy.peer.PeerAsServer

class Socks5Worker(appContext: Context, workerParams: WorkerParameters):
    Worker(appContext, workerParams) {

    override fun doWork(): Result {

        // Do the work here--in this case
        val socks5Server: PeerAsServer = inputData.keyValueMap["socks5Server"] as PeerAsServer
        socks5Server.start()

        // Indicate whether the work finished successfully with the Result
        return Result.success()
    }
}
