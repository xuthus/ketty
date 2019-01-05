package ru.elcoder.accounts

import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.Singleton
import com.google.inject.name.Names
import org.hibernate.SessionFactory
import org.hibernate.cfg.Configuration
import ru.elcoder.accounts.exceptions.AccountsException
import ru.elcoder.accounts.rest.AccountsExceptionMapper
import java.io.FileInputStream
import java.io.InputStreamReader
import java.util.*
import javax.ws.rs.ext.ExceptionMapper


class ApplicationModule : AbstractModule() {

    override fun configure() {
        val properties = Properties()
        properties.load(InputStreamReader(FileInputStream("application.properties"), Charsets.UTF_8))
        Names.bindProperties(binder(), properties)
    }

    @Provides
    @Singleton
    fun provideExceptionMapper(): ExceptionMapper<AccountsException> =
            AccountsExceptionMapper()

    @Provides
    @Singleton
    fun provideSessionFactory(): SessionFactory =
            Configuration().configure().buildSessionFactory()

}
