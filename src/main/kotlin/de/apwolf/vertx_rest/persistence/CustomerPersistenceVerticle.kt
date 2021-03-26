package de.apwolf.vertx_rest.persistence

import de.apwolf.vertx_rest.logic.Customer
import de.apwolf.vertx_rest.logic.CustomerId
import io.vertx.core.AbstractVerticle
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import io.vertx.ext.jdbc.JDBCClient
import io.vertx.ext.sql.ResultSet
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger
import org.apache.logging.log4j.kotlin.Logging

class CustomerPersistenceHandler : Handler<AsyncResult<ResultSet>>, Logging {

    override fun handle(event: AsyncResult<ResultSet>) {
        if (event.failed()) {
            logger.error("Error on database query", event.cause())
        }
    }

}

class CustomerPersistenceVerticle : AbstractVerticle(), Logging {

    override fun start(startPromise: Promise<Void>) {
        val config = config()
        config.put("url", "jdbc:hsqldb:mem:customer?shutdown=true")
        config.put("driver_class", "org.hsqldb.jdbcDriver")
        val client = JDBCClient.createShared(vertx, config(), "customer")

        client.call("CREATE TABLE IF NOT EXISTS CUSTOMER (id INTEGER IDENTITY, name VARCHAR(100), birthday DATE )") {
            if (it.failed()) {
                logger.error("Error on database query", it.cause())
                startPromise.fail(it.cause())
            } else {
                logger.info("Created table customer")
            }
        }
    }

    private fun validateDatabaseCall() {

    }

    internal val someCustomers =
        hashMapOf<CustomerId, Customer>(
            CustomerId.of(1) to Customer(CustomerId.of(1), "Harrison Fraud", LocalDate.parse("1980-01-01")),
            CustomerId.of(2) to Customer(CustomerId.of(2), "Rocky Raccoon Clark", LocalDate.parse("1950-08-11")),
            CustomerId.of(3) to Customer(CustomerId.of(3), "Oaf Tobark", LocalDate.parse("1955-02-24")),
        )

    private val nextCustomerId = AtomicInteger(someCustomers.size + 1)

    fun loadCustomer(customerId: CustomerId): Customer? {
        return someCustomers[customerId]
    }

    fun insertCustomer(customer: Customer): Customer {
        val customerId = CustomerId.of(getNextCustomerId())
        if (someCustomers[customerId] != null) {
            // Would be nicer to just get a new id but whatever...
            throw IllegalStateException("Chose next id $customerId but it's already set! Race condition?")
        }
        val customerToInsert = Customer(customerId, customer.name, customer.birthday)
        someCustomers[customerId] = customerToInsert

        return customerToInsert
    }

    fun updateCustomer(customer: Customer): Customer {
        val customerId = customer.id ?: throw IllegalArgumentException("Customer $customer has no customerId")
        someCustomers[customerId] = customer
        return someCustomers[customerId]!!
    }

    fun deleteCustomer(customerId: CustomerId): Boolean {
        val deletedCustomer = someCustomers.remove(customerId)
        return deletedCustomer != null
    }

    /**
     * Horrible id creation, I know :^)
     */
    internal fun getNextCustomerId(): Int {
        // Id was already incremented at init
        return nextCustomerId.getAndIncrement()
    }
}