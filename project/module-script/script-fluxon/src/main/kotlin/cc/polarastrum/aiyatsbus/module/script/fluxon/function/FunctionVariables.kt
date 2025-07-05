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
object FunctionVariables {

    fun init(runtime: FluxonRuntime) {
        runtime.registerFunction("ordinary", 2) { target, args ->
            val enchant = args[0] as AiyatsbusEnchantment
            val name = args[1] as String
            enchant.variables.ordinary(name)
        }

        runtime.registerFunction("leveled", 4) { target, args ->
            val enchant = args[0] as AiyatsbusEnchantment
            val name = args[1] as String
            val level = args[2] as Int
            val withUnit = args[3] as Boolean
            enchant.variables.leveled(name, level, withUnit)
        }

        runtime.registerFunction("modifiable", 3) { target, args ->
            val enchant = args[0] as AiyatsbusEnchantment
            val item = args[1] as ItemStack
            val name = args[2] as String
            enchant.variables.modifiable(name, item)
        }

        runtime.registerFunction("setModifiable", 4) { target, args ->
            val enchant = args[0] as AiyatsbusEnchantment
            val item = args[1] as ItemStack
            val name = args[2] as String
            val value = args[3]
            enchant.variables.modifyVariable(item, name, value.toString())
        }
    }
}