package ru.elcoder.accounts.rest

import com.google.inject.Inject
import com.google.inject.Singleton
import org.slf4j.LoggerFactory
import ru.elcoder.accounts.exceptions.AccountClosedAccountsException
import ru.elcoder.accounts.exceptions.NegativeAmountAccountsException
import ru.elcoder.accounts.exceptions.NotEnoughFundsAccountsException
import ru.elcoder.accounts.models.Account
import ru.elcoder.accounts.services.AccountsRepository
import ru.elcoder.accounts.services.DEFAULT_LOCK_TIMEOUT_MSECS
import java.util.concurrent.TimeUnit
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.ext.Provider


@Singleton
@Provider
@Path("/accounts")
class AccountsService {

    private val logger = LoggerFactory.getLogger(this.javaClass)!!

    @Inject
    private lateinit var accountsRepository: AccountsRepository

    @GET
    @Path("/{number}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getAccountByNumber(@PathParam("number") number: String): Account {
        logger.info("getAccountByNumber, number = {}", number)
        return accountsRepository.getAccountByNumber(number)
    }

    @PUT
    @Path("/create/{initialAmount}")
    @Produces(MediaType.APPLICATION_JSON)
    fun createAccount(@PathParam("initialAmount") initialAmount: Long): Account {
        logger.info("createAccount, initialAmount = {}", initialAmount)
        validateAmount(initialAmount, 0)
        return accountsRepository.createAccount(initialAmount)
    }

    @POST
    @Path("/close/{number}")
    @Produces(MediaType.APPLICATION_JSON)
    fun closeAccount(@PathParam("number") number: String) {
        logger.info("closeAccount, number = {}", number)
        accountsRepository.closeAccount(number)
    }

    @POST
    @Path("/transfer/{number1}/to/{number2}/{amount}")
    @Produces(MediaType.APPLICATION_JSON)
    fun transfer(
            @PathParam("number1") number1: String,
            @PathParam("number2") number2: String,
            @PathParam("amount") amount: Long) {
        logger.info("transfer, from {}, to {}, amount {}", number1, number2, amount)
        validateAmount(amount)
        accountsRepository.lockAccounts(arrayOf(number1, number2), DEFAULT_LOCK_TIMEOUT_MSECS, TimeUnit.MILLISECONDS) { accounts ->
            for (account in accounts) {
                if (account.closed) {
                    throw AccountClosedAccountsException(account.number!!)
                }
            }
            val source = accounts[if (accounts[0].number == number1) 0 else 1]
            val destination = accounts[if (accounts[1].number == number2) 1 else 0]
            assert(source.number != destination.number)
            if (source.balance < amount) {
                throw NotEnoughFundsAccountsException(source.number!!, amount)
            }
            logger.debug("transfer, source: {}, destination: {}", source.number, destination.number)
            source.balance -= amount
            destination.balance += amount
            accountsRepository.saveAccounts(*accounts)
        }
    }

    private fun validateAmount(amount: Long, minimalAmount: Long = 1) {
        if (amount < minimalAmount) {
            throw NegativeAmountAccountsException(amount)
        }
    }
}
