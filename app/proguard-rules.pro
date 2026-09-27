-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.woojik.ondeviceai.** {
    *** Companion;
}
-keepclasseswithmembers class com.woojik.ondeviceai.** {
    kotlinx.serialization.KSerializer serializer(...);
}
