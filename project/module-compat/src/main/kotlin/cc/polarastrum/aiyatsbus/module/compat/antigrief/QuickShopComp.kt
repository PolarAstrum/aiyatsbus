package cc.polarastrum.aiyatsbus.module.compat.antigrief

import cc.polarastrum.aiyatsbus.core.compat.AntiGrief
import cc.polarastrum.aiyatsbus.core.compat.AntiGriefChecker
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.maxgamer.quickshop.QuickShop
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

/**
 * Aiyatsbus
 * cc.polarastrum.aiyatsbus.module.compat.antigrief.QuickShopComp
 *
 * @author mical
 * @since 2025/9/10 22:44
 */
class QuickShopComp : AntiGrief {

    override fun canPlace(player: Player, location: Location): Boolean {
        return (QuickShop.getInstance().shopManager.getShop(location)?.owner ?: return true) == player.uniqueId
    }

    override fun canBreak(player: Player, location: Location): Boolean {
        return (QuickShop.getInstance().shopManager.getShop(location)?.owner ?: return true) == player.uniqueId
    }

    override fun canInteract(player: Player, location: Location): Boolean {
        return (QuickShop.getInstance().shopManager.getShop(location)?.owner ?: return true) == player.uniqueId
    }

    override fun canInteractEntity(player: Player, entity: Entity): Boolean {
        return true
    }

    override fun canDamage(player: Player, entity: Entity): Boolean {
        return true
    }

    override fun getAntiGriefPluginName(): String {
        return "QuickShop"
    }

    companion object {

        @Awake(LifeCycle.ACTIVE)
        fun init() {
            AntiGriefChecker.registerNewCompatibility(QuickShopComp())
        }
    }
}