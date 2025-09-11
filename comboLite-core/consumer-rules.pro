# 保留核心公共 API 接口
-keep public interface com.combo.core.api.IPluginEntryClass
-keep public interface com.combo.core.api.IPluginActivity
-keep public interface com.combo.core.api.IPluginService
-keep public interface com.combo.core.api.IPluginReceiver

# 保留框架核心管理器和公共入口
-keep public class com.combo.core.runtime.PluginManager {
    public static <fields>;
    public static <methods>;
}

# 保留 Android 四大组件的基类和代理类
-keep public class com.combo.core.component.activity.BaseHostActivity { *; }
-keep public class com.combo.core.component.activity.BasePluginActivity { *; }
-keep public class com.combo.core.component.service.BaseHostService { *; }
-keep public class com.combo.core.component.service.BasePluginService { *; }
-keep public class com.combo.core.component.receiver.BaseHostReceiver { *; }
-keep public class com.combo.core.component.provider.BaseHostProvider { *; }

# 为宿主 App 提供的 Application 基类
-keep public class com.combo.core.runtime.app.BaseHostApplication

# 保留公共的工具类和扩展函数
-keep public class com.combo.core.utils.ExtensionsKt {
    public static <methods>;
}

# 保留用于序列化的数据模型
-keep class com.combo.core.model.** { *; }
-keep @kotlinx.serialization.Serializable class * {
    <fields>;
    *** Companion;
}
-keep class *$$serializer { *; }

# 保留权限系统和注解
-keep @interface com.combo.core.security.permission.RequiresPermission { *; }
-keepclassmembers class * {
    @com.combo.core.security.permission.RequiresPermission <methods>;
}

# 保留崩溃处理和授权相关的 Activity
-keep public class com.combo.core.security.crash.CrashActivity
-keep public class com.combo.core.security.auth.AuthorizationActivity

# 保留 Jetpack Compose 相关规则
-keepclasseswithmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}
-keepclassmembers class **.R$* {
    <fields>;
}
-keepclassmembers class **.*Kt {
    @androidx.compose.runtime.Composable <methods>;
}

# 保留第三方库（Koin）的相关规则
-keep class org.koin.** { *; }
-keep class * extends org.koin.core.module.Module
-keep class * implements org.koin.core.module.Module
-keep class org.koin.ksp.generated.** { *; }
-keep,includedescriptorclasses class **.*_Factory { *; }