package com.universalremote.tv.controller

import com.universalremote.tv.data.CredentialStore
import com.universalremote.tv.model.DiscoveredTV
import com.universalremote.tv.model.TVBrand

object TVControllerFactory {
    fun create(tv: DiscoveredTV, credentialStore: CredentialStore): BaseTVController = when (tv.brand) {
        TVBrand.LG -> LGWebOSController(tv, credentialStore)
        TVBrand.SAMSUNG -> SamsungTizenController(tv, credentialStore)
        TVBrand.SONY -> SonyBraviaController(tv, credentialStore)
        TVBrand.ROKU -> RokuECPController(tv, credentialStore)
        TVBrand.VIZIO -> VizioSmartCastController(tv, credentialStore)
        TVBrand.PANASONIC -> PanasonicVieraController(tv, credentialStore)
        TVBrand.PHILIPS -> PhilipsJointSpaceController(tv, credentialStore)
        TVBrand.ANDROID_TV -> AndroidTVADBController(tv, credentialStore)
        TVBrand.CHROMECAST -> ChromecastDIALController(tv, credentialStore)
        TVBrand.UNKNOWN -> GenericDLNAController(tv, credentialStore)
    }
}
