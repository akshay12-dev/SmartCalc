# Keep ML Kit model + OCR classes when minifying.
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.vision.**

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *