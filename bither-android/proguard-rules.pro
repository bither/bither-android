# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/nn/Library/Android/sdk/tools/proguard/proguard-android.txt
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

# Keep source file and line numbers for better crash logs
-keepattributes SourceFile,LineNumberTable

# apache
-dontwarn android.net.http.AndroidHttpClient
-dontwarn org.apache.http.**

# logback
-dontwarn ch.qos.logback.core.net.*

-dontwarn javax.annotation.**
-dontwarn javax.inject.**
-dontwarn sun.misc.Unsafe

# bither.net
-keep class net.bither.ChooseModeActivity$ShowHideView { *; }
-keep class net.bither.ui.base.WrapLayoutParamsForAnimator { *; }
-keep class net.bither.ui.base.MarketListHeader$BgHolder { *; }
-keep class net.bither.BitherSetting { *; }
-keep public enum net.bither.** { *; }
-keep public enum net.bither.bitherj.factory.ImportHDSeed$ImportHDSeedType { *; }


-keep class org.apache.http.** { *; }
-dontwarn org.apache.http.**
-dontwarn android.net.http.AndroidHttpClient
-dontwarn com.google.gson.**
-dontwarn java.nio.file.**
-dontwarn org.spongycastle.**
-dontwarn com.fasterxml.uuid.**

# SpongyCastle
-dontwarn org.spongycastle.jce.provider.X509LDAPCertStoreSpi
-dontwarn org.spongycastle.x509.util.LDAPStoreHelper

# Keep source file and line numbers for better crash logs
-keepattributes SourceFile,LineNumberTable

# Avoid throws declarations getting removed from retrofit service definitions
#-keepattributes Exceptions
#-dontwarn okhttp3.**
#-keep class okhttp3.** { *; }
#-dontwarn retrofit2.**
#-keep class retrofit2.** { *; }
#-dontwarn okio.**

# Using reflection to retrieve system method from PackageManager
-keep class android.content.pm.* { *; }

-dontwarn sun.misc.**

# rxjava
#-keep class rx.schedulers.Schedulers {
#    public static <methods>;
#}
#-keep class rx.schedulers.ImmediateScheduler {
#    public <methods>;
#}
#-keep class rx.schedulers.TestScheduler {
#    public <methods>;
#}
#-keep class rx.schedulers.Schedulers {
#    public static ** test();
#}
#-keepclassmembers class rx.internal.util.unsafe.*ArrayQueue*Field* {
#    long producerIndex;
#    long consumerIndex;
#}
#-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueProducerNodeRef {
#    long producerNode;
#    long consumerNode;
#}

# RetroLambda
-dontwarn java.lang.invoke.*

# Configuration for Guava 19.0
#
# disagrees with instructions provided by Guava project: https://code.google.com/p/guava-libraries/wiki/UsingProGuardWithGuava

-dontwarn com.google.common.**
-dontwarn com.google.errorprone.annotations.**

-keep class com.google.common.io.Resources {
    public static <methods>;
}
-keep class com.google.common.collect.Lists {
    public static ** reverse(**);
}
-keep class com.google.common.base.Charsets {
    public static <fields>;
}

-keep class com.google.common.base.Joiner {
    public static Joiner on(String);
    public ** join(...);
}

-keep class com.google.common.collect.MapMakerInternalMap$ReferenceEntry
-keep class com.google.common.cache.LocalCache$ReferenceEntry

# http://stackoverflow.com/questions/9120338/proguard-configuration-for-guava-with-obfuscation-and-optimization
-dontwarn javax.annotation.**
-dontwarn javax.inject.**
-dontwarn sun.misc.Unsafe

# Gson uses generic type information stored in a class file when working with fields. Proguard
# removes such information by default, so configure it to keep all of it.
-keepattributes Signature

# Android Studio 2.2 patch
-keepattributes EnclosingMethod

# eventbus
-keepattributes *Annotation*
-keepclassmembers class ** {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
#-keep enum org.greenrobot.eventbus.ThreadMode { *; }

# Only required if you use AsyncExecutor
#-keepclassmembers class * extends org.greenrobot.eventbus.util.ThrowableFailureEvent {
#    <init>(java.lang.Throwable);
#}
