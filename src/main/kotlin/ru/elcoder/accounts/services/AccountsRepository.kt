package ru.elcoder.accounts.services

import com.google.inject.Inject
import com.google.inject.Singleton
import org.hibernate.Session
import org.hibernate.SessionFactory
import org.hibernate.Transaction
import org.slf4j.LoggerFactory
import ru.elcoder.accounts.exceptions.AccountLockedAccountsException
import ru.elcoder.accounts.exceptions.AlreadyClosedAccountsException
import ru.elcoder.accounts.exceptions.NotFoundAccountsException
import ru.elcoder.accounts.models.Account
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

internal const val DEFAULT_LOCK_TIMEOUT_MSECS: Long = 1000L

@Singleton
open class AccountsRepository {

    private val logger = LoggerFactory.getLogger(this.javaClass)!!

    @Inject
    private lateinit var accountNumberGenerator: AccountNumberGenerator
    @Inject
    private lateinit var sessionFactory: SessionFactory
    val locks = ConcurrentHashMap<String, Lock>()

    /**
     * a bit overlogged - left after deep debugging
     */
    fun lockAccounts(numbers: Array<String>, waitLimit: Long, timeUnit: TimeUnit, action: (Array<Account>) -> Unit) {
        logger.debug("lockAccounts, numbers = {}", numbers)
        val sortedNumbers = numbers.sorted()
        val accounts = ArrayList<Account>()
        try {
            sortedNumbers.forEach { number ->
                val lock = locks.computeIfAbsent(number) { ReentrantLock() }
                logger.info("lock for account {}, locked: {}", number, (lock as ReentrantLock).isLocked)
                val startAt = System.currentTimeMillis()
                if (!lock.tryLock(waitLimit, timeUnit)) {
                    logger.warn("account locked for more than ${System.currentTimeMillis() - startAt} msecs")
                    throw AccountLockedAccountsException(number)
                }
                logger.info("account {} locked", number)
                try {
                    accounts.add(getAccountByNumber(number))
                } catch (e: NotFoundAccountsException) {
                    lock.unlock()
                    throw e
                }
            }
            action.invoke(accounts.toArray(arrayOf<Account>()))
        } finally {
            logger.info("unlock accounts, accounts: {}, reversed: {}", accounts.size, accounts.reversed().size)
            for (account in accounts.reversed()) {
                val lock = locks[account.number]
                if (lock != null) {
                    lock.unlock()
                    logger.info("account {} unlocked", account.number)
                }
            }
        }
    }

    fun createAccount(initialAmount: Long): Account {
        while (true) {
            val number = accountNumberGenerator.generate()
            try {
                lockAccounts(arrayOf(number), 1L, TimeUnit.NANOSECONDS) { }
            } catch (e: NotFoundAccountsException) {
                val account = Account(number = number)
                account.balance = initialAmount
                saveAccounts(account)
                return account
            }
        }
    }

    fun closeAccount(number: String) {
        lockAccounts(arrayOf(number), DEFAULT_LOCK_TIMEOUT_MSECS, TimeUnit.MILLISECONDS) { accounts ->
            val account = accounts[0]
            if (account.closed) {
                throw AlreadyClosedAccountsException(number)
            }
            account.closed = true
            saveAccounts(*accounts)
        }
    }

    open fun getAccountByNumber(number: String): Account {
        return sessionFactory.withSession { session ->
            session.createQuery("from Account where number = :number")
                    .setParameter("number", number)
                    .uniqueResultOptional()
                    .orElseThrow {
                        NotFoundAccountsException(number)
                    } as Account
        }
    }

    fun saveAccounts(vararg accounts: Account) {
        sessionFactory.withSession { session ->
            session.withTransaction {
                for (account in accounts) {
                    session.saveOrUpdate(account)
                }
            }
        }
    }

}

private fun <T> Session.withTransaction(consumer: (Transaction) -> T): T {
    transaction.begin()
    try {
        val result = consumer(transaction)
        transaction.commit()
        return result
    } catch (e: Exception) {
        transaction.rollback()
        throw e
    }
}

private fun <T> SessionFactory.withSession(consumer: (Session) -> T): T {
    this.openSession().use { session ->
        return consumer(session)
    }
}
