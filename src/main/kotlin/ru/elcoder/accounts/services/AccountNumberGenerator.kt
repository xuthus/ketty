package ru.elcoder.accounts.services

import com.google.inject.Singleton
import java.util.concurrent.ThreadLocalRandom


const val ACCOUNT_NUMBER_SIZE = 20

@Singleton
class AccountNumberGenerator {

    fun generate(): String {
        val builder = StringBuilder(ACCOUNT_NUMBER_SIZE)
        for (i in 1..ACCOUNT_NUMBER_SIZE) {
            builder.append(ThreadLocalRandom.current().nextInt(0, 10))
        }
        return builder.toString()
    }
}
