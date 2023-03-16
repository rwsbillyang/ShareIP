package com.github.rwsbillyang.appproxy

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors


abstract class ProgressTask<Params, Progress, Result> {
    enum class Status {
        PENDING, RUNNING, FINISHED
    }
    @Volatile
    var status = Status.PENDING
        private set

    var isCancelled = false
        private set



    private inner class ProgressRunnable @SafeVarargs constructor(vararg params: Params) :
        Runnable {
        val params: Array<Params>
        val handler = Handler(Looper.getMainLooper())

        init {
            this.params = params as Array<Params>
        }

        override fun run() {
            if (status != Status.PENDING) {
                when (status) {
                    Status.RUNNING -> throw IllegalStateException(
                        "Cannot execute task:"
                                + " the task is already running."
                    )
                    Status.FINISHED -> throw IllegalStateException(
                        ("Cannot execute task:"
                                + " the task has already been executed "
                                + "(a task can be executed only once)")
                    )
                    else -> {}
                }
            }
            status = Status.RUNNING
            try {
                onPreExecute()
                val result = doInBackground(*params)
                handler.post {
                    if (!isCancelled) {
                        onPostExecute(result)
                        status = Status.FINISHED
                    } else {
                        onCancelled()
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun execute(vararg params: Params) {
        val executorService = Executors.newSingleThreadExecutor()
        executorService.submit(ProgressRunnable(*params))
    }

    protected open fun onPreExecute() {}
    protected abstract fun doInBackground(vararg params: Params): Result
    protected open fun onPostExecute(result: Result) {}
    fun cancel(flag: Boolean) {
        isCancelled = flag
    }

    protected open fun onCancelled() {}

}
