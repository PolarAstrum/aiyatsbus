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
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldguard.WorldGuard
import com.sk89q.worldguard.bukkit.WorldGuardPlugin
import com.sk89q.worldguard.bukkit.util.Entities
import com.sk89q.worldguard.protection.flags.Flags
import com.sk89q.worldguard.protection.flags.StateFlag
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import taboolib.common.LifeCycle
import taboolib.common.Requires
import taboolib.common.platform.Awake
import taboolib.common.util.unsafeLazy

/**
 * Aiyatsbus
 * cc.polarastrum.aiyatsbus.module.compat.antigrief.WorldGuardComp
 *
 * @author mical
 * @since 2025/2/14 14:55
 */
class WorldGuardComp : AntiGrief {

    val container by unsafeLazy {
        WorldGuard.getInstance().platform.regionContainer
    }
    private var placeFlag: StateFlag? = null
    private var breakFlag: StateFlag? = null
    private var interactFlag: StateFlag? = null

    override fun canPlace(player: Player, location: Location): Boolean {
        return container.createQuery()
            .testBuild(
                BukkitAdapter.adapt(location),
                WorldGuardPlugin.inst().wrapPlayer(player),
                placeFlag ?: Flags.BLOCK_PLACE
            )
    }

    override fun canBreak(player: Player, location: Location): Boolean {
        return container.createQuery()
            .testBuild(
                BukkitAdapter.adapt(location),
                WorldGuardPlugin.inst().wrapPlayer(player),
                breakFlag ?: Flags.BLOCK_BREAK
            )
    }

    override fun canInteract(player: Player, location: Location): Boolean {
        return container.createQuery()
            .testBuild(
                BukkitAdapter.adapt(location),
                WorldGuardPlugin.inst().wrapPlayer(player),
                interactFlag ?: Flags.INTERACT
            )
    }

    override fun canInteractEntity(player: Player, entity: Entity): Boolean {
        return container.createQuery()
            .testState(
                BukkitAdapter.adapt(entity.location),
                WorldGuardPlugin.inst().wrapPlayer(player),
                Flags.INTERACT
            )
    }

    override fun canDamage(player: Player, entity: Entity): Boolean {
        return if (Entities.isNPC(entity)) {
            true
        } else {
            container.createQuery()
                .testState(
                    BukkitAdapter.adapt(entity.location),
                    WorldGuardPlugin.inst().wrapPlayer(player),
                    Flags.DAMAGE_ANIMALS
                )
        }
    }

    override fun getAntiGriefPluginName(): String {
        return "WorldGuard"
    }

    @Requires(classes = ["com.sk89q.worldguard.WorldGuard"])
    companion object {

        @Awake(LifeCycle.ACTIVE)
        fun init() {
            AntiGriefChecker.registerNewCompatibility(WorldGuardComp())
        }
    }
} 