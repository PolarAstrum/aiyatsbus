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
package cc.polarastrum.aiyatsbus.impl.registration.v12104_nms

import cc.polarastrum.aiyatsbus.core.Aiyatsbus
import cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantment
import cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantmentBase
import cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantmentManager
import cc.polarastrum.aiyatsbus.core.registration.modern.ModernEnchantmentRegisterer
import cc.polarastrum.aiyatsbus.impl.registration.v12104_paper.EnchantmentHelper
import io.papermc.paper.registry.entry.RegistryTypeMapper
import io.papermc.paper.registry.legacy.DelayedRegistry
import net.minecraft.core.*
import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.IChatBaseComponent
import net.minecraft.resources.MinecraftKey
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.craftbukkit.v1_21_R3.CraftRegistry
import org.bukkit.craftbukkit.v1_21_R3.CraftServer
import org.bukkit.craftbukkit.v1_21_R3.enchantments.CraftEnchantment
import org.bukkit.craftbukkit.v1_21_R3.util.CraftNamespacedKey
import org.bukkit.enchantments.Enchantment
import taboolib.common.platform.PlatformFactory
import taboolib.library.reflex.Reflex.Companion.getProperty
import taboolib.library.reflex.Reflex.Companion.invokeMethod
import taboolib.module.nms.MinecraftVersion
import java.lang.reflect.Modifier
import java.util.*
import java.util.function.BiFunction
import javax.annotation.Nullable

/**
 * NOTICE 不再支持 spigot
 *
 * @author mical
 * @since 2024/2/17 17:28
 */
class DefaultModernEnchantmentRegisterer : ModernEnchantmentRegisterer {

    private val enchantmentRegistry = if (MinecraftVersion.isUniversalCraftBukkit) {
        (Bukkit.getServer() as CraftServer).server
            .registryAccess()
            .lookupOrThrow(Registries.ENCHANTMENT)
    } else {
        ((Bukkit.getServer() as CraftServer).server)
            .registryAccess()
            .invokeMethod<IRegistry<NMSEnchantment>>("e", Registries.ENCHANTMENT, isStatic = false, remap = false)!! // lookupOrThrow
    }

    private val bukkitRegistry = if (MinecraftVersion.isUniversalCraftBukkit) {
        (org.bukkit.Registry.ENCHANTMENT as DelayedRegistry<Enchantment, *>).delegate()
    } else {
        org.bukkit.Registry.ENCHANTMENT
    }

    private val frozenField = RegistryMaterials::class.java
        .declaredFields
        .filter { it.type.isPrimitive }[0]
        .apply { isAccessible = true }

    private val allTags = if (MinecraftVersion.isUniversalCraftBukkit) {
        RegistryMaterials::class.java
            .declaredFields
            .filter { it.type.name.contains("TagSet") }[0]
            .apply { isAccessible = true }
    } else {
        RegistryMaterials::class.java
            .getDeclaredField("k") // allTags
            .apply { isAccessible = true }
    }

    private val unregisteredIntrusiveHoldersField = RegistryMaterials::class.java
        .declaredFields
        .filter { it.type == Map::class.java }
        .filter { it.isAnnotationPresent(Nullable::class.java) }[0]
        .apply { isAccessible = true }

    // 1.21.4+ only has minecraftToBukkit in CraftRegistry, removing the duplicate in WritableCraftRegistry
    private val minecraftToBukkit = CraftRegistry::class.java
        .getDeclaredField("minecraftToBukkit")
        .apply { isAccessible = true }

    private val cache = CraftRegistry::class.java
        .getDeclaredField("cache")
        .apply { isAccessible = true }

    override fun replaceRegistry() {
        val api = PlatformFactory.getAPI<AiyatsbusEnchantmentManager>()

        val newRegistryMTB =
            BiFunction<NamespacedKey, NMSEnchantment, Enchantment?> { key, registry ->
                val isVanilla = enchantmentRegistry.containsKey(CraftNamespacedKey.toMinecraft(key))
                val aiyatsbus = api.getEnchant(key)

                if (isVanilla) {
                    if (MinecraftVersion.isUniversalCraftBukkit) {
                        EnchantmentHelper.createCraftEnchantment(enchantmentRegistry.get(CraftNamespacedKey.toMinecraft(key)).get())
                    } else {
                        CraftEnchantment(key, registry)
                    }
                } else if (aiyatsbus != null) {
                    aiyatsbus as Enchantment
                } else null
            }

        // Update bukkit registry
        @Suppress("UNCHECKED_CAST")
        if (MinecraftVersion.isUniversalCraftBukkit) {
            minecraftToBukkit.set(
                bukkitRegistry,
                RegistryTypeMapper(newRegistryMTB as BiFunction<NamespacedKey, NMSEnchantment, Enchantment>)
            )
        } else {
            minecraftToBukkit.set(bukkitRegistry, newRegistryMTB)
        }

        // Clear the enchantment cache
        cache.set(bukkitRegistry, mutableMapOf<NamespacedKey, Enchantment>())

        // Unfreeze NMS registry
        frozenField.set(enchantmentRegistry, false)
        unregisteredIntrusiveHoldersField.set(enchantmentRegistry, IdentityHashMap<NMSEnchantment, Holder.c<NMSEnchantment>>())

        /*
        Creating an unbound tag set requires using reflection because the inner class is
        package-private, so we just find the method manually.
         */

        val unboundTagSet = RegistryMaterials::class.java
            .declaredClasses[0]
            .declaredMethods
            .filter { Modifier.isStatic(it.modifiers) }
            .filter { it.parameterCount == 0 }[0]
            .apply { isAccessible = true }
            .invoke(null)

        allTags.set(enchantmentRegistry, unboundTagSet)
    }

    override fun register(enchant: AiyatsbusEnchantmentBase): Enchantment {
        // Clear the enchantment cache
        cache.set(bukkitRegistry, mutableMapOf<NamespacedKey, Enchantment>())

        if (enchantmentRegistry.containsKey(CraftNamespacedKey.toMinecraft(enchant.enchantmentKey))) {
            val nms = enchantmentRegistry[CraftNamespacedKey.toMinecraft(enchant.enchantmentKey)]

            if (nms.isPresent) {
                if (MinecraftVersion.isUniversalCraftBukkit) {
                    return EnchantmentHelper.createAiyatsbusCraftEnchantment(enchant, nms.get()) as CraftEnchantment
                } else {
                    return AiyatsbusVanillaCraftEnchantment(enchant, nms.get().value())
                }
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
            Aiyatsbus.api().getMinecraftAPI().componentFromJson(enchant.basicData.name) as IChatBaseComponent,
            enchantment.getProperty<NMSEnchantmentC>("definition")!!,
            enchantment.getProperty<HolderSet<NMSEnchantment>>("exclusiveSet")!!,
            enchantment.getProperty<DataComponentMap.a>("effectMapBuilder")!!.build()
        )
    }
}

typealias NMSEnchantment = net.minecraft.world.item.enchantment.Enchantment
typealias NMSEnchantmentC = net.minecraft.world.item.enchantment.Enchantment.c