package com.universalremote.tv.model

data class DiscoveredTV(
    val ip: String,
    val brand: TVBrand,
    val modelName: String,
    val friendlyName: String,
    val port: Int,
    val protocol: String,
    val requiresPairing: Boolean,
    val macAddress: String? = null,
    val descriptionUrl: String? = null
) {
    val id: String get() = "$ip:${port}:${brand.name}"
}
