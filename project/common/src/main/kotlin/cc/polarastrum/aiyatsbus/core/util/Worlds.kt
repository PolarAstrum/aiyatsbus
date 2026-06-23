package cc.polarastrum.aiyatsbus.core.util

import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.metadata.Metadatable
import org.bukkit.persistence.PersistentDataHolder
import org.bukkit.persistence.PersistentDataType
import taboolib.library.xseries.particles.XParticle
import taboolib.platform.util.bukkitPlugin
import taboolib.platform.util.removeMeta
import taboolib.platform.util.setMeta
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.cos
import kotlin.math.sin

/**
 * 世界工具类
 * 
 * 提供世界、位置、持久化数据容器（PDC）等相关的工具函数。
 * 包含时间判断、PDC 操作、元数据标记、位置序列化等功能。
 *
 * @author mical
 * @since 2024/8/27 17:23
 */

/**
 * 世界是否为白天
 * 
 * 根据世界时间判断是否为白天（时间在 0-12300 或 23850-24000 之间）。
 * Minecraft 中一天为 24000 刻，白天约为 0-12300 刻。
 */
val World.isDay: Boolean
    get() = time !in 12300..23850

/**
 * 世界是否为黑夜
 * 
 * 根据世界时间判断是否为黑夜（时间在 12301-23849 之间）。
 * 黑夜期间怪物会自然生成。
 */
val World.isNight: Boolean
    get() = time in 12301..23849

/**
 * 从 PDC 获取内容
 * 
 * 使用操作符重载从持久化数据容器中获取数据。
 * 支持自定义命名空间，默认为 "aiyatsbus"。
 *
 * @param T 原始数据类型
 * @param Z 目标数据类型
 * @param key 数据键名
 * @param type 数据类型
 * @param namespace 命名空间，默认为 "aiyatsbus"
 * @return 获取的数据，如果不存在则返回 null
 * 
 * @example
 * ```kotlin
 * // 获取字符串数据
 * val name = block["playerName", PersistentDataType.STRING]
 * 
 * // 获取整数数据
 * val level = block["enchantLevel", PersistentDataType.INTEGER]
 * 
 * // 使用自定义命名空间
 * val custom = block["key", PersistentDataType.STRING, "myplugin"]
 * ```
 */
operator fun <T, Z> PersistentDataHolder.get(key: String, type: PersistentDataType<T, Z>, namespace: String = "aiyatsbus"): Z? {
    return persistentDataContainer.get(NamespacedKey(namespace, key), type)
}

/**
 * 向 PDC 设置内容（指定命名空间）
 * 
 * 使用操作符重载向持久化数据容器中设置数据。
 * 支持自定义命名空间，数据会持久化保存。
 *
 * @param T 原始数据类型
 * @param Z 目标数据类型
 * @param namespace 命名空间
 * @param key 数据键名
 * @param type 数据类型
 * @param value 要设置的值
 * 
 * @example
 * ```kotlin
 * // 设置字符串数据
 * block["custom", "playerName", PersistentDataType.STRING] = "Steve"
 * 
 * // 设置整数数据
 * block["custom", "enchantLevel", PersistentDataType.INTEGER] = 5
 * ```
 */
operator fun <T, Z : Any> PersistentDataHolder.set(namespace: String, key: String, type: PersistentDataType<T, Z>, value: Z) {
    persistentDataContainer.set(NamespacedKey(namespace, key), type, value)
}

/**
 * 向 PDC 设置内容（默认命名空间）
 * 
 * 使用操作符重载向持久化数据容器中设置数据，使用默认命名空间。
 * 数据会持久化保存，即使服务器重启也不会丢失。
 *
 * @param T 原始数据类型
 * @param Z 目标数据类型
 * @param key 数据键名
 * @param type 数据类型
 * @param value 要设置的值
 * 
 * @example
 * ```kotlin
 * // 设置字符串数据
 * block["playerName", PersistentDataType.STRING] = "Steve"
 * 
 * // 设置整数数据
 * block["enchantLevel", PersistentDataType.INTEGER] = 5
 * 
 * // 设置布尔数据
 * block["isProtected", PersistentDataType.BOOLEAN] = true
 * ```
 */
