package de.apwolf.vertx_rest.persistence

import de.apwolf.vertx_rest.logic.Customer
import de.apwolf.vertx_rest.logic.CustomerId
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger

class CustomerPersistence {

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