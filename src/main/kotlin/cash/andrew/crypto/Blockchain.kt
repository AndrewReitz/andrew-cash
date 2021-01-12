package cash.andrew.crypto

import cash.andrew.crypto.model.Block
import cash.andrew.crypto.model.ChainResponse
import cash.andrew.crypto.model.Transaction
import io.micronaut.core.type.Argument
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.RxHttpClient
import okio.ByteString.Companion.toByteString
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import javax.inject.Singleton

@Singleton
open class Blockchain(
    private val httpClient: RxHttpClient
) {
    private val logger = LoggerFactory.getLogger(Blockchain::class.java)

    val chain = linkedSetOf<Block>()
    val lastBlock: Block get() = chain.last()

    val nodes = linkedSetOf<String>()

    private val currentTransactions = linkedSetOf<Transaction>()

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
     * Add a new node to the list of nodes.
     */
    fun registerNode(address: String) {
        nodes.add(address)
    }

    /**
     * Determines if a given blockchain is valid.
     */
    fun validChain(chain: Set<Block>): Boolean {
        var lastBlock = chain.first()
        chain.drop(1).forEach { block ->
            if (block.previousHash != hash(lastBlock)) return false
            if (validProof(lastBlock.proof, block.proof)) return false

            lastBlock = block
        }

        return true
    }

    /**
     * Resolve any conflicts in our chain with the longest on the network.
     * @return true if our chain was replaced false if not.
     */
    fun resolveConflicts(): Boolean {
        val neighbours = nodes

        val maxLength = chain.size
        var newChain: Set<Block>? = null

        neighbours.forEach {
            runCatching {
                val response = httpClient.retrieve(
                    HttpRequest.GET<Any>("$it/chain"),
                    Argument.of(ChainResponse::class.java)
                ).singleOrError()
                    .blockingGet()

                if (response.length > maxLength && validChain(response.chain)) {
                    chain.clear()
                    chain.addAll(response.chain)
                    newChain = response.chain
                }
            }.onFailure { e ->
                logger.error("Error connecting to node $it", e)
            }
        }

        val nc = newChain
        if (nc != null) {
            chain.clear()
            chain.addAll(nc)
            return true
        }

        return false
    }

    /**
     * Validate the proof of work.
     */
    private fun validProof(lastProof: Long, proof: Long): Boolean {
        return "$lastProof$proof".toByteArray().toByteString().sha512().hex().take(4) == "0000"
    }
}
