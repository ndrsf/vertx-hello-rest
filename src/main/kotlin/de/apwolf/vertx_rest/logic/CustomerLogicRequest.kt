package de.apwolf.vertx_rest.logic

import de.apwolf.vertx_rest.logic.CustomerLogicRequestMode.INSERT
import de.apwolf.vertx_rest.logic.CustomerLogicRequestMode.UPDATE
import java.time.LocalDate

data class CustomerLogicRequest(val customerId: CustomerId?, val name: String?, val birthday: LocalDate?,
    val mode: CustomerLogicRequestMode) {

    init {

        val errorMessage: String
        val valid: Boolean = when (mode) {
            UPDATE -> {
                errorMessage = "customerId, name and birthday must be set"
                customerId != null && name != null && birthday != null
            }
            INSERT -> {
                errorMessage = "name and birthday must be set"
                name != null && birthday != null
            }
        }

        if (!valid) {
            throw IllegalStateException("Error when creating CustomerLogicRequest, $errorMessage")
        }

    }

}

enum class CustomerLogicRequestMode {

    UPDATE, INSERT

}
