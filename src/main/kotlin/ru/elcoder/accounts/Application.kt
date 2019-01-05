package ru.elcoder.accounts

import com.google.inject.Guice
import com.google.inject.Inject
import com.google.inject.name.Named
import org.glassfish.jersey.jackson.JacksonFeature
import org.glassfish.jersey.jetty.JettyHttpContainerFactory
import org.glassfish.jersey.server.ResourceConfig
import org.slf4j.LoggerFactory
import ru.elcoder.accounts.exceptions.AccountsException
import ru.elcoder.accounts.rest.AccountsService
import javax.ws.rs.core.UriBuilder
import javax.ws.rs.ext.ExceptionMapper


class Application {

    private val logger = LoggerFactory.getLogger(this.javaClass)!!

    @Inject
    private lateinit var exceptionMapper: ExceptionMapper<AccountsException>
    @Inject
    private lateinit var accountsService: AccountsService
    @Inject
    @Named("jetty.port")
    private var port: Int = 9090

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val injector = Guice.createInjector(ApplicationModule())
            val app = injector.getInstance(Application::class.java)
            app.start()
        }
    }

    private fun start() {
        logger.info("Starting Accounts Service")
        val baseUri = UriBuilder.fromUri("http://localhost/").port(port).build()
        val config = ResourceConfig()
                .register(JacksonFeature::class.java)
                .register(accountsService)
                .register(exceptionMapper)

        val server = JettyHttpContainerFactory.createServer(baseUri, config)
        try {
            server.join()
        } finally {
            server.destroy()
        }
    }
}