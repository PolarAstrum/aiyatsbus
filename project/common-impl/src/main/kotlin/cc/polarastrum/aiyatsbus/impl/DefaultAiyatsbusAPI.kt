package cc.polarastrum.aiyatsbus.impl

import cc.polarastrum.aiyatsbus.core.*
import cc.polarastrum.aiyatsbus.core.registration.AiyatsbusEnchantmentRegisterer
import cc.polarastrum.aiyatsbus.core.script.AiyatsbusScriptHandler
import taboolib.common.platform.PlatformFactory
import taboolib.common.util.t
import taboolib.module.nms.nmsProxy
import java.util.concurrent.CompletableFuture

/**
 * Aiyatsbus
 * com.mcstarrysky.aiyatsbus.impl.DefaultAiyatsbusAPI
 *
 * @author mical
 * @since 2024/2/17 16:20
 */
class DefaultAiyatsbusAPI : AiyatsbusAPI {

    private val artifactHandler = PlatformFactory.getAPI<AiyatsbusArtifactHandler>()

    private val enchantmentFilter = PlatformFactory.getAPI<AiyatsbusEnchantmentFilter>()

    private val enchantmentManager = PlatformFactory.getAPI<AiyatsbusEnchantmentManager>()

    private val eventExecutor = PlatformFactory.getAPI<AiyatsbusEventExecutor>()

    private val displayManager = PlatformFactory.getAPI<AiyatsbusDisplayManager>()

    private val language = PlatformFactory.getAPI<AiyatsbusLanguage>()

    private val playerDataHandler = PlatformFactory.getAPI<AiyatsbusPlayerDataHandler>()

    private val scriptHandler = PlatformFactory.getAPI<AiyatsbusScriptHandler>()

    private val minecraftAPI0 by lazy {
        proxy<AiyatsbusMinecraftAPI>("cc.polarastrum.aiyatsbus.impl.DefaultAiyatsbusMinecraftAPI")
    }

    private val skillHandler = PlatformFactory.getAPI<AiyatsbusSkillHandler>()

    private val tickHandler = PlatformFactory.getAPI<AiyatsbusTickHandler>()

    override fun getArtifactHandler(): AiyatsbusArtifactHandler {
        return artifactHandler
    }

    override fun getEnchantmentFilter(): AiyatsbusEnchantmentFilter {
        return enchantmentFilter
    }

    override fun getEnchantmentManager(): AiyatsbusEnchantmentManager {
        return enchantmentManager
    }

    override fun getEnchantmentRegisterer(): AiyatsbusEnchantmentRegisterer {
        return registerer
    }

    override fun getMinecraftAPI(): AiyatsbusMinecraftAPI {
        return minecraftAPI0
    }

    override fun getDisplayManager(): AiyatsbusDisplayManager {
        return displayManager
    }

    override fun getEventExecutor(): AiyatsbusEventExecutor {
        return eventExecutor
    }

    override fun getLanguage(): AiyatsbusLanguage {
        return language
    }

    override fun getPlayerDataHandler(): AiyatsbusPlayerDataHandler {
        return playerDataHandler
    }

    override fun getScriptHandler(): AiyatsbusScriptHandler {
        return scriptHandler
    }

    override fun getSkillHandler(): AiyatsbusSkillHandler {
        return skillHandler
    }

    override fun getTickHandler(): AiyatsbusTickHandler {
        return tickHandler
    }

    init {
        CompletableFuture.runAsync {
            minecraftAPI0
        }
    }

    companion object {

        lateinit var registerer: AiyatsbusEnchantmentRegisterer

        inline fun <reified T> proxy(bind: String, vararg parameter: Any): T {
            val time = System.currentTimeMillis()
            val proxy = nmsProxy(T::class.java, bind, *parameter)
            val cost = System.currentTimeMillis() - time
            println("""
            [Aiyatsbus] 代理类 ${T::class.java.simpleName} 已生成，用时 $cost 毫秒。
            [Aiyatsbus] Generated ${T::class.java.simpleName} in ${System.currentTimeMillis() - time}ms
        """.t())
            return proxy
        }
    }
}