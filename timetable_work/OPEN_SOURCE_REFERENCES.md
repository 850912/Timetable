# Open-source / official references used in Build16–Build20

## Android Wear Data Layer

Official Android documentation:

- https://developer.android.com/training/wearables/data/overview

Build17 uses runtime Google Play services availability as the first-level environment check and keeps the existing Wear Data Layer integration for Google environments.

## Android Bluetooth permissions

Official Android documentation:

- https://developer.android.com/develop/connectivity/bluetooth/bt-permissions

Build17's runtime permission handling follows the Android 12+ BLUETOOTH_SCAN / CONNECT / ADVERTISE model and the pre-Android-12 location requirement.

## Nordic Android BLE Library

Open-source reference:

- https://github.com/NordicSemiconductor/Android-BLE-Library
- Maven Central: `no.nordicsemi.android:ble:2.11.0`

License: BSD 3-Clause.

The implementation in this project does **not** add a runtime dependency on the library. Its GATT sequencing, operation waiting, MTU awareness, long-packet framing and retry-oriented design were used as open-source engineering references while keeping the project independent from a third-party runtime BLE abstraction. This is intentional because the project needs a symmetric GATT server + client role on both phone and Wear and must minimize dependency surface for China-ROM environments.

## Vendor SDK choice

Samsung Accessory SDK and vendor-private transports were not inserted as the generic China transport. The final architecture keeps a vendor-neutral BLE compatibility boundary so additional vendor adapters can be added later without changing the shared protocol.
