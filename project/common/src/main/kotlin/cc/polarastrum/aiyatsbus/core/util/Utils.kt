package cc.polarastrum.aiyatsbus.core.util

import com.google.common.base.Enums
import com.google.gson.Gson
import org.bukkit.Bukkit
import taboolib.common.LifeCycle
import taboolib.common.TabooLib
import taboolib.common.util.t
import taboolib.library.reflex.Reflex.Companion.invokeMethod
import taboolib.library.reflex.ReflexClass
import taboolib.library.reflex.UnsafeAccess
import java.lang.reflect.Field

/**
 * 通用工具类
 * 
 * 提供反射操作、JSON 验证、字段设置等通用工具函数。
 *
 * @author mical
 * @since 2024/2/17 17:07
 */

fun isStopping(): Boolean {
    return TabooLib.isStopped() || Bukkit.getServer().isStopping
}

/**
 * 全局 GSON 实例
 * 
 * 用于 JSON 序列化和反序列化的全局 Gson 对象。
 */
val GSON = Gson()

/**
 * 字符串转枚举值
 * 
 * 将字符串转换为指定枚举类型的值，支持自定义转换函数。
 *
 * @param T 枚举类型
 * @param transfer 字符串转换函数，默认为转大写
 * @return 对应的枚举值，如果转换失败则返回 null
 * 
 * @example
 * ```kotlin
 * "sword".enumOf<WeaponType>() // 返回 WeaponType.SWORD
 * "magic".enumOf<SpellType> { it.capitalize() } // 返回 SpellType.Magic
 * ```
 */
inline fun <reified T : Enum<T>> String?.enumOf(transfer: (String) -> String = { it.uppercase() }): T? {
    return if (this == null) null else Enums.getIfPresent(T::class.java, transfer(this)).orNull()
}

/**
 * 深度调用方法
 * 
 * 通过路径分隔符调用嵌套方法，支持形如 "method1/method2/method3" 的调用路径。
 *
 * @param T 返回类型
 * @param name 方法调用路径，使用 "/" 分隔
 * @return 方法调用结果，如果调用失败则返回 null
 * 
 * @example
 * ```kotlin
 * val result = someObject.invokeMethodDeep<String>("getData/getName")
 * ```
 */
@Suppress("UNCHECKED_CAST")
fun <T> Any?.invokeMethodDeep(name: String): T? {
    var result: Any? = this
    for (method in name.split('/')) {
        result = result?.invokeMethod(method)
    }
    return result as? T
}

/**
 * 获取包名
 * 
 * 从类名中提取包名部分。
 *
 * @return 包名，如果类名为空则返回 null
 * 
 * @example
 * ```kotlin
 * reflexClass.packageName() // 返回 "com.example.package"
 * ```
 */
fun ReflexClass.packageName(): String? {
    return name?.substringBeforeLast('.')
}

/**
 * 验证 JSON 字符串
 * 
 * 检查字符串是否为有效的 JSON 格式。
 *
 * @return 如果是有效的 JSON 则返回 true
 * 
 * @example
 * ```kotlin
 * "{\"name\": \"test\"}".isValidJson() // 返回 true
 * "invalid json".isValidJson() // 返回 false
 * ```
 */
fun String.isValidJson(): Boolean {
    if (trim().isEmpty() || !startsWith("{")) return false
    return kotlin.runCatching {
        GSON.fromJson(this, Any::class.java)
    }.isSuccess
}

/**
 * 设置静态 final 字段
 * 
 * 使用 Unsafe API 设置静态 final 字段的值。
 * 这是一个危险操作，请谨慎使用。
 *
 * @param value 要设置的新值
 * 
 * @example
 * ```kotlin
 * field.setStaticFinal("new value")
 * ```
 */
fun Field.setStaticFinal(value: Any) {
    val offset = UnsafeAccess.unsafe.staticFieldOffset(this)
    UnsafeAccess.unsafe.putObject(UnsafeAccess.unsafe.staticFieldBase(this), offset, value)
}

fun safeguard(cn: String, en: String, time: Long = 5000L, block: () -> Unit) {
    try {
        block()
    } catch (t: Throwable) {
        if (TabooLib.getCurrentLifeCycle() != LifeCycle.ACTIVE) {
            Bukkit.getLogger().severe("""
                    [Aiyatsbus] 无法初始化 $cn，为避免数据丢失，服务器将会被强制关闭！
                    [Aiyatsbus] Failed to initialize $en. To avoid data loss, the server will be forced to shut down!
                """.t())
            t.printStackTrace()
            Thread.sleep(time)
            Runtime.getRuntime().halt(-1)
        }
    }
}

/**
 * ItemsAdder 插件是否启用
 * 
 * 检查 ItemsAdder 插件是否在类路径中存在。
 */
internal val itemsAdderEnabled = runCatching { 
    Class.forName("dev.lone.itemsadder.api.ItemsAdder") 
}.isSuccess

/**
 * CraftEngine 插件是否启用
 *
 * 检查 CraftEngine 插件是否在类路径中存在。
 */
internal val craftEngineEnabled = runCatching {
    Class.forName("net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine")
}.isSuccess

var libreforgeEnabled: Boolean = false