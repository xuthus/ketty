# ketty
kotlin, jetty, jersey, guice, hibernate, H2, concurrency

Before I used Kotlin in production for one time, but like to use it in persional projects. Sometimes.
Did not used Jetty and Guice at all.

Build: `mvn clean package`

Run: `run.cmd`

Not implemented:
* I did not used DI implementation used in Jetty by default, even use com.google.* annotations instead of javax.* ones to avoid it
* logging only for debugging
* business logic
* unit tests mocking - almost all of them are running in almost full context - with database (H2), hibernate, but without jetty
* there are no integration tests - where http client calls rest service - because there is direct rest service test and those tests will be Jetty tests indeed
