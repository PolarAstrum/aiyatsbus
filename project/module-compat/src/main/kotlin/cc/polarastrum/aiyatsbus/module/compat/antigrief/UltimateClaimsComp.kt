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
import com.craftaro.ultimateclaims.UltimateClaims
import com.craftaro.ultimateclaims.member.ClaimPerm
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import taboolib.common.LifeCycle
import taboolib.common.Requires
import taboolib.common.platform.Awake
import taboolib.common.util.unsafeLazy

/**
 * Aiyatsbus
 * cc.polarastrum.aiyatsbus.module.compat.antigrief.UltimateClaimsComp
 *
 * @author mical
 * @since 2025/2/14 14:55
 */
class UltimateClaimsComp : AntiGrief {

    val ultimateClaims by unsafeLazy {
        Bukkit.getPluginManager().getPlugin("UltimateClaims") as UltimateClaims
    }

    override fun canPlace(player: Player, location: Location): Boolean {
        return ultimateClaims.claimManager.getClaim(location.chunk)
            ?.playerHasPerms(player, ClaimPerm.PLACE) ?: true
    }

    override fun canBreak(player: Player, location: Location): Boolean {
        return ultimateClaims.claimManager.getClaim(location.chunk)
            ?.playerHasPerms(player, ClaimPerm.BREAK) ?: true
    }

    override fun canInteract(player: Player, location: Location): Boolean {
        return ultimateClaims.claimManager.getClaim(location.chunk)
            ?.playerHasPerms(player, ClaimPerm.INTERACT) ?: true
    }

    override fun canInteractEntity(player: Player, entity: Entity): Boolean {
        return ultimateClaims.claimManager.getClaim(entity.location.chunk)
            ?.playerHasPerms(player, ClaimPerm.INTERACT) ?: true
    }

    override fun canDamage(player: Player, entity: Entity): Boolean {
        return ultimateClaims.claimManager.getClaim(entity.location.chunk)
            ?.playerHasPerms(player, ClaimPerm.INTERACT) ?: true
    }

    override fun getAntiGriefPluginName(): String {
        return "UltimateClaims"
    }

    @Requires(classes = ["com.craftaro.ultimateclaims.UltimateClaims"])
    companion object {

        @Awake(LifeCycle.ACTIVE)
        fun init() {
            AntiGriefChecker.registerNewCompatibility(UltimateClansComp())
        }
    }
} 