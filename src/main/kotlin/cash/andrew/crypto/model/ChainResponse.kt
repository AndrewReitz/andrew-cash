package cash.andrew.crypto.model

data class ChainResponse(
    val chain: Set<Block>,
    val length: Int
)
