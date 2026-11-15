# Universal TV Remote (Android)

A Kotlin + Jetpack Compose Android project for discovering and controlling compatible TVs and media renderers on the same local network.

## Included

- SSDP discovery for the requested DIAL, UPnP, LG webOS, Samsung and Roku targets, with UPnP device-description parsing.
- Android NSD browsing for Google Cast, AirPlay and Android TV Remote service records.
- Bounded `/24` TCP probing across the configured vendor ports and HTTP-based fingerprints.
- AES-GCM credential encryption using an Android Keystore key and SharedPreferences metadata storage.
- Working protocol paths for LG webOS SSAP, Samsung Tizen remote WebSocket, Sony Bravia IRCC, Roku ECP, Vizio SmartCast pairing/key commands, Panasonic Viera SOAP, Philips JointSpace HTTP, Chromecast DIAL app launch, and generic UPnP AVTransport/RenderingControl where the device description advertises those services.
- Compose screens for scanning, pairing, remote control, saved devices, theme choice and Wake-on-LAN.
- UDP Wake-on-LAN magic-packet transmission when a MAC address is available from the ARP table or saved metadata.

## Important compatibility and security notes

No Android application can control literally every Smart TV. Vendor APIs vary by model, region, firmware and settings; some TVs disable network control by default or require a vendor-specific authorization flow. Discovery is best-effort and a port being open is not proof that a device supports every command.

The Android TV controller deliberately reports that modern Android TV wireless pairing is not implemented. Current Android TV remote pairing uses a separate certificate-based protocol; a connection to TCP 5555 alone is not ADB authorization. This build can identify legacy ADB endpoints but does not send unauthenticated shell commands.

Philips Android TV firmware may require Digest Authentication. This project includes the JointSpace HTTP endpoints and PIN request/grant attempt, but does not claim support for every Digest-authenticated firmware variant. Chromecast DIAL is intentionally limited to app launching and limited volume calls; DIAL is not a full directional remote API. Generic DLNA exposes media and volume only when the device description contains standard AVTransport/RenderingControl service URLs.

Samsung WSS and Vizio HTTPS currently use a trust-all certificate manager to interoperate with self-signed TV certificates, as requested. This weakens TLS server authentication and should only be used on a trusted home network. Legacy vendor APIs also require cleartext HTTP, so the manifest explicitly enables cleartext traffic.

Credentials are encrypted with AES-GCM and an Android Keystore key. AndroidX Security Crypto is not used because its `EncryptedSharedPreferences` API is deprecated in the current 1.1 line; this implementation keeps the requested SharedPreferences storage while avoiding that deprecated API.

## Build requirements

- Android Studio with Android SDK Platform 35 installed.
- JDK 17.
- Internet access on the first Gradle sync so Gradle and Maven dependencies can be downloaded.

The execution environment used to assemble this archive did not have an Android SDK or a preinstalled Gradle distribution and could not resolve external hosts, so `assembleDebug` could not be run here. The project files were statically reviewed, but an actual Android Studio/Gradle build remains required to verify the final APK against the installed SDK and device-specific network behavior.

## Build & run instructions

1. Extract this folder and open `UniversalTVRemote` in Android Studio.
2. Install Android SDK Platform 35 and configure JDK 17 in Android Studio.
3. Allow Gradle sync to download the Android Gradle Plugin and declared dependencies.
4. Build the debug APK with `./gradlew assembleDebug` (Windows: `gradlew.bat assembleDebug`).
5. Install `app/build/outputs/apk/debug/app-debug.apk`, connect the phone and TV to the same Wi-Fi network, then grant any Android network permissions requested by the OS.
