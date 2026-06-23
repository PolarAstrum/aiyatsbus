package cc.polarastrum.aiyatsbus.module.script.fluxon.function

import cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantment
import cc.polarastrum.aiyatsbus.module.script.fluxon.FluxonScriptHandler
import cc.polarastrum.aiyatsbus.module.script.fluxon.relocate.FluxonRelocate
import org.bukkit.inventory.ItemStack
import org.tabooproject.fluxon.runtime.FluxonRuntime
import org.tabooproject.fluxon.runtime.FunctionSignature.returns
import org.tabooproject.fluxon.runtime.Type
import org.tabooproject.fluxon.runtime.java.Export
import taboolib.common.LifeCycle
import taboolib.common.Requires
import taboolib.common.platform.Awake

/**
 * Aiyatsbus
 * cc.polarastrum.aiyatsbus.module.script.fluxon.function.FunctionVariables
 *
 * @author lynn
 * @since 2025/7/5
 */
@Requires(missingClasses = ["!org.tabooproject.fluxon.ParseScript"])
@FluxonRelocate
object FnVariables {

    val TYPE = Type.fromClass(FnVariables::class.java)!!

    @Awake(LifeCycle.LOAD)
    fun init() {
        FluxonScriptHandler.DEFAULT_PACKAGE_AUTO_IMPORT += "aiy:variables"
        with(FluxonRuntime.getInstance()) {
            registerFunction("aiy:variables", "variables", returns(TYPE).noParams()) { it.setReturnRef(FnVariables) }
            exportRegistry.registerClass(FnVariables::class.java, "aiy:variables")
        }
    }

    @Export
    fun ordinary(enchant: AiyatsbusEnchantment, name: String): Any? {
        return enchant.variables.ordinary(name)
    }

    @Export
    fun leveled(enchant: AiyatsbusEnchantment, name: String, level: Int, withUnit: Boolean): Any {
        return enchant.variables.leveled(name, level, withUnit)
    }

    @Export
    fun modifiable(enchant: AiyatsbusEnchantment, item: ItemStack, name: String): Any {
        return enchant.variables.modifiable(name, item)
    }

    @Export
    fun setModifiable(enchant: AiyatsbusEnchantment, item: ItemStack, name: String, value: Any?): ItemStack {
        return enchant.variables.modifyVariable(item, name, value.toString())
    }
}