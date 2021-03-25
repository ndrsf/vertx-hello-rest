package de.apwolf.vertx_rest.restadapter

import de.apwolf.vertx_rest.logic.Customer
import java.time.LocalDate

data class CustomerResponse(val id: Int, val name: String, val birthday: LocalDate) {

    companion object {

        fun fromCustomer(customer: Customer): CustomerResponse {
            return CustomerResponse(customer.id.id, customer.name, customer.birthday)
        }
    }
}
