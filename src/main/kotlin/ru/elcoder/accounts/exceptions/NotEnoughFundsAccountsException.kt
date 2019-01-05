package ru.elcoder.accounts.exceptions

@Suppress("CanBeParameter", "MemberVisibilityCanBePrivate")
class NotEnoughFundsAccountsException(val number: String, val amount: Long)
    : AccountsException("There is not enough funds to debit the account $number on $amount sum")
