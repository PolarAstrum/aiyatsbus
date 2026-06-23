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
package cc.polarastrum.aiyatsbus.impl.registration.v12100_nms

import cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantment
import cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantmentBase
import cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantmentManager
import cc.polarastrum.aiyatsbus.core.VanillaAiyatsbusEnchantmentBase
import cc.polarastrum.aiyatsbus.core.registration.modern.ModernEnchantmentRegisterer
import cc.polarastrum.aiyatsbus.impl.registration.v12100_paper.EnchantmentHelper
import net.minecraft.core.*
import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.IChatBaseComponent
import net.minecraft.resources.MinecraftKey
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.craftbukkit.v1_21_R1.CraftRegistry
import org.bukkit.craftbukkit.v1_21_R1.CraftServer
import org.bukkit.craftbukkit.v1_21_R1.enchantments.CraftEnchantment
import org.bukkit.craftbukkit.v1_21_R1.util.CraftChatMessage
import org.bukkit.craftbukkit.v1_21_R1.util.CraftNamespacedKey
import org.bukkit.enchantments.Enchantment
import taboolib.common.platform.PlatformFactory
import taboolib.library.reflex.Reflex.Companion.getProperty
import java.util.*
import java.util.function.BiFunction

/**
 * Aiyatsbus
 * com.mcstarrysky.aiyatsbus.impl.registration.modern.DefaultModernEnchantmentRegisterer
 *
 * @author mical
 * @since 2024/2/17 17:28
 */
class DefaultModernEnchantmentRegisterer : ModernEnchantmentRegisterer {

    private val enchantmentRegistry = ((Bukkit.getServer() as CraftServer).server
        .registryAccess() as IRegistryCustom)
        .registryOrThrow(Registries.ENCHANTMENT)

    private val bukkitRegistry = org.bukkit.Registry.ENCHANTMENT

    private val frozenField = RegistryMaterials::class.java
        .declaredFields
        .filter { it.type.isPrimitive }[0]
        .apply { isAccessible = true }

    private val unregisteredIntrusiveHoldersField = RegistryMaterials::class.java
        .declaredFields
        .last { it.type == Map::class.java }
        .apply { isAccessible = true }

    private val minecraftToBukkit = bukkitRegistry::class.java
        .getDeclaredField("minecraftToBukkit")
        .apply { isAccessible = true }

    private val minecraftToBukkitAlt = CraftRegistry::class.java
        .getDeclaredField("minecraftToBukkit")
        .apply { isAccessible = true }

    private val cache = CraftRegistry::class.java
        .getDeclaredField("cache")
        .apply { isAccessible = true }

    override fun unfreezeRegistry() {
        frozenField.set(enchantmentRegistry, false)
        unregisteredIntrusiveHoldersField.set(enchantmentRegistry, IdentityHashMap<NMSEnchantment, Holder.c<NMSEnchantment>>())
    }

    override fun replaceRegistry() {
        val api = PlatformFactory.getAPI<AiyatsbusEnchantmentManager>()

        val newRegistryMTB =
            BiFunction<NamespacedKey, NMSEnchantment, Enchantment?> { key, registry ->
                val aiyatsbus = api.getEnchant(key)

                if (aiyatsbus != null) {
                    aiyatsbus as Enchantment
                } else {
                    // 此时若获取不到则一定是获取原版附魔
                    // 此时获取更多附魔返回值应当为 null
                    EnchantmentHelper.createCraftEnchantment(key, enchantmentRegistry.get(CraftNamespacedKey.toMinecraft(key))) as CraftEnchantment
                }
            }

        // Update bukkit registry
        minecraftToBukkit.set(bukkitRegistry, newRegistryMTB)
        minecraftToBukkitAlt.set(bukkitRegistry, newRegistryMTB)

        // Clear the enchantment cache
        cache.set(bukkitRegistry, mutableMapOf<NamespacedKey, Enchantment>())

        // Unfreeze NMS registry
        unfreezeRegistry()
    }

    override fun freezeRegistry() {
    }

    override fun register(enchant: AiyatsbusEnchantmentBase): Enchantment {
        // Clear the enchantment cache
        cache.set(bukkitRegistry, mutableMapOf<NamespacedKey, Enchantment>())

        if (enchantmentRegistry.containsKey(CraftNamespacedKey.toMinecraft(enchant.enchantmentKey))) {
            val nms = enchantmentRegistry[CraftNamespacedKey.toMinecraft(enchant.enchantmentKey)]

            if (nms != null) {
                return (if (enchant.alternativeData.isVanilla) {
                    if (enchant !is VanillaAiyatsbusEnchantmentBase) throw IllegalArgumentException("Enchant ${enchant.id} must be an impl of VanillaAiyatsbusEnchantment!")
                    EnchantmentHelper.createVanillaCraftEnchantment(enchant, nms)
                } else {
                    EnchantmentHelper.createAiyatsbusCraftEnchantment(enchant, nms)
                }) as CraftEnchantment
            } else {
                throw IllegalStateException("Enchantment ${enchant.id} wasn't registered")
            }
        }

        val vanillaEnchantment = vanillaEnchantment(enchant)


        enchantmentRegistry.createIntrusiveHolder(vanillaEnchantment)

        IRegistry.register(
            enchantmentRegistry,
            MinecraftKey.withDefaultNamespace(enchant.id),
            vanillaEnchantment
        )

        return register(enchant)
    }

    override fun unregister(enchant: AiyatsbusEnchantment) {

    }

    private fun vanillaEnchantment(enchant: AiyatsbusEnchantment): NMSEnchantment {
        val enchantment = NMSEnchantment.enchantment(
            NMSEnchantment.definition(
                HolderSet.empty(),
                1,
                enchant.basicData.maxLevel,
                NMSEnchantment.constantCost(1),
                NMSEnchantment.constantCost(1),
                0,
            )
        )
//        return enchantment.build(MinecraftKey.withDefaultNamespace(enchant.id))
        return NMSEnchantment(
            CraftChatMessage.fromJSON(enchant.basicData.name) as IChatBaseComponent,
            enchantment.getProperty<NMSEnchantmentC>("definition")!!,
            enchantment.getProperty<HolderSet<NMSEnchantment>>("exclusiveSet")!!,
            enchantment.getProperty<DataComponentMap.a>("effectMapBuilder")!!.build()
        )
    }
}

typealias NMSEnchantment = net.minecraft.world.item.enchantment.Enchantment
typealias NMSEnchantmentC = net.minecraft.world.item.enchantment.Enchantment.c