# NativeCore resolves JNI methods by class + method name. Do not rename or obfuscate.
-keep class io.github.kingkaonix.aiaio.core.NativeCore { *; }
-keepclasseswithmembernames class io.github.kingkaonix.aiaio.core.NativeCore {
    native <methods>;
}

# Keep AIDL stubs used across processes.
-keep class io.github.kingkaonix.aiaio.core.ICoreService { *; }
-keep class io.github.kingkaonix.aiaio.runtime.IRuntimeService { *; }

# Shizuku instantiates the user service by name from the APK. It has a stable tag, but the
# class itself must survive shrinking/obfuscation.
-keep class io.github.kingkaonix.aiaio.runtime.RuntimeUserService { *; }

# kotlinx.serialization is used for sync/protocol models.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
