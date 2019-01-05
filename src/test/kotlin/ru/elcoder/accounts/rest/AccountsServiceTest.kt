package ru.elcoder.accounts.rest

import com.google.inject.Guice
import com.google.inject.Inject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import ru.elcoder.accounts.ApplicationModule
import ru.elcoder.accounts.exceptions.AlreadyClosedAccountsException
import ru.elcoder.accounts.exceptions.NegativeAmountAccountsException
import ru.elcoder.accounts.exceptions.NotEnoughFundsAccountsException
import ru.elcoder.accounts.exceptions.NotFoundAccountsException
import ru.elcoder.accounts.services.ACCOUNT_NUMBER_SIZE

class AccountsServiceTest {

    @Inject
    private lateinit var service: AccountsService

    @Before
    fun setUp() {
        val injector = Guice.createInjector(ApplicationModule())
        injector.injectMembers(this)
    }

    @Test(expected = NotFoundAccountsException::class)
    fun getAccountByNumber_NotExistingAccount() {
        service.getAccountByNumber("456456456")
    }

    @Test
    fun getAccountByNumber_ExistingAccount() {
        val account = service.createAccount(1234567L)
        val account2 = service.getAccountByNumber(account.number!!)
        assertEquals(account.number, account2.number)
        assertEquals(1234567L, account2.balance)
        assertFalse(account2.closed)
    }

    @Test
    fun createAccount_Normal() {
        val account = service.createAccount(2345678L)
        assertEquals(ACCOUNT_NUMBER_SIZE, account.number!!.length)
        assertEquals(2345678L, account.balance)
        assertFalse(account.closed)
    }

    @Test(expected = NegativeAmountAccountsException::class)
    fun createAccount_IllegalAmount() {
        service.createAccount(-2345678L)
    }

    @Test
    fun createAccount_ZeroAmount() {
        service.createAccount(0L)
    }

    @Test
    fun closeAccount_Normal() {
        val account = service.createAccount(3456789L)
        service.closeAccount(account.number!!)
        val account2 = service.getAccountByNumber(account.number!!)
        assertTrue(account2.closed)
    }

    @Test(expected = AlreadyClosedAccountsException::class)
    fun closeAccount_AlreadyClosed() {
        val account = service.createAccount(3456789L)
        service.closeAccount(account.number!!)
        service.closeAccount(account.number!!)
    }

    @Test(expected = NotFoundAccountsException::class)
    fun closeAccount_NotExistingAccount() {
        service.closeAccount("4568756")
    }

    @Test
    fun transfer_Normal() {
        val account1 = service.createAccount(100L)
        val account2 = service.createAccount(200L)
        service.transfer(account1.number!!, account2.number!!, 50L)
        val account12 = service.getAccountByNumber(account1.number!!)
        val account22 = service.getAccountByNumber(account2.number!!)
        assertEquals(50L, account12.balance)
        assertEquals(250L, account22.balance)
    }

    @Test(expected = NotEnoughFundsAccountsException::class)
    fun transfer_TooMuchMoney() {
        val account1 = service.createAccount(100L)
        val account2 = service.createAccount(200L)
        service.transfer(account1.number!!, account2.number!!, 101L)
    }

    @Test(expected = NotFoundAccountsException::class)
    fun transfer_NotExistingSenderAccount() {
        val account = service.createAccount(100L)
        service.transfer("12456656", account.number!!, 10L)
    }

    @Test(expected = NotFoundAccountsException::class)
    fun transfer_NotExistingRecipientAccount() {
        val account = service.createAccount(100L)
        service.transfer(account.number!!, "12456656", 10L)
    }

}