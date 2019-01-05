package ru.elcoder.accounts.models

import ru.elcoder.accounts.services.ACCOUNT_NUMBER_SIZE
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id

@Entity
class Account(
        @Id
        @GeneratedValue
        var id: Long? = null,

        @Column(name = "NUMBER", length = ACCOUNT_NUMBER_SIZE, nullable = false, unique = true) // enough to create index
        var number: String?,

        @Column(name = "BALANCE", nullable = false)
        var balance: Long = 0,

        @Column(name = "CLOSED", nullable = false)
        var closed: Boolean = false
) {

    constructor() : this(number = null)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return number == (other as Account).number
    }

    override fun hashCode(): Int {
        return number.hashCode()
    }

}