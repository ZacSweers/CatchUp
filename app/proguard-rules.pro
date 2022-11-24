# This is a configuration file for ProGuard.
# http://proguard.sourceforge.net/index.html#manual/usage.html
-allowaccessmodification
-dontpreverify
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# For native methods, see http://proguard.sourceforge.net/manual/examples.html#native
-keepclasseswithmembernames class * {
    native <methods>;
}

# For enumeration classes, see http://proguard.sourceforge.net/manual/examples.html#enumerations
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepclassmembers class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator CREATOR;
}

# Assume isInEditMode() always return false in release builds so they can be pruned
-assumevalues public class * extends android.view.View {
  boolean isInEditMode() return false;
}

-keepclassmembers class **.R$* {
    public static <fields>;
}

-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*

# Retrofit
# This is to keep parameters on retrofit2.http-annotated methods while still allowing removal of unused ones
-keep,allowobfuscation @interface retrofit2.http.**
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.** <methods>;
}

# Okio
-dontwarn okio.**

# Kotlin
-dontwarn kotlin.**

# Javax Extras
-dontwarn com.uber.javaxextras.**

# Some unsafe classfactory stuff
-keep class sun.misc.Unsafe { *; }

# CheckerFramework/EP
-dontwarn org.checkerframework.**
-dontwarn afu.org.checkerframework.**
-dontwarn com.google.errorprone.annotations.**

# Tikxml
# The name of @Xml types is used to look up the generated adapter.
-keepnames @com.tickaroo.tikxml.annotation.Xml class *
-keep class **$$TypeAdapter

# Retain generated TypeAdapter if annotated type is retained.
# NOTE this doesn't work right now. Would like to get it working rather than keep the blanket typeadapter keep above, but oh well
#-if @com.tickaroo.tikxml.annotation.Xml class *
#-keep class <1>$$TypeAdapter {
#    <init>(...);
#    <fields>;
#}

# Ensure the custom, fast service loader implementation is removed. R8 will fold these for us
-assumenosideeffects class kotlinx.coroutines.internal.MainDispatcherLoader {
    boolean FAST_SERVICE_LOADER_ENABLED return false;
}
-assumenosideeffects class kotlinx.coroutines.internal.FastServiceLoader {
    boolean ANDROID_DETECTED return true;
}
-checkdiscard class kotlinx.coroutines.internal.FastServiceLoader

# In release builds, isDebug() is always false
-assumevalues class * implements dev.zacsweers.catchup.appconfig.AppConfig {
  boolean isDebug(...) return false;
}
# AppConfig#sdkInt is always 21+
-assumevalues public class dev.zacsweers.catchup.appconfig.AppConfig {
  int getSdkInt(...) return 28..2147483647;
}

# Check that qualifier annotations have been discarded.
-checkdiscard @javax.inject.Qualifier class *

# Coroutines debug agent bits
-dontwarn java.lang.instrument.ClassFileTransformer
-dontwarn sun.misc.SignalHandler

# From OkHttp but gated appropriately
-dontwarn org.conscrypt.ConscryptHostnameVerifier

# ZoneRulesProvider _does_ exist!
-dontwarn java.time.zone.ZoneRulesProvider

-dontwarn com.caverock.androidsvg.**
-dontwarn kotlinx.serialization.**
-dontwarn pl.droidsonroids.gif.**