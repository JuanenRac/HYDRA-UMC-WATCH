# kotlinx-serialization: keep the generated serializers of the protocol
# messages (the sealed hierarchy is resolved by class discriminator at runtime).
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-dontnote kotlinx.serialization.**

-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.hydraumc.watch.protocol.**$$serializer { *; }
-keepclassmembers class com.hydraumc.watch.protocol.** {
    *** Companion;
}
-keepclasseswithmembers class com.hydraumc.watch.protocol.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class com.hydraumc.watch.protocol.** { *; }
