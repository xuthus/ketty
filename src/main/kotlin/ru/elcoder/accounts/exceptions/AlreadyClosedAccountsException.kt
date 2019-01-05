package ru.elcoder.accounts.exceptions

@Suppress("CanBeParameter", "MemberVisibilityCanBePrivate")
class AlreadyClosedAccountsException(val number: String)
    : AccountsException("Account $number is closed already")
