package ru.elcoder.accounts.services

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountNumberGeneratorTest {

    @Test
    fun generate() {
        val generator = AccountNumberGenerator()
        val accountNumber = generator.generate()
        assertEquals(ACCOUNT_NUMBER_SIZE, accountNumber.length)
        accountNumber.forEach { c ->
            assertTrue(c >= '0')
            assertTrue(c <= '9')
        }
    }
}