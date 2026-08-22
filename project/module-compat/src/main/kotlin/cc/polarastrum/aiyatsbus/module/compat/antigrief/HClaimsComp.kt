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
import com.hakan.claim.ClaimPlugin
import com.hakan.claim.model.member.ClaimMemberPermission
import com.hakan.claim.service.ClaimService
import com.hakan.claim.shadow.com.hakan.spinjection.SpigotBootstrap
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import taboolib.common.LifeCycle
import taboolib.common.Requires
import taboolib.common.platform.Awake
import taboolib.common.util.unsafeLazy
import kotlin.jvm.optionals.getOrNull

/**
 * Aiyatsbus
 * cc.polarastrum.aiyatsbus.module.compat.antigrief.HClaimsComp
 *
 * @author mical
 * @since 2025/2/14 14:55
 */
class HClaimsComp : AntiGrief {

    val service by unsafeLazy {
        SpigotBootstrap.of(ClaimPlugin::class.java)
            .injector.getInstance(ClaimService::class.java)
    }

    override fun canPlace(player: Player, location: Location): Boolean {
        return service.findByLocation(location).getOrNull()
            ?.hasPermission(player, ClaimMemberPermission.BLOCK_PLACE) ?: true
    }

    override fun canBreak(player: Player, location: Location): Boolean {
        return service.findByLocation(location).getOrNull()
            ?.hasPermission(player, ClaimMemberPermission.BLOCK_BREAK) ?: true
    }

    override fun canInteract(player: Player, location: Location): Boolean {
        return service.findByLocation(location).getOrNull()
            ?.hasPermission(player, ClaimMemberPermission.INTERACT) ?: true
    }

    override fun canInteractEntity(player: Player, entity: Entity): Boolean {
        return service.findByLocation(entity.location).getOrNull()
            ?.hasPermission(player, ClaimMemberPermission.INTERACT) ?: true
    }

    override fun canDamage(player: Player, entity: Entity): Boolean {
        return service.findByLocation(entity.location).getOrNull()
            ?.hasPermission(player, ClaimMemberPermission.DAMAGE_ANIMALS) ?: true
    }

    override fun getAntiGriefPluginName(): String {
        return "hClaims"
    }

    @Requires(classes = ["com.hakan.claim.ClaimPlugin"])
    companion object {

        @Awake(LifeCycle.ACTIVE)
        fun init() {
            AntiGriefChecker.registerNewCompatibility(HClaimsComp())
        }
    }
} 