operator fun <T, Z : Any> PersistentDataHolder.set(key: String, type: PersistentDataType<T, Z>, value: Z) {
    set("aiyatsbus", key, type, value)
}

/**
 * 判断 PDC 是否包含某个键
 * 
 * 检查持久化数据容器中是否包含指定的键。
 * 支持自定义命名空间检查。
 *
 * @param T 原始数据类型
 * @param Z 目标数据类型
 * @param key 数据键名
 * @param type 数据类型
 * @param namespace 命名空间，默认为 "aiyatsbus"
 * @return 如果包含该键则返回 true
 * 
 * @example
 * ```kotlin
 * // 检查默认命名空间
 * if (block.has("playerName", PersistentDataType.STRING)) {
 *     println("包含玩家名称数据")
 * }
 * 
 * // 检查自定义命名空间
 * if (block.has("key", PersistentDataType.STRING, "myplugin")) {
 *     println("包含自定义数据")
 * }
 * ```
 */
fun <T, Z : Any> PersistentDataHolder.has(key: String, type: PersistentDataType<T, Z>, namespace: String = "aiyatsbus"): Boolean {
    return persistentDataContainer.has(NamespacedKey(namespace, key), type)
}

/**
 * 从 PDC 移除内容
 * 
 * 从持久化数据容器中移除指定的键值对。
 * 支持自定义命名空间，移除后数据将不再存在。
 *
 * @param key 要移除的键名
 * @param namespace 命名空间，默认为 "aiyatsbus"
 * 
 * @example
 * ```kotlin
 * // 移除默认命名空间的数据
 * block.remove("playerName")
 * 
 * // 移除自定义命名空间的数据
 * block.remove("key", "myplugin")
 * ```
 */
fun PersistentDataHolder.remove(key: String, namespace: String = "aiyatsbus") {
    return persistentDataContainer.remove(NamespacedKey(namespace, key))
}

/**
 * 标记对象
 * 
 * 为可元数据对象添加标记。
 * 标记使用插件命名空间，用于临时数据存储。
 *
 * @param key 标记键名
 * 
 * @example
 * ```kotlin
 * // 标记方块为已处理
 * block.mark("processed")
 * 
 * // 标记实体为特殊状态
 * entity.mark("inCombat")
 * ```
 */
fun Metadatable.mark(key: String) {
    setMeta(key, bukkitPlugin)
}

/**
 * 移除标记
 * 
 * 移除可元数据对象的标记。
 * 标记移除后，相关数据将不再存在。
 *
 * @param key 要移除的标记键名
 * 
 * @example
 * ```kotlin
 * // 移除方块标记
 * block.unmark("processed")
 * 
 * // 移除实体标记
 * entity.unmark("inCombat")
 * ```
 */
fun Metadatable.unmark(key: String) {
    removeMeta(key)
}

/**
 * 将位置序列化为文本字符串
 * 
 * 将 Location 的世界名和坐标信息序列化为文本格式。
 * 格式为 "worldName,x,y,z"，便于存储和传输。
 *
 * @return 格式为 "worldName,x,y,z" 的字符串
 * 
 * @example
 * ```kotlin
 * val location = player.location
 * val serialized = location.serialized // 返回 "world,100,64,200"
 * 
 * // 用于配置文件存储
 * config.set("spawn.location", location.serialized)
 * ```
 */
val Location.serialized get() = "${world.name},$blockX,$blockY,$blockZ"

/**
 * 生成 RNA 螺旋形粒子。
 *
 * 以 [loc] 为中心，沿 Y 轴从 `-height` 到 `height` 绕圈生成粒子。
 * 默认每圈生成 10 个采样点，只生成 1 圈。
 *
 * @param T 粒子额外数据类型，例如 [org.bukkit.Particle.DustOptions]、[org.bukkit.block.data.BlockData] 等
 * @param particle 粒子类型
 * @param loc 粒子效果中心点
 * @param amount 每个采样点生成的粒子数量
 * @param option 粒子的额外数据；粒子不需要数据时可传入 null
 * @param height 螺旋高度的一半，粒子会从 `-height` 生成到 `height`
 * @param range 螺旋半径
 */
fun <T> XParticle.spawnRNAParticles(
    loc: Location,
    amount: Int,
    option: T?,
    height: Double,
    range: Double,
) {
    spawnRNAParticles(loc, amount, option, height, range, 10, 1)
}

