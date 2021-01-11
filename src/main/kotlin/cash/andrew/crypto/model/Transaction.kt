package cash.andrew.crypto.model

import java.io.Serializable

data class Transaction(
    val sender: String,
    val recipient: String,
    val amount: Long
) : Serializable {
    val timestamp: Long = System.currentTimeMillis()
}
