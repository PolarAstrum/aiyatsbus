package cc.polarastrum.aiyatsbus.module.script.fluxon.function.game

import cc.polarastrum.aiyatsbus.core.util.Vectors
import cc.polarastrum.aiyatsbus.core.util.checkIfIsNPC
import cc.polarastrum.aiyatsbus.core.util.equippedItems
import cc.polarastrum.aiyatsbus.core.util.isBehind
import cc.polarastrum.aiyatsbus.core.util.placeBlock
import cc.polarastrum.aiyatsbus.core.util.realDamage
import cc.polarastrum.aiyatsbus.module.script.fluxon.FluxonScriptHandler
import cc.polarastrum.aiyatsbus.module.script.fluxon.relocate.FluxonRelocate
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.util.Vector
import org.tabooproject.fluxon.runtime.FluxonRuntime
import org.tabooproject.fluxon.runtime.FunctionSignature.returns
import org.tabooproject.fluxon.runtime.Type
import org.tabooproject.fluxon.runtime.java.Export
import org.tabooproject.fluxon.runtime.java.Optional
import taboolib.common.LifeCycle
import taboolib.common.Requires
import taboolib.common.platform.Awake
import taboolib.library.xseries.XPotion
//import taboolib.module.nms.getI18nName

/**
 * Aiyatsbus
 * cc.polarastrum.aiyatsbus.module.script.fluxon.function.game.FnEntity
 *
 * @author mical
 * @since 2026/1/2 00:09
 */
@Requires(missingClasses = ["!org.tabooproject.fluxon.ParseScript"])
@FluxonRelocate
object FnPlayer {

    val TYPE = Type.fromClass(FnPlayer::class.java)!!

    @Awake(LifeCycle.LOAD)
    fun init() {
        FluxonScriptHandler.DEFAULT_PACKAGE_AUTO_IMPORT += "aiy:player"
        with(FluxonRuntime.getInstance()) {
            registerFunction("aiy:player", "player", returns(TYPE).noParams()) { it.setReturnRef(FnPlayer) }
            exportRegistry.registerClass(FnPlayer::class.java, "aiy:player")
        }
    }

    @Export
    fun placeBlock(player: Player, placedBlock: Block, @Optional itemInHand: ItemStack?): Boolean {
        return player.placeBlock(placedBlock, itemInHand ?: player.itemInHand)
    }
}