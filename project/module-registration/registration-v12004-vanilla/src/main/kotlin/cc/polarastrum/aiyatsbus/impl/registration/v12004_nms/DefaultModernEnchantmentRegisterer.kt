/*
 * This file is part of EcoEnchants, licensed under the GPL-3.0 License.
 *
 *  Copyright (C) 2024 Auxilor
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
package cc.polarastrum.aiyatsbus.impl.registration.v12004_nms

import cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantment
import cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantmentBase
import cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantmentManager
import cc.polarastrum.aiyatsbus.core.BuiltinAiyatsbusEnchantmentBase
import cc.polarastrum.aiyatsbus.core.InternalAiyatsbusEnchantmentBase
import cc.polarastrum.aiyatsbus.core.registration.modern.ModernEnchantmentRegisterer
import cc.polarastrum.aiyatsbus.core.util.setStaticFinal
import cc.polarastrum.aiyatsbus.impl.registration.v12004_paper.AiyatsbusCraftEnchantment
import cc.polarastrum.aiyatsbus.impl.registration.v12004_paper.BuiltinAiyatsbusCraftEnchantment
import cc.polarastrum.aiyatsbus.impl.registration.v12004_paper.InternalAiyatsbusCraftEnchantment
import cc.polarastrum.aiyatsbus.impl.registration.v12004_paper.NMSAiyatsbusEnchantment
import cc.polarastrum.aiyatsbus.impl.registration.v12004_paper.VanillaCraftEnchantment
import net.minecraft.core.Holder
import net.minecraft.core.IRegistry
import net.minecraft.core.IRegistryCustom
import net.minecraft.core.RegistryMaterials
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.server.MinecraftServer
import net.minecraft.world.item.enchantment.Enchantments
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.v1_20_R3.CraftRegistry
import org.bukkit.craftbukkit.v1_20_R3.CraftServer
import org.bukkit.craftbukkit.v1_20_R3.enchantments.CraftEnchantment
import org.bukkit.craftbukkit.v1_20_R3.util.CraftNamespacedKey
import org.bukkit.enchantments.Enchantment
import taboolib.common.platform.PlatformFactory
import taboolib.common.util.unsafeLazy
import taboolib.library.reflex.Reflex.Companion.getProperty
import java.util.*
import kotlin.collections.HashMap

/**
 * Aiyatsbus
 * com.mcstarrysky.aiyatsbus.impl.registration.modern.DefaultModernEnchantmentRegisterer
 *
 * @author mical
 * @since 2024/2/17 17:28
 */
class DefaultModernEnchantmentRegisterer : ModernEnchantmentRegisterer {

    private val frozenField = RegistryMaterials::class.java
        .declaredFields
        .filter { it.type.isPrimitive }[0]
        .apply { isAccessible = true }

    private val unregisteredIntrusiveHoldersField = RegistryMaterials::class.java
        .declaredFields
        .last { it.type == Map::class.java }
        .apply { isAccessible = true }

    private val registries by unsafeLazy {
        Bukkit.getServer().getProperty<HashMap<Class<*>, org.bukkit.Registry<*>>>("registries")!!
    }

    private val vanillaEnchantments = Enchantments::class.java
        .declaredFields
        .asSequence()
        .filter { it.type == net.minecraft.world.item.enchantment.Enchantment::class.java }
        .map { it.get(null) as net.minecraft.world.item.enchantment.Enchantment }
        .mapNotNull { BuiltInRegistries.ENCHANTMENT.getKey(it) }
        .map { CraftNamespacedKey.fromMinecraft(it) }
        .toSet()

    override fun unfreezeRegistry() {
        frozenField.set(BuiltInRegistries.ENCHANTMENT, false)
        unregisteredIntrusiveHoldersField.set(
            BuiltInRegistries.ENCHANTMENT,
            IdentityHashMap<net.minecraft.world.item.enchantment.Enchantment,
                    Holder.c<net.minecraft.world.item.enchantment.Enchantment>>()
        )
    }

    override fun replaceRegistry() {
        val server = Bukkit.getServer() as CraftServer
        val api = PlatformFactory.getAPI<AiyatsbusEnchantmentManager>()

        @Suppress("UNCHECKED_CAST")
        val registry = CraftRegistry(
            Enchantment::class.java as Class<in Enchantment?>,
            // TabooLib NMSProxy 已知问题: 调用对象中「仅在父类」声明的方法或字段无法被 TabooLib NMSProxy 重定向
            // TODO: java.lang.NoSuchMethodError: 'net.minecraft.server.dedicated.DedicatedServer net.minecraft.server.dedicated.DedicatedPlayerList.c()'
            ((server.handle.server as MinecraftServer).registryAccess() as IRegistryCustom).registryOrThrow(Registries.ENCHANTMENT)
        ) { key, registry ->
            val isVanilla = vanillaEnchantments.contains(key)
            val aiyatsbus = api.getEnchant(key)

            if (aiyatsbus != null) {
                aiyatsbus as Enchantment
            } else if (isVanilla) {
                CraftEnchantment(key, registry)
            } else null
        }

        // Register to server
        registries[Enchantment::class.java] = registry

        // Register to API
        org.bukkit.Registry::class.java
            .getDeclaredField("ENCHANTMENT")
            .setStaticFinal(registry)

        // Unfreeze NMS registry
        unfreezeRegistry()
    }

    override fun freezeRegistry() {
    }

    override fun register(enchant: AiyatsbusEnchantmentBase): Enchantment {
        if (BuiltInRegistries.ENCHANTMENT.containsKey(CraftNamespacedKey.toMinecraft(enchant.enchantmentKey))) {
            val nms = BuiltInRegistries.ENCHANTMENT[CraftNamespacedKey.toMinecraft(enchant.enchantmentKey)]
            if (nms != null) {
                 return if (enchant.alternativeData.isVanilla) {
                     if (enchant !is cc.polarastrum.aiyatsbus.core.VanillaAiyatsbusEnchantmentBase) throw IllegalArgumentException("Enchant ${enchant.id} must be an impl of VanillaAiyatsbusEnchantment!")
                     VanillaCraftEnchantment(enchant, nms)
                 } else {
                     when (enchant) {
                         is BuiltinAiyatsbusEnchantmentBase -> BuiltinAiyatsbusCraftEnchantment(enchant, nms)
                         is InternalAiyatsbusEnchantmentBase -> InternalAiyatsbusCraftEnchantment(enchant, nms)
                         else -> AiyatsbusCraftEnchantment(enchant, nms)
                     }
                 }
            } else {
                throw IllegalStateException("Enchantment ${enchant.id} wasn't registered")
            }
        }
        IRegistry.register(BuiltInRegistries.ENCHANTMENT, enchant.id, NMSAiyatsbusEnchantment(enchant.id))
        return register(enchant)
    }

    override fun unregister(enchant: AiyatsbusEnchantment) {

    }
}