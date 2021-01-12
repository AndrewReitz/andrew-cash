package cash.andrew.crypto

import cash.andrew.crypto.model.ChainResponse
import cash.andrew.crypto.model.RegisterNodeRequest
import cash.andrew.crypto.model.TransactionRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Produces
import java.util.UUID
import javax.inject.Inject

private val NODE_IDENTIFIER = UUID.randomUUID().toString()

@Controller("/")
class BlockchainController @Inject constructor(
    private val blockchain: Blockchain
) {

    @Post("/transactions/new")
    @Produces(MediaType.APPLICATION_JSON)
    suspend fun newTransaction(transaction: TransactionRequest): HttpResponse<Map<String, String>> {
        val index = blockchain.newTransaction(
            sender = transaction.sender,
            recipient = transaction.recipient,
            amount = transaction.amount
        )

        return HttpResponse.created(
            mapOf(
                "message" to "Transaction will be added to block $index"
            )
        )
    }

    @Get("/mine")
    @Produces(MediaType.APPLICATION_JSON)
    suspend fun mine(): HttpResponse<Map<String, Any>>? {
        val lastBlock = blockchain.lastBlock
        val lastProof = lastBlock.proof

        val proof = blockchain.proofOfWork(lastProof)

        // We must receive a reward for finding the proof.
        // The sender is "0" to signify that this node has mined a new coin.
        blockchain.newTransaction(
            sender = "0",
            recipient = NODE_IDENTIFIER,
            amount = 1,
        )

        val previousHash = blockchain.hash(lastBlock)
        val block = blockchain.newBlock(
            proof = proof,
            previousHash = previousHash
        )

        return HttpResponse.ok(
            mapOf(
                "message" to "New Block Forged",
                "index" to block.index,
                "transaction" to block.transactions,
                "proof" to block.proof,
                "previousHash" to block.previousHash
            )
        )
    }

    @Get("/chain")
    @Produces(MediaType.APPLICATION_JSON)
    suspend fun chain(): HttpResponse<ChainResponse> {
        return HttpResponse.ok(
            ChainResponse(
                chain = blockchain.chain,
                length = blockchain.chain.size
            )
        )
    }

    @Post("/nodes/register")
    @Produces(MediaType.APPLICATION_JSON)
    suspend fun register(request: RegisterNodeRequest): HttpResponse<Any> {
        if (request.nodes.isEmpty()) {
            return HttpResponse.badRequest("Error: Please supply a valid list of nodes")
        }

        request.nodes.forEach {
            blockchain.registerNode(it)
        }

        return HttpResponse.ok(
            mapOf(
                "message" to "New nodes added",
                "total_nodes" to blockchain.nodes.size
            )
        )
    }

    @Get("/nodes/resolve")
    @Produces(MediaType.APPLICATION_JSON)
    suspend fun consensus(): HttpResponse<Map<String, Any>> {
        val replaced = blockchain.resolveConflicts()

        return HttpResponse.ok(
            mapOf(
                "message" to if (replaced) "Our chain was replaced" else "Our chain is authoritative",
                "chain" to blockchain.chain
            )
        )
    }
}
