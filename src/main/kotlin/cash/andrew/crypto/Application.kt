package cash.andrew.crypto

import io.micronaut.runtime.Micronaut.*

fun main(args: Array<String>) {
	build()
	    .args(*args)
		.packages("cash.andrew.crypto")
		.start()
}

