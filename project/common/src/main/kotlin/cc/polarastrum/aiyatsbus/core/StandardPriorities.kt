package cc.polarastrum.aiyatsbus.core

import taboolib.common.LifeCycle

/**
 * 标准优先级常量
 *
 * 定义了系统中各个组件的加载和执行优先级。
 * 数值越小优先级越高，确保依赖关系正确的加载顺序。
 * 用于控制附魔系统的初始化顺序，避免依赖问题。
 *
 * @author mical
 * @since 2024/2/26 23:47
 */
object StandardPriorities {

    /** 稀有度优先级，最高优先级，用于定义附魔的稀有程度 */
    const val RARITY = 0
    /** 附魔目标优先级，用于定义附魔可以应用的目标类型 */
    const val TARGET = 1
    /** 事件执行器优先级，用于处理附魔相关的事件 */
    const val EVENT_EXECUTORS = 2
    /** 技能触发器优先级，与事件执行器同级，用于处理附魔技能相关的事件 */
    const val SKILL_HANDLER = EVENT_EXECUTORS
    /** 粒子触发器优先级，与技能触发器同级，用于处理附魔粒子相关的事件 */
    const val ARTIFACT_HANDLER = SKILL_HANDLER
    /** 内置触发器优先级，用于系统内置的触发机制 */
    const val INTERNAL_TRIGGERS = 3
    /** 定时器优先级，与内置触发器同级，用于定时执行的任务 */
    const val TICKERS = 3
    /** 附魔效果优先级，用于定义附魔的具体效果 */
    const val ENCHANTMENT = 4
    /** 冻结注册表优先级，用于锁定注册表防止后续修改 */
    const val FREEZE_REGISTRY = 5
    /** 附魔组优先级，用于组织和管理附魔分组 */
    const val GROUP = 6
    /** 限制条件优先级，用于定义附魔的使用限制 */
    const val LIMITATIONS = 7
    /** 玩家数据优先级，用于处理玩家相关的数据 */
    const val PLAYER_DATA = 8
    /** 菜单系统优先级，用于用户界面的菜单系统 */
    const val MENU = 9

    /**
     * 获取数据属性优先级
     *
     * 根据配置 ID 返回对应的优先级值。
     * 只支持品质、附魔组、附魔对象、内置触发器四种类型。
     *
     * @param id 配置项 ID，不区分大小写
     * @return 对应的优先级值，如果 ID 不匹配则返回 -1
     */
    fun getDataProperty(id: String): Int {
        return when (id.lowercase()) {
            "rarity" -> RARITY
            "target" -> TARGET
            "internal_triggers" -> INTERNAL_TRIGGERS
            "group" -> GROUP
            else -> -1
        }
    }

    /**
     * 获取数据生命周期
     *
     * 根据配置 ID 返回对应的生命周期阶段。
     * 稀有度和目标在 LOAD 阶段加载，其他在 ENABLE 阶段加载。
     *
     * @param id 配置项 ID，不区分大小写
     * @return 对应的生命周期阶段
     */
    fun getDataLifeCycle(id: String): LifeCycle {
        return when (id.lowercase()) {
            "rarity", "target" -> LifeCycle.LOAD
            else -> LifeCycle.ENABLE
        }
    }
}