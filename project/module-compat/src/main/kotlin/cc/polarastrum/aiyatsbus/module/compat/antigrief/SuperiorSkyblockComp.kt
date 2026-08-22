/*
 * This file is part of AntiGriefLib, licensed under the MIT License.
 *
 *  Copyright (c) 2024 XiaoMoMi
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.

 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */
package cc.polarastrum.aiyatsbus.module.compat.antigrief

import cc.polarastrum.aiyatsbus.core.compat.AntiGrief
import cc.polarastrum.aiyatsbus.core.compat.AntiGriefChecker
import com.bgsoftware.superiorskyblock.SuperiorSkyblockPlugin
import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI
import com.bgsoftware.superiorskyblock.api.island.Island
import com.bgsoftware.superiorskyblock.api.island.IslandPrivilege
import com.bgsoftware.superiorskyblock.api.service.region.RegionManagerService
import com.bgsoftware.superiorskyblock.service.region.ProtectionHelper
import com.bgsoftware.superiorskyblock.world.BukkitEntities
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import taboolib.common.LifeCycle
import taboolib.common.Requires
import taboolib.common.platform.Awake
import taboolib.common.util.unsafeLazy
import java.util.*

/**
 * Aiyatsbus
 * cc.polarastrum.aiyatsbus.module.compat.antigrief.SuperiorSkyblockComp
 *
 * @author mical
 * @since 2025/2/14 14:55
 */
class SuperiorSkyblockComp : AntiGrief {

    private var api: SuperiorSkyblockApi

    init {
        try {
            BukkitEntities.getCategory(EntityType.ZOMBIE)
            api = SuperiorSkyblockImpl23()
        } catch (_: Throwable) {
            api = SuperiorSkyblockImpl26()
        }
    }

    private interface SuperiorSkyblockApi {

        fun canDamage(player: Player, entity: Entity): Boolean
    }

    // 不太想改变以前 api 的调用方法
    private class SuperiorSkyblockImpl23 : SuperiorSkyblockApi {

        override fun canDamage(player: Player, entity: Entity): Boolean {
            val category = BukkitEntities.getCategory(entity.type)
            val plugin = SuperiorSkyblockPlugin.getPlugin(
                SuperiorSkyblockPlugin::class.java
            )
            return Optional.ofNullable<Island>(SuperiorSkyblockAPI.getIslandAt(entity.location))
                .map { island ->
                    var banPvp = false
                    if (entity is Player) {
                        val targetPlayer = plugin.players.getSuperiorPlayer(entity)
                        if (category == BukkitEntities.EntityCategory.UNKNOWN) {
                            banPvp =
                                if (island.isSpawn) (plugin.settings.spawn.isProtected && !plugin.settings.spawn.isPlayersDamage) else ((!plugin.settings.isVisitorsDamage && island.isVisitor(
                                    targetPlayer,
                                    false
                                )) ||
                                        (!plugin.settings.isCoopDamage && island.isCoop(targetPlayer)))
                        }
                    }
                    if (entity is Player) !banPvp else island.hasPermission(
                        SuperiorSkyblockAPI.getPlayer(
                            player
                        ), category.damagePrivilege
                    )
                }
                .orElse(true)
        }
    }

    // 这样调用应该是更优解
    private class SuperiorSkyblockImpl26 : SuperiorSkyblockApi {

        private val plugin by unsafeLazy {
            SuperiorSkyblockPlugin.getPlugin(
                SuperiorSkyblockPlugin::class.java
            )
        }

        private val protectionManager by unsafeLazy { plugin.services.getService(RegionManagerService::class.java) }

        override fun canDamage(player: Player, entity: Entity): Boolean {
            // ProtectionListener
            val damagerSource = BukkitEntities.getPlayerSource(player)
                .map { plugin.players.getSuperiorPlayer(it) }.orElse(null)
            val interactionResult = protectionManager.handleEntityDamage(player, entity)
            return !ProtectionHelper.shouldPreventInteraction(interactionResult, damagerSource, false)
        }
    }

    override fun canPlace(player: Player, location: Location): Boolean {
        return SuperiorSkyblockAPI.getIslandAt(location)
            ?.hasPermission(SuperiorSkyblockAPI.getPlayer(player), IslandPrivilege.getByName("BUILD")) ?: true
    }

    override fun canBreak(player: Player, location: Location): Boolean {
        return SuperiorSkyblockAPI.getIslandAt(location)
            ?.hasPermission(SuperiorSkyblockAPI.getPlayer(player), IslandPrivilege.getByName("BREAK")) ?: true
    }

    override fun canInteract(player: Player, location: Location): Boolean {
        return SuperiorSkyblockAPI.getIslandAt(location)
            ?.hasPermission(SuperiorSkyblockAPI.getPlayer(player), IslandPrivilege.getByName("INTERACT")) ?: true
    }

    override fun canInteractEntity(player: Player, entity: Entity): Boolean {
        return SuperiorSkyblockAPI.getIslandAt(entity.location)
            ?.hasPermission(SuperiorSkyblockAPI.getPlayer(player), IslandPrivilege.getByName("INTERACT")) ?: true
    }

    override fun canDamage(player: Player, entity: Entity): Boolean {
        return api.canDamage(player, entity)
    }

    override fun getAntiGriefPluginName(): String {
        return "SuperiorSkyblock2"
    }

    @Requires(classes = ["com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI"])
    companion object {

        @Awake(LifeCycle.ACTIVE)
        fun init() {
            AntiGriefChecker.registerNewCompatibility(SuperiorSkyblockComp())
        }
    }
} 