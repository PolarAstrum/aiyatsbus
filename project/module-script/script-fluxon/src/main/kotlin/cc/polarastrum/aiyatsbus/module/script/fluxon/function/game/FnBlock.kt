package cc.polarastrum.aiyatsbus.module.script.fluxon.function.game

import cc.polarastrum.aiyatsbus.core.util.BlockUtils
import cc.polarastrum.aiyatsbus.module.script.fluxon.FluxonScriptHandler
import cc.polarastrum.aiyatsbus.module.script.fluxon.relocate.FluxonRelocate
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import org.tabooproject.fluxon.runtime.FluxonRuntime
import org.tabooproject.fluxon.runtime.FunctionSignature.returns
import org.tabooproject.fluxon.runtime.Type
import org.tabooproject.fluxon.runtime.java.Export
import org.tabooproject.fluxon.runtime.java.Optional
import taboolib.common.LifeCycle
import taboolib.common.Requires
import taboolib.common.platform.Awake

/**
 * Aiyatsbus
 * cc.polarastrum.aiyatsbus.module.script.fluxon.function.game.FnBlock
 *
 * @author mical
 * @since 2026/1/1 23:56
 */
@Requires(missingClasses = ["!org.tabooproject.fluxon.ParseScript"])
@FluxonRelocate
object FnBlock {

    val TYPE = Type.fromClass(FnBlock::class.java)!!

    @Awake(LifeCycle.LOAD)
    fun init() {
        FluxonScriptHandler.DEFAULT_PACKAGE_AUTO_IMPORT += "aiy:block"
        with(FluxonRuntime.getInstance()) {
            registerFunction("aiy:block", "block", returns(TYPE).noParams()) { it.setReturnRef(FnBlock) }
            exportRegistry.registerClass(FnBlock::class.java, "aiy:block")
        }
    }

    @Export
    fun getDrops(block: Block?, @Optional item: ItemStack?, @Optional entity: Entity?): Collection<ItemStack> {
        return block?.getDrops(item, entity) ?: emptyList()
    }

    @Export
    fun getVein(block: Block, @Optional amount: Int?): List<Location> {
        return BlockUtils.getVein(block, amount ?: -1)
    }
}