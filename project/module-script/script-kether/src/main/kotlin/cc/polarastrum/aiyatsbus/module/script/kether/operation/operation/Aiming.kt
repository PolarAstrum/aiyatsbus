/*
 *  Copyright (C) 2022-2024 PolarAstrumLab
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cc.polarastrum.aiyatsbus.module.script.kether.operation.operation

import cc.polarastrum.aiyatsbus.core.util.*
import org.bukkit.ChatColor
import org.bukkit.entity.*
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.util.Vector
import taboolib.platform.util.submit

/**
 * Aiyatsbus
 * com.mcstarrysky.aiyatsbus.module.kether.action.operation.Aiming
 *
 * @author mical
 * @since 2024/5/19 12:14
 */
object Aiming {

    @Suppress("UNCHECKED_CAST")
    fun shootBow(args: List<Any?>?) {
        shootBow(
            args?.get(0).coerceDouble(),
            args?.get(1).coerceLong(),
            args?.get(2) as EntityShootBowEvent,
            ChatColor.values().firstOrNull { it.name.lowercase() == args[3].toString().lowercase() } ?: ChatColor.WHITE,
            args[4] as List<String>
        )
    }

    private fun shootBow(range: Double, ticks: Long, event: EntityShootBowEvent, color: ChatColor, blackList: List<String>) {
        if (event.projectile is AbstractArrow) {
            val player = event.entity
            var minAng = 6.28f
            player.submit {
                var minEntity: Entity? = null
                for (entity in player.getNearbyEntities(range, range, range)) {
                    if (player.hasLineOfSight(entity) && entity is LivingEntity && entity.type.name.lowercase() !in blackList) {
                        val to = entity.location.toVector().clone().subtract(player.location.toVector())
                        val angle = event.projectile.velocity.angle(to)
                        if (angle >= minAng) {
                            continue
                        }
                        minAng = angle
                        minEntity = entity
                    }
                }
                val arrow = event.projectile as AbstractArrow
                if (minEntity != null) {

//                TeamColorUtils.getTeamByColor(color)?.addEntry(arrow.uniqueId.toString())
                    arrow.isGlowing = true
//                TeamColorUtils.getTeamByColor(color)?.addEntry(minEntity.uniqueId.toString())
                    minEntity.isGlowing = true

                    arrow.submit(delay = 1L, period = ticks) {
                        val speed = arrow.velocity.length()
                        if (arrow.isOnGround || arrow.isDead || minEntity.isDead) {
                            arrow.isGlowing = false
                            minEntity.isGlowing = false
                            cancel()
                            return@submit
                        }
                        val to = minEntity.location.clone().add(Vector(0.0, 0.5, 0.0)).subtract(arrow.location).toVector()
                        val dirVel = arrow.velocity.clone().normalize()
                        val dirTarget = to.clone().normalize()
                        val ang = dirVel.angle(dirTarget)
                        var speed_ = 0.9  * speed + 0.1399999999999999
                        if (minEntity is Player && arrow.location.distance(minEntity.location) < 8.0) {
                            if (minEntity.isBlocking) {
                                speed_ = speed * 0.6
                            }
                        }
                        val newVel = if (ang < 0.12) {
                            dirVel.clone().multiply(speed_)
                        } else {
                            val newDir = dirVel.clone().multiply((ang - 0.12) / ang).add(dirTarget.clone().multiply(0.12 / ang))
                            newDir.normalize()
                            newDir.clone().multiply(speed_)
                        }
                        arrow.velocity = newVel.add(Vector(0.0, 0.03, 0.0))
                    }
                }
            }
        }
    }

//    private fun shootBow(range: Double, ticks: Long, event: EntityShootBowEvent, color: ChatColor, blackList: List<String>) {
//        val arrow = event.projectile as AbstractArrow
//        val who: LivingEntity = event.entity
//
//        TeamColorUtils.getTeamByColor(color)?.addEntry(arrow.uniqueId.toString())
//        arrow.isGlowing = true
//        arrow.shooter = event.entity
//
//        var target: LivingEntity? = (
//                // 优先寻找玩家所指向的实体
//                who.getTargetEntity(
//                    range.roundToInt() * 2
//                ) ?: who.getTargetBlockExact( // 如果没有，则根据距离找可用的实体
//                    range.roundToInt() * 2
//                )?.location?.getNearbyEntities(range, range, range)?.firstOrNull()
//
//                )?.let {
//                if (
//                    it.uniqueId !== who.uniqueId
//                    && it is LivingEntity
//                    && it !is ArmorStand
//                    && who.hasLineOfSight(it)
//                ) it else null
//            } // 如果有预先瞄准则获取特定的目标并持续追踪
//
//        submit(delay = 1L, period = ticks) {
//            // 判断此箭货是否还在飞行
//            if (arrow.isDead || arrow.isInBlock || target?.isDead == true) {
//                cancel()
//                target?.isGlowing = false
//                return@submit
//            }
//
//            // 分成两个 else 而不是一个是为了找到后就立即修正一次方向
//            if (target == null) {
//                target = arrow.getNearbyEntities(range, range, range).firstOrNull {
//                    it.uniqueId !== who.uniqueId
//                            // && !PermissionUtils.checkIfIsNPC(entity)
//                            && it is Mob
//                            && it.type.name.lowercase() !in blackList
//                            && who.hasLineOfSight(it)
//                } as LivingEntity?
//            }
//
//            // 如果找到了目标就继续冲
//            target?.let {
//                TeamColorUtils.getTeamByColor(color)?.addEntry(it.uniqueId.toString())
//                it.isGlowing = true
//
//                val perfectDirection: Vector = arrow.location.clone()
//                    .subtract(it.eyeLocation)
//                    .toVector()
//                    .normalize()
//                    .multiply(-1)
//                // 设置箭速
//                VectorUtils.addVelocity(arrow, perfectDirection.multiply(arrow.velocity.length()), false)
//            }
//        }
//    }
}