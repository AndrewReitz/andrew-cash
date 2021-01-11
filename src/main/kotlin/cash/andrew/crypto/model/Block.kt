package cash.andrew.crypto.model

import java.io.Serializable

data class Block(
    val index: Long,
    val timestamp: Long,
    val transactions: Set<Transaction>,
    val proof: Long,
    val previousHash: String
) : Serializable

