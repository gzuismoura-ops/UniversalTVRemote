package com.universalremote.tv.model

enum class TVBrand(val displayName: String, val emoji: String) {
    LG("LG webOS", "📺"), SAMSUNG("Samsung", "📱"), SONY("Sony Bravia", "🎵"),
    ROKU("Roku", "🟣"), VIZIO("Vizio", "📡"), PANASONIC("Panasonic", "📺"),
    PHILIPS("Philips", "💡"), ANDROID_TV("Android TV", "🤖"), CHROMECAST("Chromecast / Google TV", "📡"), UNKNOWN("Unknown / DLNA", "📺");

    companion object {
        fun fromText(value: String): TVBrand {
            val s = value.lowercase()
            return when {
                "webos" in s || "lge" in s || "lg electronics" in s -> LG
                "samsung" in s || "tizen" in s -> SAMSUNG
                "sony" in s || "bravia" in s -> SONY
                "roku" in s -> ROKU
                "vizio" in s -> VIZIO
                "panasonic" in s || "viera" in s -> PANASONIC
                "philips" in s || "jointspace" in s -> PHILIPS
                "android tv" in s || "adt-" in s -> ANDROID_TV
                "chromecast" in s || "google cast" in s -> CHROMECAST
                else -> UNKNOWN
            }
        }
    }
}
