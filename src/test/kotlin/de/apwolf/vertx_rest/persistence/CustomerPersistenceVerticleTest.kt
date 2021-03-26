package de.apwolf.vertx_rest.persistence

import de.apwolf.vertx_rest.logic.Customer
import de.apwolf.vertx_rest.logic.CustomerId
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
class CustomerPersistenceVerticleTest {

    private lateinit var sut: CustomerPersistenceVerticle

    @BeforeEach
    fun beforeEach(vertx: Vertx, testContext: VertxTestContext) {
        sut = CustomerPersistenceVerticle()

        testContext.completeNow()
    }

    /**
     * Insert, delete and insert to see if customerId is not used twice and correctly incremented
     */
    @Test
    fun testGetNextCustomerId(vertx: Vertx, testContext: VertxTestContext) {
        sut.someCustomers.clear()
        sut.someCustomers.putAll(
            hashMapOf<CustomerId, Customer>(
                CustomerId.of(1) to Customer(CustomerId.of(1), "Harrison Fraud", LocalDate.parse("1980-01-01")),
                CustomerId.of(2) to Customer(CustomerId.of(2), "Rocky Raccoon Clark", LocalDate.parse("1950-08-11")),
                CustomerId.of(3) to Customer(CustomerId.of(3), "Oaf Tobark", LocalDate.parse("1955-02-24")),
            )
        )
        val insertedCustomer = sut.insertCustomer(Customer(
            null,
            "Dude 4",
            LocalDate.now()))
        assertEquals(CustomerId.of(4), insertedCustomer.id)

        val deletedCustomer = sut.deleteCustomer(CustomerId.of(4))
        assertTrue(deletedCustomer)

        val insertedCustomerAfterDeletion = sut.insertCustomer(Customer(
            null,
            "Dude 5",
            LocalDate.now()))
        assertEquals(CustomerId.of(5), insertedCustomerAfterDeletion.id)

        testContext.completeNow()
    }

}
