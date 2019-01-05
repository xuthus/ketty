package ru.elcoder.accounts.rest

import ru.elcoder.accounts.exceptions.AccountsException
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.ext.ExceptionMapper
import javax.ws.rs.ext.Provider

@Provider
class AccountsExceptionMapper : ExceptionMapper<AccountsException> {

    override fun toResponse(exception: AccountsException): Response =
            Response
                    .status(exception.status)
                    .entity(exception.message)
                    .type(MediaType.TEXT_PLAIN_TYPE)
                    .build()

}