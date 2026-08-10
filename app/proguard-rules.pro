# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# BlackBox Core & Virtual Framework Keep Rules
-keep class top.niunaijun.blackbox.** { *; }
-keep class top.niunaijun.jnihook.** { *; }
-keep class com.equinox.virtual.** { *; }
-keep class black.** { *; }

# Preserve JNI methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Preserve Reflection / Serialization / Annotations
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,Exceptions,SourceFile,LineNumberTable

# Firebase & Google Play Services Keep Rules
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-keepclassmembers class * implements com.google.firebase.components.ComponentRegistrar {
    public *;
}
-keep class com.google.firebase.provider.FirebaseInitProvider { *; }
-dontwarn com.google.firebase.**

# Firestore specific (gRPC, Protobuf, etc.)
-keep class io.grpc.** { *; }
-keep class com.google.protobuf.** { *; }
-keep class com.google.common.base.** { *; }
-keep class com.google.common.collect.** { *; }
-keep class com.google.common.io.** { *; }
-keep class com.google.common.util.concurrent.** { *; }
-dontwarn io.grpc.**
-dontwarn com.google.protobuf.**
-dontwarn javax.annotation.**

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# BlackBox
-keep class top.niunaijun.blackbox.** { *; }
-keep class com.equinox.virtual.** { *; }
-keep interface top.niunaijun.blackbox.** { *; }
-dontwarn top.niunaijun.blackbox.**

# Keep models
-keep class com.equinox.virtual.model.** { *; }
-keepclassmembers class com.equinox.virtual.model.** { *; }

-dontwarn java.lang.invoke.**
