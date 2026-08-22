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
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import p1xel.nobuildplus.API.NBPAPI
import p1xel.nobuildplus.Flags
import p1xel.nobuildplus.NoBuildPlus
import taboolib.common.LifeCycle
import taboolib.common.Requires
import taboolib.common.platform.Awake
import taboolib.common.util.unsafeLazy
import taboolib.library.reflex.Reflex.Companion.invokeMethod

/**
 * Aiyatsbus
 * cc.polarastrum.aiyatsbus.module.compat.antigrief.NoBuildPlusComp
 *
 * @author mical
 * @since 2025/2/14 14:55
 */
class NoBuildPlusComp : AntiGrief {

    private var api: NoBuildPlusApi

    init {
        try {
            p1xel.nobuildplus.api.NBPAPI::class.java
            api = NoBuildPlusImpl2()
        } catch (_: Throwable) {
            api = NoBuildPlusImpl1()
        }
    }

    private interface NoBuildPlusApi {

        fun canExecute(world: String, flags: Flags): Boolean
    }

    private class NoBuildPlusImpl1 : NoBuildPlusApi {

        private val api by unsafeLazy {
            NoBuildPlus.getInstance().invokeMethod<NBPAPI>("getAPI")!!
        }

        override fun canExecute(world: String, flags: Flags): Boolean {
            return api.canExecute(world, flags)
        }
    }

    private class NoBuildPlusImpl2 : NoBuildPlusApi {

        private val api by unsafeLazy {
            NoBuildPlus.getInstance().api
        }

        override fun canExecute(world: String, flags: Flags): Boolean {
            return api.canExecute(world, flags)
        }
    }

    override fun canPlace(player: Player, location: Location): Boolean {
        return !api.canExecute(player.world.name, Flags.build)
    }

    override fun canBreak(player: Player, location: Location): Boolean {
        return !api.canExecute(player.world.name, Flags.destroy)
    }

    override fun canInteract(player: Player, location: Location): Boolean {
        return !api.canExecute(player.world.name, Flags.use)
    }

    override fun canInteractEntity(player: Player, entity: Entity): Boolean {
        val world = player.world.name
        return !when (entity.type.name.uppercase()) {
            "VILLAGER" -> api.canExecute(world, Flags.villager)
            "HORSE", "DONKEY", "MULE", "SKELETON_HORSE", "ZOMBIE_HORSE", "MINECART", "MINECART_CHEST", "MINECART_FURNACE", "MINECART_HOPPER", "MINECART_TNT" -> api.canExecute(
                world,
                Flags.ride
            )

            "ITEM_FRAME", "GLOW_ITEM_FRAME" -> api.canExecute(world, Flags.frame)
            "ARMOR_STAND" -> api.canExecute(world, Flags.armorstand)
            "PAINTING" -> api.canExecute(world, Flags.painting)
            "FISHING_HOOK" -> api.canExecute(world, Flags.hook)
            else -> false
        }
    }

    override fun canDamage(player: Player, entity: Entity): Boolean {
        return !api.canExecute(
            player.world.name,
            if (entity is Player) Flags.pvp else Flags.mob_damage
        )
    }

    override fun getAntiGriefPluginName(): String {
        return "NoBuildPlus"
    }

    @Requires(classes = ["p1xel.nobuildplus.API.NBPAPI"])
    companion object {

        @Awake(LifeCycle.ACTIVE)
        fun init() {
            AntiGriefChecker.registerNewCompatibility(NoBuildPlusComp())
        }
    }
} 