package cash.andrew.crypto

import cash.andrew.crypto.model.Block
import cash.andrew.crypto.model.Transaction
import okio.ByteString.Companion.toByteString
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import javax.inject.Singleton

@Singleton
open class Blockchain {
    val chain = linkedSetOf<Block>()
    private val currentTransactions = linkedSetOf<Transaction>()

    val lastBlock: Block get() = chain.last()

    init {
        // create the genesis block
        newBlock(previousHash = "", proof = 100)
    }

    /**
     * creates a new block in the BlockChain
     * @param previousHash the hash of the previous block in the blockchain
     * @param proof the proof given by the Proff of Work algorithm
     */
    fun newBlock(previousHash: String, proof: Long): Block {
        val newBlock = Block(
            index = chain.size + 1L,
            timestamp = System.currentTimeMillis(),
            proof = proof,
            previousHash = previousHash,
            transactions = currentTransactions
        )

        chain.add(newBlock)
        return newBlock
    }

    /**
     * Creates a new transaction to go into the next mined Block.
     *
     * @param sender the address of the sender
     * @param recipient the address of the Recipient of the new transaction
     * @param amount the amount the sender will sent to the recipient
     */
    fun newTransaction(sender: String, recipient: String, amount: Long): Long {
        currentTransactions.add(
            Transaction(
                sender = sender,
                recipient = recipient,
                amount = amount
            )
        )

        return lastBlock.index + 1L
    }

    /**
     * Create a sha-512 hash of the given block
     *
     * @param block the block to hash
     * @return the hashed value of the block
     */
    fun hash(block: Block): String = ByteArrayOutputStream().use { byteArrayStream ->
        ObjectOutputStream(byteArrayStream).use { objectStream ->
            objectStream.writeObject(block)
        }
        byteArrayStream.toByteArray().toByteString().sha512().hex()
    }

    /**
     * Simple PoW algo.
     * Find a number p such that hash(pp) contains leading 4 zeros, where p is the previous p.
     * p is the previous proof, and p is the new proof
     */
    fun proofOfWork(lastProof: Long): Long {
        var proof = 0L
        while (!validProof(lastProof = lastProof, proof = proof)) {
            proof++
        }

        return proof
    }

    /**
     * Validate the proof of work.
     */
    private fun validProof(lastProof: Long, proof: Long): Boolean {
        return "$lastProof$proof".toByteArray().toByteString().sha512().hex().take(4) == "0000"
    }
}
