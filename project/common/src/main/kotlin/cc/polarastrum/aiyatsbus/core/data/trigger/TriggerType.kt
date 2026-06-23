package cc.polarastrum.aiyatsbus.core.data.trigger

/**
 * 触发器类型枚举
 *
+ * 定义附魔机制的三类触发方式：事件监听器、定时器与技能触发。
 * - `LISTENER`：依赖 Bukkit 事件触发。
 * - `TICKER`：按固定时间间隔触发。
 * - `SKILL`：由玩家动作触发的技能型事件。
 *
 * @author mical
 * @since 2026/2/8 21:19
 */
enum class TriggerType(
    val isUnique: Boolean = false
) {

    /** 粒子效果 */
    ARTIFACT(true),

    /** 事件监听器触发 */
    LISTENER,

    /** 定时器触发 */
    TICKER,

    /** 技能动作触发 */
    SKILL,

    /** 内置触发器触发，可用代码编写，包含了事件触发和定时触发 */
    BUILTIN(true)
}
