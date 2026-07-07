# Add project specific ProGuard rules here.
-keepclassmembers class com.mimoterm.core.ai.** { *; }
-keepclassmembers class com.mimoterm.core.filemanager.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
