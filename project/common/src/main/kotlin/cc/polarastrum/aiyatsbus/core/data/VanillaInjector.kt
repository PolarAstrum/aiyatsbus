package cc.polarastrum.aiyatsbus.core.data

import cc.polarastrum.aiyatsbus.core.Aiyatsbus
import cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantment
import cc.polarastrum.aiyatsbus.core.script.ScriptType
import org.bukkit.entity.LivingEntity
import taboolib.common.platform.function.warning
import taboolib.library.configuration.ConfigurationSection

/**
 * Aiyatsbus
 * cc.polarastrum.aiyatsbus.core.data.Injector
 *
 * @author mical
 * @since 2026/4/22 08:49
 */
data class VanillaInjector(
    private val root: ConfigurationSection,
    private val enchant: AiyatsbusEnchantment,
    var enable: Boolean = root.getBoolean("enable", false),
    val before: VanillaInjectionExecutor? = root.getConfigurationSection("before")?.let { VanillaInjectionExecutor(it, enchant) },
    val value: String = root.getString("execute.value", "")!!,
    val after: VanillaInjectionExecutor? = root.getConfigurationSection("after")?.let { VanillaInjectionExecutor(it, enchant) }
)

data class VanillaInjectionExecutor(
    private val root: ConfigurationSection,
    private val enchant: AiyatsbusEnchantment,
    val handle: String = root.getString("handle", "")!!,
    val scriptType: ScriptType = ScriptType.valueOf(root.getString("type") ?: "FLUXON")
) {

    val internalId: String =
        "VanillaEnchantment_" + enchant.basicData.id + "_VanillaInjector_" + root.name

    init {
        try {
            with(Aiyatsbus.api().getScriptHandler().getScriptHandler(scriptType)) {
                preheat(handle, internalId)
            }
        } catch (ex: Throwable) {
            warning("Unable to preheat the vanilla injector (${root.name}) of enchantment ${enchant.id}")
            ex.printStackTrace()
        }
    }

    fun execute(entity: LivingEntity, vars: MutableMap<String, Any?>): Any? {
//        if (!AiyatsbusEnchantmentExecuteEvent(entity, this, type, handle, vars).call()) return
        return Aiyatsbus.api().getScriptHandler().getScriptHandler(scriptType).invoke(handle, internalId, entity, vars)
    }
}