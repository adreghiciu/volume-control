# Proguard rules for Volume Control app
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

# Keep Gson classes
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Keep our app's classes
-keep class com.volumecontrol.android.** { *; }
