package ru.elcoder.accounts.exceptions

@Suppress("CanBeParameter", "MemberVisibilityCanBePrivate")
class NegativeAmountAccountsException(val amount: Long)
    : AccountsException("Amount cannot be less or equal to 0: $amount")
