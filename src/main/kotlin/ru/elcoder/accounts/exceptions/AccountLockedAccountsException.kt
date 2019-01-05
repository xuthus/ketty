package ru.elcoder.accounts.exceptions

import java.net.HttpURLConnection

@Suppress("MemberVisibilityCanBePrivate", "CanBeParameter")
class AccountLockedAccountsException(val number: String)
    : AccountsException("Account $number is locked") {
    override val status: Int
        get() = HttpURLConnection.HTTP_FORBIDDEN
}
