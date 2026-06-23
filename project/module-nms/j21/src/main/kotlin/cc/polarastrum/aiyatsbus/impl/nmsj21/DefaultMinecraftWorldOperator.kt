package cc.polarastrum.aiyatsbus.impl.nmsj21

import cc.polarastrum.aiyatsbus.core.MinecraftWorldOperator
import net.minecraft.core.BlockPos
import org.bukkit.ChatColor
import org.bukkit.block.Block
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Entity
import org.bukkit.entity.Player

/**
 * Aiyatsbus
 * cc.polarastrum.aiyatsbus.impl.nms.nms.DefaultMinecraftWorldOperator
 *
 * @author mical
 * @since 2025/8/16 08:52
 */
class DefaultMinecraftWorldOperator : MinecraftWorldOperator {

    init {
        NMSGlow.instance
    }

    override fun breakBlock(player: Player, block: Block): Boolean {
        return (player as CraftPlayer).handle.gameMode.destroyBlock(BlockPos(block.x, block.y, block.z))
    }

    override fun setEntityGlowing(entity: Entity, receiver: Player, color: ChatColor) {
        return NMSGlow.instance.setEntityGlowing(entity, receiver, color)
    }

    override fun unsetEntityGlowing(entity: Entity, receiver: Player) {
        return NMSGlow.instance.unsetEntityGlowing(entity, receiver)
    }

    override fun setBlockGlowing(block: Block, receiver: Player, color: ChatColor) {
        return NMSGlow.instance.setBlockGlowing(block, receiver, color)
    }

    override fun unsetBlockGlowing(block: Block, receiver: Player) {
        return NMSGlow.instance.unsetBlockGlowing(block, receiver)
    }
}