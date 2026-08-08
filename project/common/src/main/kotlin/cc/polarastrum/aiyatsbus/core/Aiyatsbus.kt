package cc.polarastrum.aiyatsbus.core

/**
 * Aiyatsbus 核心对象
 *
 * Aiyatsbus 插件的核心对象，负责管理 API 实例的注册和获取。
 * 提供全局访问点来获取 AiyatsbusAPI 实例。
 *
 * @author mical
 * @since 2024/2/17 15:31
 */
object Aiyatsbus {

    /** API 实例 */
    private var api: AiyatsbusAPI? = null

    /** 是否正在重载 */
    var isRestarting: Boolean = false

    /**
     * 获取 API 实例
     *
     * @return AiyatsbusAPI 实例
     * @throws IllegalStateException 如果 API 未加载或加载失败
     */
    fun api(): AiyatsbusAPI {
        return api ?: error("AiyatsbusAPI has not finished loading, or failed to load!")
    }

    /**
     * 注册 API 实例
     *
     * @param api 要注册的 API 实例
     */
    fun register(api: AiyatsbusAPI) {
        Aiyatsbus.api = api
    }
}