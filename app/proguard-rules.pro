# This is a configuration file for ProGuard.
# http://proguard.sourceforge.net/index.html#manual/usage.html
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose
-dontpreverify

# Optimize all the things (other than those listed)
-optimizations !field/*

-allowaccessmodification
-repackageclasses ''

# Note that you cannot just include these flags in your own
# configuration file; if you are including this file, optimization
# will be turned off. You'll need to either edit this file, or
# duplicate the contents of this file and remove the include of this
# file from your project's proguard.config path property.

-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgent
-keep public class * extends android.preference.Preference
-keep public class * extends android.support.v4.app.Fragment
-keep public class * extends androidx.fragment.app.Fragment
-keep public class * extends android.app.Fragment
-keep public class com.android.vending.licensing.ILicensingService

# For native methods, see http://proguard.sourceforge.net/manual/examples.html#native
-keepclasseswithmembernames class * {
    native <methods>;
}

-keep public class * extends android.view.View {
    public <init>(android.content.Context, android.util.AttributeSet);
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}

# For enumeration classes, see http://proguard.sourceforge.net/manual/examples.html#enumerations
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

-keepclassmembers class **.R$* {
    public static <fields>;
}

-keep class com.google.android.material.theme.MaterialComponentsViewInflater

-keepattributes *Annotation*
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable,Signature,JavascriptInterface

# OkHttp
-dontwarn okhttp3.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Retrofit
# Platform calls Class.forName on types which do not exist on Android to determine platform.
-dontnote retrofit2.Platform
# Platform used when running on Java 8 VMs. Will not be used at runtime.
-dontwarn retrofit2.Platform$Java8
# Retain generic type information for use by reflection by converters and adapters.
-keepattributes Signature, InnerClasses
# Retain declared checked exceptions for use by a Proxy instance.
-keepattributes Exceptions
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

# RxJava 1
-dontwarn sun.misc.**
-keepclassmembers class rx.internal.util.unsafe.*ArrayQueue*Field* {
   long producerIndex;
   long consumerIndex;
}
-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueProducerNodeRef {
    rx.internal.util.atomic.LinkedQueueNode producerNode;
}
-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueConsumerNodeRef {
    rx.internal.util.atomic.LinkedQueueNode consumerNode;
}
-dontnote rx.internal.util.PlatformDependent

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

# Fonts have a messed up proguard config
-keep class android.support.v4.provider.** { *; }

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

# TODO should be able to remove in AGP 3.7 & Coroutines 1.3.0 stable
# Ensure the custom, fast service loader implementation is removed.
-assumevalues class kotlinx.coroutines.internal.MainDispatcherLoader {
  boolean FAST_SERVICE_LOADER_ENABLED return false;
}
-checkdiscard class kotlinx.coroutines.internal.FastServiceLoader
