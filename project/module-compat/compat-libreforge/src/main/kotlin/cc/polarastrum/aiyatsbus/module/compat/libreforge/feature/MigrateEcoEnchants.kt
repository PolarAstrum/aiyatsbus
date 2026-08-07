package cc.polarastrum.aiyatsbus.module.compat.libreforge.feature

import cc.polarastrum.aiyatsbus.core.AiyatsbusSettings
import cc.polarastrum.aiyatsbus.core.aiyatsbusEt
import cc.polarastrum.aiyatsbus.core.aiyatsbusRarity
import cc.polarastrum.aiyatsbus.module.compat.libreforge.enchant.MissingDependencyException
import com.willfp.eco.core.config.Configs
import com.willfp.eco.util.containsIgnoreCase
import org.bukkit.Bukkit
import taboolib.module.configuration.Configuration
import taboolib.module.configuration.Type
import taboolib.common.platform.function.warning
import taboolib.library.configuration.ConfigurationSection
import java.io.File
import java.util.*

/**
 * AiyatsbusLibreforge
 * com.mcstarrysky.aiyatsbus.libreforge.feature.MigrateEcoEnchants
 *
 * @author mical
 * @date 2024/8/22 15:01
 */
object MigrateEcoEnchants {

    private val rarityTransfer = mapOf(
        "common" to "普通",
        "uncommon" to "罕见",
        "rare" to "精良",
        "epic" to "史诗",
        "legendary" to "传奇",
        "special" to "稀世",
        "veryspecial" to "稀世"
    )

    private val hardcodedEnchantConfigTransfer = mapOf(
        "permanence_curse" to emptyList(),
        "repairing" to listOf("repair-per-level", "frequency", "not-while-holding"),
        "replenish" to listOf("consume-seeds", "only-fully-grown"),
        "soulbound" to listOf("single-use")
    )

    private fun getRarity(id: String, type: String, eco: String): String {
        if (type == "curse") {
            warning("[AiyatsbusLibreforge] EcoEnchants curse enchantment $id's rarity ($eco) has been converted to Aiyatsbus rarity: 诅咒")
            return "诅咒"
        }
        if (eco == "veryspecial") {
            warning("[AiyatsbusLibreforge] EcoEnchants rarity (veryspecial) in enchantment $id has been converted to Aiyatsbus rarity: 稀世")
        }
        if (rarityTransfer[eco] == null || aiyatsbusRarity(rarityTransfer[eco]!!) == null) {
            warning("[AiyatsbusLibreforge] Aiyatsbus rarity ${rarityTransfer[eco]} not found!")
        }
        return (aiyatsbusRarity(rarityTransfer[eco] ?: return eco) ?: return eco).name
    }

    private val target = mapOf(
        "pickaxe" to listOf("镐"),
        "axe" to listOf("斧"),
        "hoe" to listOf("锄"),
        "sword" to listOf("剑"),
        "shovel" to listOf("铲"),
        "helmet" to listOf("头盔"),
        "chestplate" to listOf("胸甲"),
        "leggings" to listOf("护腿"),
        "boots" to listOf("靴子"),
        "armor" to listOf("头盔", "胸甲", "护腿", "靴子"),
        "trident" to listOf("三叉戟"),
        "bow" to listOf("弓"),
        "crossbow" to listOf("弩"),
        "shears" to listOf("剪刀"),
        "shield" to listOf("盾牌"),
        "fishing_rod" to listOf("钓鱼竿"),
        "flint_and_steel" to listOf("打火石"),
        "carrot_on_a_stick" to listOf("萝卜钓竿"),
        "elytra" to listOf("鞘翅"),
        "mace" to listOf("重锤")
    )

    private fun getTarget(id: String, eco: List<String>): List<String> {
        val result = LinkedList<String>()
        for (tar in eco) {
            val aiyatsbus = target[tar]
            if (aiyatsbus == null) {
                warning("[AiyatsbusLibreforge] EcoEnchants target $tar for enchantment $id doesn't not exist in Aiyatsbus!")
                result += tar
                continue
            }
            result += aiyatsbus
        }
        return result
    }

    fun migrate(file: File, id: String, ecoConfig: ConfigurationSection) {
        val config = Configuration.empty(type = Type.YAML, concurrent = false)
        val taboolibEcoConfig = Configuration.loadFromFile(file)
        config["basic.id"] = id
        config["basic.name"] = ecoConfig.getString("display-name")
        config["basic.max-level"] = ecoConfig.getInt("max-level")

        val type = ecoConfig.getString("type")
        config["temp.type"] = type
        config.setComment("temp.type", "Unavailable configuration item")

        config["alternative.is-tradeable"] = ecoConfig.getBoolean("tradeable") ?: true
        config["alternative.is-discoverable"] = ecoConfig.getBoolean("discoverable") ?: true
        config["alternative.is-treasure"] = !(ecoConfig.getBoolean("enchantable") ?: true)

        config["alternative.is-eco"] = true

        if (type == "curse") {
            config["alternative.is-cursed"] = true
            config["alternative.grindstoneable"] = false
        }

        val rarity = getRarity(id, type ?: "", ecoConfig.getString("rarity") ?: "")
        config["rarity"] = rarity
        if (type == "curse") {
            config.setComment("rarity", "Original type: $rarity")
        }
        config["targets"] = getTarget(id, ecoConfig.getStringList("targets"))

        config["limitations"] = ecoConfig.getStringList("conflict").map {
            if (it.lowercase() == "all" || it.lowercase() == "everything") {
                return@map "CONFLICT_ENCHANT:*"
            }
            val name = aiyatsbusEt(it)?.basicData?.name ?: it
            return@map "CONFLICT_ENCHANT:$name"
        }

        config["display.description.general"] = AiyatsbusSettings.ecoEnchantsColorCode + (ecoConfig.getString("description")?.replaceVariables() ?: "")

        config["dependencies"] = ecoConfig.getStringList("dependencies")

        ecoConfig.getString("placeholder")?.run { config["variables.leveled.placeholder"] = ":${this.replaceVariables()}" }

        try {
            for (key in ecoConfig.getConfigurationSection("placeholders")?.getKeys(false) ?: emptyList()) {
                val expr = ecoConfig.getString("placeholders.$key")?.replaceVariables() ?: continue
                config["variables.leveled.$key"] = ":$expr"
            }
        } catch (_: Throwable) {  }

        config["effects"] = taboolibEcoConfig["effects"]
        config["conditions"] = taboolibEcoConfig["conditions"]

        hardcodedEnchantConfigTransfer[id]?.forEach { key ->
            config[key] = taboolibEcoConfig[key]
        }

        config.saveToFile(file)
    }

    private fun String.replaceVariables(): String {
        return replace(Regex("%(\\w+)%"), "{\$1}")
    }

    fun isMissingPlugins(file: File): Boolean {
        val missingPlugins = mutableSetOf<String>()
        val ecoConfig = Configs.fromFile(file)

        for (dependency in ecoConfig.getStrings("dependencies")) {
            if (!Bukkit.getPluginManager().plugins.map { it.name }.containsIgnoreCase(dependency)) {
                missingPlugins += dependency
            }
        }

        if (missingPlugins.isNotEmpty()) {
            warning(MissingDependencyException(missingPlugins).message)
            return true
        }
        return false
    }
}