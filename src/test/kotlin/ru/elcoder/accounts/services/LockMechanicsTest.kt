package ru.elcoder.accounts.services

import com.google.inject.Guice
import com.google.inject.Inject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.slf4j.LoggerFactory
import ru.elcoder.accounts.ApplicationModule
import ru.elcoder.accounts.exceptions.AccountLockedAccountsException
import ru.elcoder.accounts.models.Account
import ru.elcoder.accounts.rest.AccountsService
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

class LockMechanicsTest {

    private val logger = LoggerFactory.getLogger(this.javaClass)!!

    private val repository = object : AccountsRepository() {
        override fun getAccountByNumber(number: String): Account {
            return Account(number = number, balance = 1000L, closed = false)
        }
    }

    @Inject
    private lateinit var service: AccountsService

    @Before
    fun setUp() {
        val injector = Guice.createInjector(ApplicationModule())
        injector.injectMembers(this)
    }


    @Test
    fun testLogic() {
        val locks = ArrayList<ReentrantLock>()
        repository.lockAccounts(arrayOf("1234567", "2345678", "3456789"), DEFAULT_LOCK_TIMEOUT_MSECS, TimeUnit.MILLISECONDS) { accounts ->
            assertEquals(3, repository.locks.size)
            assertTrue(repository.locks.containsKey("1234567"))
            assertTrue(repository.locks.containsKey("2345678"))
            assertTrue(repository.locks.containsKey("3456789"))
            repository.locks.forEach { _: String, lock: Lock ->
                val reentrantLock = lock as ReentrantLock
                assertTrue(reentrantLock.isLocked)
                locks.add(reentrantLock)
            }
            assertEquals(3, accounts.size)
            var prev = 0
            for (account in accounts) {
                val curr = account.number!!.toInt()
                assertTrue(curr > prev)
                prev = curr
            }
        }
        for (lock in locks) {
            assertFalse(lock.isLocked)
        }
    }

    @Test
    fun testConcurrentLogic_Timeout() {
        repository.lockAccounts(arrayOf("1234567"), 10L, TimeUnit.MILLISECONDS) {
            val startAt = System.currentTimeMillis()
            val WAIT_LIMIT = 1000L
            val thread = Thread {
                try {
                    repository.lockAccounts(arrayOf("1234567"), WAIT_LIMIT, TimeUnit.MILLISECONDS) {}
                } catch (e: AccountLockedAccountsException) {
                }
            }
            thread.start()
            thread.join()
            assertTrue((System.currentTimeMillis() - startAt) > WAIT_LIMIT)
        }
    }

    @Test
    fun testConcurrentLogic_Wait() {
        val startAt = System.currentTimeMillis()
        val WAIT_LIMIT = 500L
        val counter = AtomicInteger(0)
        val thread = Thread {
            try {
                repository.lockAccounts(arrayOf("1234567", "2345678"), WAIT_LIMIT * 2, TimeUnit.MILLISECONDS) {
                    counter.incrementAndGet()
                }
            } catch (e: AccountLockedAccountsException) {
            }
        }
        repository.lockAccounts(arrayOf("2345678", "1234567"), 10L, TimeUnit.MILLISECONDS) {
            counter.incrementAndGet()
            thread.start()
            thread.join(WAIT_LIMIT)
        }
        thread.join()
        assertTrue((System.currentTimeMillis() - startAt) > WAIT_LIMIT)
        assertTrue((System.currentTimeMillis() - startAt) < WAIT_LIMIT * 2)
        assertEquals(2, counter.get())
    }

    /**
        Main idea: Each threads generates random transfers,
        calculates accounts balance as if thread was running alone,
        and then generates finishing transfers to restore initial balance on each account.
        After completion of all threads the accounts balances must be equal to its initial values
     */
    @Test
    fun transfer_Concurrent() {
        // configure test case
        val INITIAL_BALANCE = 10_000_000L
        val LOOP_COUNT = 10
        val THREAD_COUNT = 5
        val TRANSFER_AMOUNT = 100L
        assert(INITIAL_BALANCE > THREAD_COUNT * LOOP_COUNT * TRANSFER_AMOUNT)

        // init accounts
        val accounts = ArrayList<Account>(3)
        for (i in 1..3) {
            accounts.add(service.createAccount(INITIAL_BALANCE)) // big enough to do not avoid limits
        }
        // init threads
        val threads = ArrayList<Thread>(THREAD_COUNT)
        val successTransfers = AtomicInteger(0)
        val failedTransfers = AtomicInteger(0)
        for (i in 1 until THREAD_COUNT) {
            threads.add(Thread {
                logger.info("thread start $i")
                val random = ThreadLocalRandom.current()
                val balances = LongArray(accounts.size) { INITIAL_BALANCE }
                for (j in 0 until LOOP_COUNT) {
                    val acc1 = random.nextInt(accounts.size)
                    var acc2 = random.nextInt(accounts.size)
                    while (acc2 == acc1) {
                        acc2 = random.nextInt(accounts.size)
                    }
                    val account1 = accounts[acc1].number!!
                    val account2 = accounts[acc2].number!!
                    val increment = random.nextLong(TRANSFER_AMOUNT) + 1L
                    try {
                        service.transfer(account1, account2, increment)
                        successTransfers.incrementAndGet()
                        balances[acc1] -= increment
                        balances[acc2] += increment
                    } catch (e: AccountLockedAccountsException) {
                        // do nothing
                        logger.warn("thread $i account locked!")
                        failedTransfers.incrementAndGet()
                    }
                }
                // finishing transfers - return accounts balances to its initial state
                // calculate differences and transfer it to accounts
                for (m in 0 until balances.size) {
                    logger.debug("   account: {}, balance: {}", accounts[m].number, balances[m])
                }
                logger.info("  accounts balancing $i")
                for (j in 0 until balances.size - 1) {
                    if (balances[j] != INITIAL_BALANCE) {
                        for (k in j + 1 until balances.size) {
                            if (balances[k] != INITIAL_BALANCE) {
                                val amount = Math.abs(INITIAL_BALANCE - balances[j])
                                if (amount > 0) { // may be == 0
                                    val needDecrement = balances[j] > INITIAL_BALANCE
                                    val acc1 = accounts[if (needDecrement) j else k].number!!
                                    val acc2 = accounts[if (needDecrement) k else j].number!!
                                    service.transfer(acc1, acc2, amount)
                                    balances[if (needDecrement) j else k] -= amount
                                    balances[if (needDecrement) k else j] += amount
                                }
                            }
                        }
                    }
                }
            })
            threads[i - 1].start()
        }
        logger.info("waiting for threads completion")
        for (thread in threads) {
            thread.join()
        }
        logger.info("check asserts")
        accounts.forEach { acc ->
            val account = service.getAccountByNumber(acc.number!!)
            assertEquals(INITIAL_BALANCE, account.balance)
        }

        logger.info("successful transfers: ${successTransfers.get()}, failed transfers: ${failedTransfers.get()}")
        assertTrue(successTransfers.get() > 0)
        assertEquals(0, failedTransfers.get()) // questionable - may fail and it is not an error
    }

}