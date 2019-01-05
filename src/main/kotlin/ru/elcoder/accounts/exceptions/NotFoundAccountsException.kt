package ru.elcoder.accounts.exceptions

import java.net.HttpURLConnection

@Suppress("MemberVisibilityCanBePrivate", "CanBeParameter")
class NotFoundAccountsException(val number: String)
    : AccountsException("Account $number not found") {
    override val status: Int
        get() = HttpURLConnection.HTTP_NOT_FOUND
}