/**
 * 生成 RNA 螺旋形粒子。
 *
 * [factor] 控制每圈采样数量，[circle] 控制总圈数。
 * 例如 `factor = 20, circle = 2` 会在两圈螺旋上生成约 40 个采样点。
 *
 * @param T 粒子额外数据类型，例如 [org.bukkit.Particle.DustOptions]、[org.bukkit.block.data.BlockData] 等
 * @param particle 粒子类型
 * @param loc 粒子效果中心点
 * @param amount 每个采样点生成的粒子数量
 * @param option 粒子的额外数据；粒子不需要数据时可传入 null
 * @param height 螺旋高度的一半，粒子会从 `-height` 生成到 `height`
 * @param range 螺旋半径
 * @param factor 每圈采样点数量
 * @param circle 螺旋圈数
 */
fun <T> XParticle.spawnRNAParticles(
    loc: Location,
    amount: Int,
    option: T?,
    height: Double,
    range: Double,
    factor: Int,
    circle: Int,
) {
    val particle = this.get() ?: return
    val world = loc.world
    var currentH = -height
    var i = 0.0
    while (i <= 360.0 * circle) {
        val rad = Math.toRadians(i)
        val sin = sin(rad) * range
        val cos = cos(rad) * range
        world.spawnParticle(particle, loc.clone().add(sin, currentH, cos), amount, option)
        i += 360.0 / factor
        currentH += 2.0 * height / factor / circle
    }
}

/**
 * 生成圆形粒子。
 *
 * 以 [loc] 为圆心，在当前水平面上生成一圈粒子。
 * 默认采样点数量为 10。
 *
 * @param T 粒子额外数据类型，例如 [org.bukkit.Particle.DustOptions]、[org.bukkit.block.data.BlockData] 等
 * @param particle 粒子类型
 * @param loc 圆心位置
 * @param amount 每个采样点生成的粒子数量
 * @param option 粒子的额外数据；粒子不需要数据时可传入 null
 * @param range 圆形半径
 */
fun <T> XParticle.spawnCircleParticles(
    loc: Location,
    amount: Int,
    option: T?,
    range: Double,
) {
    this.spawnCircleParticles(loc, amount, option, range, 10)
}

/**
 * 生成圆形粒子。
 *
 * [factor] 控制圆周上的采样点数量，数值越大圆形越平滑。
 *
 * @param T 粒子额外数据类型，例如 [org.bukkit.Particle.DustOptions]、[org.bukkit.block.data.BlockData] 等
 * @param particle 粒子类型
 * @param loc 圆心位置
 * @param amount 每个采样点生成的粒子数量
 * @param option 粒子的额外数据；粒子不需要数据时可传入 null
 * @param range 圆形半径
 * @param factor 圆周采样点数量
 */
fun <T> XParticle.spawnCircleParticles(
    loc: Location,
    amount: Int,
    option: T?,
    range: Double,
    factor: Int,
) {
    val particle = this.get() ?: return
    val world = loc.world
    var i = 0.0
    while (i <= 360.0) {
        val rad = Math.toRadians(i)
        val sin = sin(rad) * range
        val cos = cos(rad) * range
        world.spawnParticle(particle, loc.clone().add(sin, 0.0, cos), amount, option)
        i += 360.0 / factor
    }
}

/**
 * 生成单点粒子。
 *
 * 当 [option] 非空时，会先使用 [Particle.getDataType] 转换数据类型；
 * 否则使用不带额外数据的粒子生成方式，避免给普通粒子传入无效数据。
 *
 * @param T 粒子额外数据类型，例如 [org.bukkit.Particle.DustOptions]、[org.bukkit.block.data.BlockData] 等
 * @param particle 粒子类型
 * @param loc 粒子生成位置
 * @param amount 生成的粒子数量
 * @param option 粒子的额外数据；粒子不需要数据时可传入 null
 */
fun <T> XParticle.spawnSimpleParticle(loc: Location, amount: Int, option: T?) {
    val particle = this.get() ?: return
    if (option != null) {
        loc.world.spawnParticle(particle, loc, amount, particle.dataType.cast(option))
    } else {
        loc.world.spawnParticle(particle, loc, amount)
    }
}
