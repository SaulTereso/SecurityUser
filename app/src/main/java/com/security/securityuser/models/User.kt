package com.security.securityuser.models

import com.beust.klaxon.*

private val klaxon = Klaxon()

data class User (
    val id: String? = null,
    val name: String? = null,
    val app: String? = null,
    val email: String? = null,
    val phone: String? = null,
    var image: String? = null,
) {



    public fun toJson() = klaxon.toJsonString(this)

    companion object {
        public fun fromJson(json: String) = klaxon.parse<Client>(json)
    }
}