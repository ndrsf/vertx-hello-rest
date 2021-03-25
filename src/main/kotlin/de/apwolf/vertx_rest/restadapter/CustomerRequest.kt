package de.apwolf.vertx_rest.restadapter

import de.apwolf.vertx_rest.logic.CustomerId
import de.apwolf.vertx_rest.logic.CustomerLogicRequest
import de.apwolf.vertx_rest.logic.CustomerLogicRequestMode
import java.time.LocalDate

/**
 * A REST request for a customer - id is optional because it is set for PUT requests but not for POST
 */
data class CustomerRequest(var id: Int?, val name: String, val birthday: LocalDate) {

    /**
     * Converts a request to a request to the logic
     */
    fun toCustomerLogicRequest(mode: CustomerLogicRequestMode): CustomerLogicRequest {
        val customerId = if (id == null) {
            null
        } else {
            CustomerId.of(id!!)
        }
        return CustomerLogicRequest(customerId, name, birthday, mode)
    }

}
