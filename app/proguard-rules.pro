# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/pandanomic/dev/android/android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class com.android.vending.licensing.ILicensingService
-keep class android.support.v8.renderscript.** { *; }

-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

-keepattributes SourceFile,LineNumberTable,Signature,JavascriptInterface

# OkHttp
-dontwarn okhttp3.**

# Retrofit
# Platform calls Class.forName on types which do not exist on Android to determine platform.
-dontnote retrofit2.Platform
# Platform used when running on Java 8 VMs. Will not be used at runtime.
-dontwarn retrofit2.Platform$Java8
# Retain generic type information for use by reflection by converters and adapters.
-keepattributes Signature
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
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep class com.bumptech.glide.GeneratedAppGlideModuleImpl

# Fonts have a messed up proguard config
-keep class android.support.v4.provider.** { *; }

# CheckerFramework/EP
-dontwarn org.checkerframework.**
-dontwarn afu.org.checkerframework.**
-dontwarn com.google.errorprone.annotations.**

# Bypass
-keep class in.uncod.android.bypass.Document { <init>(...); }
-keep class in.uncod.android.bypass.Element {
    <init>(...);
    void setChildren(...);
    void setParent(...);
    void addAttribute(...);
}

# Tikxml
-keepnames class **$$TypeAdapter
-keepnames @com.tickaroo.tikxml.annotation.Xml class *

# MoshKt
# Retain generated classes that end in the suffix
-keepnames class **JsonAdapter

# Prevent obfuscation of types which use @MoshiSerializable since the simple name
# is used to reflectively look up the generated adapter.
-keepnames @com.squareup.moshi.JsonClass class *
