package cc.polarastrum.aiyatsbus.module.script.fluxon.function

import cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantment
import org.bukkit.inventory.ItemStack
import org.tabooproject.fluxon.runtime.FluxonRuntime

/**
 * Aiyatsbus
 * cc.polarastrum.aiyatsbus.module.script.fluxon.function.FunctionVariables
 *
 * @author lynn
 * @since 2025/7/5
 */
object FnVariables {

    fun init() {
        with(FluxonRuntime.getInstance()) {
            registerFunction("ordinary", 2) { context ->
                val enchant = context.arguments[0] as AiyatsbusEnchantment
                val name = context.arguments[1] as String
                enchant.variables.ordinary(name)
            }

            registerFunction("leveled", 4) { context ->
                val enchant = context.arguments[0] as AiyatsbusEnchantment
                val name = context.arguments[1] as String
                val level = context.arguments[2] as Int
                val withUnit = context.arguments[3] as Boolean
                enchant.variables.leveled(name, level, withUnit)
            }

            registerFunction("modifiable", 3) { context ->
                val enchant = context.arguments[0] as AiyatsbusEnchantment
                val item = context.arguments[1] as ItemStack
                val name = context.arguments[2] as String
                enchant.variables.modifiable(name, item)
            }

            registerFunction("setModifiable", 4) { context ->
                val enchant = context.arguments[0] as AiyatsbusEnchantment
                val item = context.arguments[1] as ItemStack
                val name = context.arguments[2] as String
                val value = context.arguments[3]
                enchant.variables.modifyVariable(item, name, value.toString())
            }
        }
    }
}