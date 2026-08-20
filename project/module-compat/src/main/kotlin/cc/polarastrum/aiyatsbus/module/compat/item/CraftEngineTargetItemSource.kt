package cc.polarastrum.aiyatsbus.module.compat.item

import cc.polarastrum.aiyatsbus.core.AiyatsbusSettings
import cc.polarastrum.aiyatsbus.core.data.registry.TargetItemSource
import cc.polarastrum.aiyatsbus.core.data.registry.TargetItemSources
import cc.polarastrum.aiyatsbus.core.data.registry.TargetItemType
import cc.polarastrum.aiyatsbus.core.util.craftEngineEnabled
import net.momirealms.craftengine.bukkit.api.CraftEngineItems
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

/** CraftEngine 物品目标来源。 */
object CraftEngineTargetItemSource : TargetItemSource {

    override val id = "craftengine"

    override fun create(identifier: String, capability: Int?): TargetItemType? {
        if (!AiyatsbusSettings.supportCraftEngine || !craftEngineEnabled) return null
        return CraftEngineTargetItemType(identifier, capability)
    }

    @Awake(LifeCycle.LOAD)
    fun register() {
        TargetItemSources.register(this)
    }
}

private class CraftEngineTargetItemType(
    override val identifier: String,
    override val capability: Int?
) : TargetItemType {

    override val vanillaMaterial: Material? = null

    override fun matches(item: ItemStack): Boolean {
        val actual = runCatching { CraftEngineItems.getCustomItemId(item)?.toString() }.getOrNull()
        return actual.equals(identifier, ignoreCase = true)
    }
}
