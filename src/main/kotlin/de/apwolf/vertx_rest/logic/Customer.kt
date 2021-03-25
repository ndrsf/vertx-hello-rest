package de.apwolf.vertx_rest.logic

import java.time.LocalDate

data class Customer(val id: CustomerId, val name: String, val birthday: LocalDate)