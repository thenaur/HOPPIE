package com.example.hoppie.data.nearby

/** Nearby can report the peer disconnect before the final SAVED payload's delivery callback. */
internal class FinalReceipt {
    private var pending = false
    private var delivered = false
    private var disconnected: ((Boolean) -> Unit)? = null

    fun reset() {
        check(!pending)
        delivered = false
    }

    fun sending() {
        pending = true
    }

    fun finish(delivered: Boolean) {
        if (!pending) return
        pending = false
        this.delivered = delivered
        val notify = disconnected
        disconnected = null
        notify?.invoke(delivered)
    }

    fun onDisconnect(notify: (Boolean) -> Unit) {
        if (pending) disconnected = notify else notify(delivered)
    }
}
