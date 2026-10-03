# Libraries used here (Hilt, Room, kotlinx.serialization, Retrofit, OkHttp, Coil, CameraX, ML Kit,
# ZXing, Glance) ship their own consumer R8 rules, so few extra keep rules are needed.

# ML Kit finds its components by the class names in manifest metadata and creates them by
# reflection. In full mode, R8 drops the constructor of a class kept without members, which left
# the barcode scanner without its component in 1.1.0. CI checks that these stay intact.
-keep class * implements com.google.firebase.components.ComponentRegistrar {
    <init>();
}

# Repackage into a single package to shrink the APK a little more.
-repackageclasses
