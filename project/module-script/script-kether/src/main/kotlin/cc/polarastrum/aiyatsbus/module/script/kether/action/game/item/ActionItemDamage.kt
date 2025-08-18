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
package cc.polarastrum.aiyatsbus.module.script.kether.action.game.item

import cc.polarastrum.aiyatsbus.core.Aiyatsbus
import org.bukkit.entity.LivingEntity

/**
 * Aiyatsbus
 * com.mcstarrysky.aiyatsbus.module.kether.action.item.ActionItemDamage
 *
 * @author mical
 * @since 2024/3/20 23:30
 */
object ActionItemDamage : ActionItem.Resolver {

    override val name: Array<String> = arrayOf("damage")

    /**
     * item damage &item to 1 by &entity
     * 考虑了耐久等附魔
     */
    override fun resolve(reader: ActionItem.Reader): ActionItem.Handler<out Any?> {
        return reader.transfer {
            combine(
                source(),
                trim("to", then = int(0)),
                trim("by", then = entity())
            ) { item, damage, entity ->
                Aiyatsbus.api().getMinecraftAPI().getItemOperator().damageItemStack(item, damage, entity as LivingEntity)
            }
        }
    }
}