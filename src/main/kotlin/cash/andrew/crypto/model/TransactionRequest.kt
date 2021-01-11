package cash.andrew.crypto.model

data class TransactionRequest(
    val sender: String,
    val recipient: String,
    val amount: Long
)
