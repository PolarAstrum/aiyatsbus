package cc.polarastrum.aiyatsbus.core.data.trigger

import cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantment
import cc.polarastrum.aiyatsbus.core.data.trigger.artifact.Artifact
import cc.polarastrum.aiyatsbus.core.data.trigger.builtin.Builtin
import cc.polarastrum.aiyatsbus.core.data.trigger.event.EventExecutor
import cc.polarastrum.aiyatsbus.core.data.trigger.skill.Skill
import cc.polarastrum.aiyatsbus.core.data.trigger.ticker.Ticker
import cc.polarastrum.aiyatsbus.core.util.safeguard
import taboolib.library.configuration.ConfigurationSection
import taboolib.library.reflex.Reflex.Companion.invokeConstructor
import java.io.Closeable
import java.util.*

/**
 * 机制类类
 *
 * 负责注册和注销各种触发器。
 * 支持事件监听器、定时器、技能附魔三种触发方式。
 *
 * @author mical
 * @since 2026/2/8 18:55
 */
data class Mechanism(
    /** 配置节点 */
    private val section: ConfigurationSection?,
    /** 所属附魔 */
    var enchant: AiyatsbusEnchantment
) : Closeable {

    /** 已注册的触发器与类型映射表 */
    val registeredTriggers = HashMap<Trigger, TriggerType>()
    /** 各触发器类型的优先级缓存 */
    private val priorities = EnumMap<TriggerType, Int>(TriggerType::class.java)

    /**
     * 初始化所有触发器
     *
     * 解析配置并注册监听器、定时器、技能触发器。此方法应在附魔加载时调用。
     */
    fun init() = safeguard("附魔机制", "enchantment mechanisms") {
        // 初始化事件监听器
        section?.getConfigurationSection("listeners")?.let { listenersSection ->
            for (listener in listenersSection.getKeys(false)) {
                val trigger = EventExecutor(listenersSection.getConfigurationSection(listener)!!, enchant)
                trigger.init()
                addTrigger(TriggerType.LISTENER, trigger)
            }
        }
        // 初始化定时器
        section?.getConfigurationSection("tickers")?.let { tickersSection ->
            for (ticker in tickersSection.getKeys(false)) {
                val trigger = Ticker(tickersSection.getConfigurationSection(ticker)!!, enchant)
                trigger.init()
                addTrigger(TriggerType.TICKER, trigger)
            }
        }
        // 初始化技能
        section?.getConfigurationSection("skills")?.let { skillSection ->
            for (skill in skillSection.getKeys(false)) {
                val trigger = Skill(skillSection.getConfigurationSection(skill)!!, enchant)
                trigger.init()
                addTrigger(TriggerType.SKILL, trigger)
            }
        }
        // 初始化内置触发器
        section?.getString("builtin")?.let { builtinClass ->
            val builtin = Class.forName(builtinClass).invokeConstructor(enchant) as Builtin
            addTrigger(TriggerType.BUILTIN, builtin)
        }
        // 初始化粒子触发器
        section?.getConfigurationSection("artifact")?.let {
            addTrigger(TriggerType.ARTIFACT, Artifact(it, enchant))
        }
    }

    fun addTrigger(type: TriggerType, trigger: Trigger) {
        if (type.isUnique) {
            val entry = registeredTriggers.entries.firstOrNull { it.value == type }
            if (entry != null) {
                registeredTriggers.remove(entry.key, entry.value)
            }
        }
        registeredTriggers[trigger] = type
    }

    /**
     * 获取指定触发器类型的优先级
     *
     * @param type 触发器类型
     * @return 优先级数值，默认 0
     */
    fun priority(type: TriggerType): Int {
        return priorities.getOrPut(type) { section?.getInt("priority-${type.name.lowercase()}", 0) ?: 0 }
    }

    /**
     * 获取指定类型的所有触发器
     *
     * @param type 触发器类型
     * @return 对应触发器集合
     */
    fun triggers(type: TriggerType): Collection<Trigger> {
        return registeredTriggers.filter { it.value == type }.keys
    }

    @Suppress("unchecked_cast")
    fun <T : Trigger> triggers(type: TriggerType, clazz: Class<T>): Collection<T> {
        return registeredTriggers.filter { it.value == type && clazz.isInstance(it.key) }.keys as Collection<T>
    }

    /**
     * 获取指定类型的所有触发器
     *
     * @param type 触发器类型
     * @return 对应触发器集合
     */
    @Suppress("unchecked_cast")
    inline fun <reified T : Trigger> triggersOfType(type: TriggerType): Collection<T> {
        return registeredTriggers.filter { it.value == type && it.key is T }.keys as Collection<T>
    }

    /**
     * 判断是否存在某类型触发器
     *
     * @param type 触发器类型
     * @return 是否存在
     */
    fun hasTrigger(type: TriggerType): Boolean {
        return registeredTriggers.containsValue(type)
    }

    /**
     * 禁用触发器
     *
     * 清理所有监听器和定时器，释放相关资源。
     *
     * @example
     * ```kotlin
     * trigger.onDisable()
     * ```
     */
    override fun close() {
        registeredTriggers.forEach { it.key.close() }
        registeredTriggers.clear()
    }
}
