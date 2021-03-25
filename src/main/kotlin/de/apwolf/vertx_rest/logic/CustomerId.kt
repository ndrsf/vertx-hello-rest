package de.apwolf.vertx_rest.logic

data class CustomerId private constructor(val id: Int) : Comparable<CustomerId> {


    companion object {

        fun validateCustomerId(customerId: String): Boolean {
            return try {
                customerId.toInt()
                true
            } catch (e: Exception) {
                false
            }
        }

        fun of(id: Int): CustomerId {
            return CustomerId(id)
        }

        fun of(id: String): CustomerId? {
            if (validateCustomerId(id)) {
                return CustomerId(id.toInt())
            }
            return null
        }

    }

    override fun compareTo(other: CustomerId): Int {
        return id.compareTo(other.id)
    }

}