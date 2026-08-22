package cc.polarastrum.aiyatsbus.module.bukkit

import cc.polarastrum.aiyatsbus.impl.DefaultAiyatsbusBooster
import taboolib.common.LifeCycle
import taboolib.common.UnsupportedVersionException
import taboolib.common.platform.Plugin
import taboolib.common.platform.function.disablePlugin
import taboolib.common.platform.function.registerLifeCycleTask
import taboolib.common.util.t
import taboolib.module.nms.MinecraftVersion

/**
 * Aiyatsbus Bukkit 插件主类
 * 
 * 负责插件的生命周期管理，在初始化阶段启动 Aiyatsbus 系统。
 * 如果启动失败，会自动禁用插件并输出错误信息。
 *
 * @author mical
 * @since 2024/2/17 15:25
 */
object AiyatsbusPlugin : Plugin() {

    init {
        registerLifeCycleTask(LifeCycle.INIT) {
            if (!MinecraftVersion.isUniversal) {
                error(
                    """
                Aiyatsbus 仅支持 1.17 及以上版本。
                Aiyatsbus only supports 1.17 or above.
            """.t()
                )
                throw UnsupportedVersionException()
                disablePlugin()
            } else {
                // 在初始化阶段启动 Aiyatsbus 系统
                // 必须是 INIT 生命周期启动, 写到其他生命周期用不了
                registerLifeCycleTask(LifeCycle.INIT) {
                    try {
                        DefaultAiyatsbusBooster.startup()
                    } catch (ex: Throwable) {
                        ex.printStackTrace()
                        disablePlugin()
                    }
                }
            }
        }
    }
}