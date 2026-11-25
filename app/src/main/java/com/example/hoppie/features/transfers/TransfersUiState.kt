package com.example.hoppie.features.transfers

internal enum class TransferStatusFilter(val label: String, val emptyMessage: String) {
    Ongoing("Ongoing", "No ongoing transfers."),
    Completed("Completed", "No completed transfers yet."),
    Failed("Failed", "No failed transfers.")
}
