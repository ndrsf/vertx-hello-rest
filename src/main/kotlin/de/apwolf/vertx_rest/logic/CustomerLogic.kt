package de.apwolf.vertx_rest.logic

import de.apwolf.vertx_rest.persistence.CustomerPersistence

class CustomerLogic(val customerPersistence: CustomerPersistence) {

    fun loadCustomer(customerId: CustomerId): Customer? {
        return customerPersistence.loadCustomer(customerId)
    }

    fun insertCustomer(customerRequest: CustomerLogicRequest): Customer {
        if (customerRequest.mode != CustomerLogicRequestMode.INSERT) {
            throw IllegalArgumentException("Wrong request for insertCustomer: $customerRequest")
        }

        val customerToInsert = Customer(null, customerRequest.name!!, customerRequest.birthday!!)

        return customerPersistence.insertCustomer(customerToInsert)
    }

    fun updateCustomer(customerRequest: CustomerLogicRequest): Customer {
        if (customerRequest.mode != CustomerLogicRequestMode.UPDATE) {
            throw IllegalArgumentException("Wrong request for updateCustomer: $customerRequest")
        }

        val customerToUpdate =
            Customer(customerRequest.customerId, customerRequest.name!!, customerRequest.birthday!!)

        return customerPersistence.updateCustomer(customerToUpdate)
    }

    fun deleteCustomer(customerId: CustomerId): Boolean {
        return customerPersistence.deleteCustomer(customerId)
    }

}