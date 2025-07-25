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
package cc.polarastrum.aiyatsbus.module.script.kether.property.bukkit.entity

import cc.polarastrum.aiyatsbus.module.script.kether.AiyatsbusGenericProperty
import cc.polarastrum.aiyatsbus.module.script.kether.AiyatsbusProperty
import cc.polarastrum.aiyatsbus.module.script.kether.LiveData.Companion.liveEntity
import org.bukkit.entity.Projectile
import org.bukkit.projectiles.ProjectileSource
import taboolib.common.OpenResult

/**
 * Aiyatsbus
 * com.mcstarrysky.aiyatsbus.module.kether.property.bukkit.entity.PropertyProjectile
 *
 * @author mical
 * @since 2024/3/24 21:23
 */
@AiyatsbusProperty(
    id = "projectile",
    bind = Projectile::class
)
class PropertyProjectile : AiyatsbusGenericProperty<Projectile>("projectile") {

    override fun readProperty(instance: Projectile, key: String): OpenResult {
        val property: Any? = when (key) {
            "shooter" -> instance.shooter
            else -> return OpenResult.failed()
        }
        return OpenResult.successful(property)
    }

    override fun writeProperty(instance: Projectile, key: String, value: Any?): OpenResult {
        when (key) {
            "shooter" -> {
                instance.shooter = (value?.liveEntity as ProjectileSource?)
                    ?: return OpenResult.successful()
            }
            else -> return OpenResult.failed()
        }
        return OpenResult.successful()
    }
}