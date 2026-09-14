# ==========================================================================
#  R8 / ProGuard для Яндекс-браузера на GeckoView
# ==========================================================================
#  GeckoView очень активно использует JNI + reflection: делегаты сессии,
#  GeckoBundle-события и JNI-мосты находятся по именам. Поэтому классы движка
#  и наши делегаты обязаны остаться нетронутыми, иначе рантайм упадёт.
# ==========================================================================

-optimizationpasses 5
-allowaccessmodification
-dontpreverify
-verbose

# Сохраняем аннотации и имена, нужные reflection/JNI
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature, SourceFile, LineNumberTable, Exceptions

# --- Движок: не трогать вообще ---
-keep class org.mozilla.geckoview.** { *; }
-keep class org.mozilla.gecko.** { *; }
-keepclassmembers class * implements org.mozilla.geckoview.GeckoSession$*Delegate { *; }
-keepclassmembers class * implements org.mozilla.geckoview.GeckoRuntime$*Delegate { *; }
-keepclassmembers class * implements org.mozilla.geckoview.ContentBlocking$Delegate { *; }
-keepclassmembers class * implements org.mozilla.geckoview.WebExtension$MessageDelegate { *; }
-keepclassmembers class * implements org.mozilla.geckoview.GeckoRuntime$ActivityContextDelegate { *; }
-keepclassmembers class * implements org.mozilla.geckoview.GeckoSession$PromptDelegate { *; }
-keepclassmembers class * implements org.mozilla.geckoview.GeckoSession$PermissionDelegate { *; }
-keepclassmembers class * implements org.mozilla.geckoview.GeckoSession$ContentDelegate { *; }
-keepclassmembers class * implements org.mozilla.geckoview.GeckoSession$ProgressDelegate { *; }
-keepclassmembers class * implements org.mozilla.geckoview.GeckoSession$NavigationDelegate { *; }
-keepclassmembers class * implements org.mozilla.geckoview.GeckoSession$SelectionActionDelegate { *; }
-keepclassmembers class * implements org.mozilla.geckoview.GeckoSession$MediaDelegate { *; }

# --- Наши классы, которые дергает JNI/Android-фреймворк ---
-keep class ru.yandex.browser.mobile.core.BrowserEngine { *; }
-keep class ru.yandex.browser.mobile.core.KeepAliveService { *; }
-keep class ru.yandex.browser.mobile.core.KeepAliveReceiver { *; }
-keep class ru.yandex.browser.mobile.YandexBrowserApp { *; }
-keep class ru.yandex.browser.mobile.web.** { *; }

# --- Android-компоненты объявлены в манифесте: имена сохраняем ---
-keep public class * extends android.app.Service
-keep public class * extends android.app.Application
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends androidx.activity.ComponentActivity

# --- View-модели Compose/состояния не должны ломаться от обфускации ---
-keepclassmembers class **$$serializer { *; }
-dontwarn kotlinx.**

# --- Молчание по необязательным зависимостям движка ---
-dontwarn org.mozilla.**
-dontwarn org.yaml.snakeyaml.**
-dontwarn com.google.android.gms.**
-dontwarn org.graalvm.**
-dontwarn com.oracle.svm.**
-dontwarn javax.annotation.**
-dontwarn edu.umd.cs.findbugs.annotations.**
-dontwarn javax.lang.model.**

# --- Kotlin metadata (нужна для reflection внутри compose-рантайма) ---
-keep class kotlin.Metadata { *; }

# --- Чистим логи в релизе (мелочь, но приятно) ---
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}
