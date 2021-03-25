package de.apwolf.vertx_rest.logic

import de.apwolf.vertx_rest.logic.CustomerId
import io.vertx.junit5.VertxExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
class CustomerIdTest {

    @Test
    fun testValidateCustomerId() {
        assertTrue(CustomerId.validateCustomerId("123"))
        assertTrue(CustomerId.validateCustomerId("0"))
        assertTrue(CustomerId.validateCustomerId("-123"))
        assertFalse(CustomerId.validateCustomerId("null"))
        assertFalse(CustomerId.validateCustomerId("5.5"))
    }

    @Test
    fun testOfString() {
        assertNull(CustomerId.of("a"))
        assertEquals(1, CustomerId.of("1")!!.id)
        assertEquals(0, CustomerId.of("0")!!.id)
        assertEquals(-1, CustomerId.of("-1")!!.id)
    }

    @Test
    fun testOfInt() {
        assertEquals(1, CustomerId.of(1).id)
        assertEquals(0, CustomerId.of(0).id)
        assertEquals(-1, CustomerId.of(-1).id)
    }
}