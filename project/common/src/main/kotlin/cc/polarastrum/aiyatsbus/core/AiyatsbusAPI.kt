package cc.polarastrum.aiyatsbus.core

import cc.polarastrum.aiyatsbus.core.registration.AiyatsbusEnchantmentRegisterer
import cc.polarastrum.aiyatsbus.core.script.AiyatsbusScriptHandler

/**
 * Aiyatsbus API 接口
 *
 * 定义 Aiyatsbus 系统的核心 API 功能。
 * 提供附魔管理、注册和语言系统的访问接口。
 * 这是插件的主要入口点，所有核心功能都通过此接口访问。
 *
 * @author mical
 * @since 2024/2/17 15:31
 */
interface AiyatsbusAPI {

    /**
     * 获取粒子处理器
     *
     * 负责处理与粒子触发相关的全部逻辑。
     *
     * @return 粒子处理器实例
     */
    fun getArtifactHandler(): AiyatsbusArtifactHandler

    /**
     * 获取附魔展示管理器
     *
     * 负责管理附魔的显示效果，包括物品名称、描述、光效等。
     *
     * @return 附魔展示管理器实例
     */
    fun getDisplayManager(): AiyatsbusDisplayManager

    /**
     * 获取附魔过滤器
     *
     * 负责过滤和管理附魔的可见性和可用性。
     *
     * @return 附魔过滤器实例
     */
    fun getEnchantmentFilter(): AiyatsbusEnchantmentFilter

    /**
     * 获取附魔管理器
     *
     * 负责附魔的注册、加载、查询和管理。
     *
     * @return 附魔管理器实例，负责附魔的注册、加载和查询
     */
    fun getEnchantmentManager(): AiyatsbusEnchantmentManager

    /**
     * 获取附魔注册器
     *
     * 负责附魔的注册和注销操作。
     *
     * @return 附魔注册器实例，负责附魔的注册和注销
     */
    fun getEnchantmentRegisterer(): AiyatsbusEnchantmentRegisterer

    /**
     * 获取事件执行器
     *
     * 负责处理附魔相关的事件触发和执行。
     *
     * @return 事件执行器实例
     */
    fun getEventExecutor(): AiyatsbusEventExecutor

    /**
     * 获取语言系统
     *
     * 负责多语言支持和文本管理。
     *
     * @return 语言系统实例，负责多语言支持和文本管理
     */
    fun getLanguage(): AiyatsbusLanguage

    /**
     * 获取 Minecraft API 接口
     *
     * 提供与 Minecraft 内部系统的交互接口。
     *
     * @return Minecraft API 实例
     */
    fun getMinecraftAPI(): AiyatsbusMinecraftAPI

    /**
     * 获取玩家数据处理器
     *
     * 负责管理玩家的附魔数据和状态。
     *
     * @return 玩家数据处理器实例
     */
    fun getPlayerDataHandler(): AiyatsbusPlayerDataHandler

    /**
     * 获取脚本处理器
     *
     * 负责处理附魔相关的脚本执行。
     *
     * @return 脚本处理器实例
     */
    fun getScriptHandler(): AiyatsbusScriptHandler

    /**
     * 获取技能处理器
     *
     * 负责处理与技能触发、冷却、事件注册相关的全部逻辑。
     *
     * @return 技能处理器实例
     */
    fun getSkillHandler(): AiyatsbusSkillHandler

    /**
     * 获取附魔调度器
     *
     * 负责管理附魔的定时任务和调度。
     *
     * @return 附魔调度器实例
     */
    fun getTickHandler(): AiyatsbusTickHandler
}
