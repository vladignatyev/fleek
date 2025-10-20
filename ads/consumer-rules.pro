-keepattributes *Annotation*
-dontwarn android.media.LoudnessCodecController$OnLoudnessCodecUpdateListener
-dontwarn android.media.LoudnessCodecController
-dontwarn com.facebook.infer.annotation.Nullsafe$Mode
-dontwarn com.facebook.infer.annotation.Nullsafe

# Dependency: Ads Mediation: Meta
-keep class com.facebook.** { *; }
-keep interface com.facebook.** { *; }
