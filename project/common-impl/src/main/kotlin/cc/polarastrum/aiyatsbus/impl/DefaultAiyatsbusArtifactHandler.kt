package cc.polarastrum.aiyatsbus.impl

import cc.polarastrum.aiyatsbus.core.*
import cc.polarastrum.aiyatsbus.impl.DefaultAiyatsbusSkillHandler.AiyatsbusSkillSettings.conf
import org.bukkit.inventory.EquipmentSlot
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory
import taboolib.common.platform.function.console
import taboolib.common.platform.function.registerLifeCycleTask
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.configuration.Config
import taboolib.module.configuration.ConfigNode
import taboolib.module.configuration.Configuration
import taboolib.module.configuration.conversion

/**
 * Aiyatsbus
 * cc.polarastrum.aiyatsbus.impl.DefaultAiyatsbusSkillHandler
 *
 * @author mical
 * @since 2026/1/27 18:07
 */
class DefaultAiyatsbusArtifactHandler : AiyatsbusArtifactHandler {

    override fun getSettings(): AiyatsbusArtifactHandler.Settings {
        return AiyatsbusArtifactSettings
    }

    @ConfigNode(bind = "enchants/artifact.yml")
    object AiyatsbusArtifactSettings : AiyatsbusArtifactHandler.Settings {

        @Config("enchants/artifact.yml", autoReload = true)
        override lateinit var conf: Configuration

        @ConfigNode("normal.range")
        override var range: List<Double> = listOf()

        @ConfigNode("normal.height")
        override var height: List<Double> = listOf()

        @delegate:ConfigNode("blocks")
        override val blocks: List<String> by conversion<List<String>, List<String>> {
            map { it.uppercase() }
        }

        @delegate:ConfigNode("specialized")
        override val customParticleData: Map<EquipmentSlot, Pair<Double, Double>> by conversion<ConfigurationSection, Map<EquipmentSlot, Pair<Double, Double>>> {
            getKeys(false).associate { EquipmentSlot.valueOf(it) to (getDouble("$it.range", 0.0) to getDouble("$it.height", 0.0)) }
        }

        @delegate:ConfigNode("specialized")
        override val customParticleType: Map<EquipmentSlot, String> by conversion<ConfigurationSection, Map<EquipmentSlot, String>> {
            getKeys(false).associate { EquipmentSlot.valueOf(it) to getString("$it.type")!! }
        }
    }

    companion object {

        @Awake(LifeCycle.CONST)
        fun init() {
            PlatformFactory.registerAPI<AiyatsbusArtifactHandler>(DefaultAiyatsbusArtifactHandler())
            registerLifeCycleTask(LifeCycle.ENABLE) {
                conf.onReload {
                    console().sendLang("configuration-reload", conf.file!!.name, 0)
                }
            }
        }
    }
}