package ru.elcoder.accounts.exceptions

@Suppress("MemberVisibilityCanBePrivate", "CanBeParameter")
class AccountClosedAccountsException(val number: String)
    : AccountsException("Account $number is closed")
