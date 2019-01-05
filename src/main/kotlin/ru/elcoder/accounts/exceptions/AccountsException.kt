package ru.elcoder.accounts.exceptions

import java.net.HttpURLConnection

open class AccountsException(message: String) : Exception(message) {
    open val status: Int = HttpURLConnection.HTTP_INTERNAL_ERROR
}
