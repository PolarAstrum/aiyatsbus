package cc.polarastrum.aiyatsbus.core

import cc.polarastrum.aiyatsbus.core.data.VanillaInjector
import cc.polarastrum.aiyatsbus.core.data.trigger.Mechanism
import taboolib.module.configuration.Configuration
import java.io.File

/**
 * Aiyatsbus
 * cc.polarastrum.aiyatsbus.core.VanillaAiyatsbusEnchantment
 *
 * @author mical
 * @since 2026/4/22 08:50
 */
interface VanillaAiyatsbusEnchantment : InternalAiyatsbusEnchantment {

    val injector: VanillaInjector?
}

class VanillaAiyatsbusEnchantmentBase(
    id: String,
    file: File?,
    config: Configuration
) : InternalAiyatsbusEnchantmentBase(id, file, config), VanillaAiyatsbusEnchantment {

    override val mechanism: Mechanism? = null

    override val injector: VanillaInjector? = config.getConfigurationSection("injector")?.let { VanillaInjector(it, this) }
}