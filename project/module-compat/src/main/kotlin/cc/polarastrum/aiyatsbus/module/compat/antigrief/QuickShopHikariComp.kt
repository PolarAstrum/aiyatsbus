package cc.polarastrum.aiyatsbus.module.compat.antigrief

import cc.polarastrum.aiyatsbus.core.compat.AntiGrief
import cc.polarastrum.aiyatsbus.core.compat.AntiGriefChecker
import com.ghostchu.quickshop.api.QuickShopAPI
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

/**
 * Aiyatsbus
 * cc.polarastrum.aiyatsbus.module.compat.antigrief.QuickShopComp
 *
 * @author mical
 * @since 2025/9/10 22:44
 */
class QuickShopHikariComp : AntiGrief {

    override fun canPlace(player: Player, location: Location): Boolean {
        return (QuickShopAPI.getInstance().shopManager.getShop(location)?.owner ?: return true) == player
    }

    override fun canBreak(player: Player, location: Location): Boolean {
        return (QuickShopAPI.getInstance().shopManager.getShop(location)?.owner ?: return true) == player
    }

    override fun canInteract(player: Player, location: Location): Boolean {
        return (QuickShopAPI.getInstance().shopManager.getShop(location)?.owner ?: return true) == player
    }

    override fun canInteractEntity(player: Player, entity: Entity): Boolean {
        return true
    }

    override fun canDamage(player: Player, entity: Entity): Boolean {
        return true
    }

    override fun getAntiGriefPluginName(): String {
        return "QuickShop-Hikari"
    }

    companion object {

        @Awake(LifeCycle.ACTIVE)
        fun init() {
            AntiGriefChecker.registerNewCompatibility(QuickShopHikariComp())
        }
    }
}