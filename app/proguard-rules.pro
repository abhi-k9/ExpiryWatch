# Libraries used here (Hilt, Room, kotlinx.serialization, Retrofit, OkHttp, Coil, CameraX, ML Kit,
# ZXing, Glance) ship their own consumer R8 rules, so no extra keep rules are needed.

# Repackage into a single package to shrink the APK a little more.
-repackageclasses
