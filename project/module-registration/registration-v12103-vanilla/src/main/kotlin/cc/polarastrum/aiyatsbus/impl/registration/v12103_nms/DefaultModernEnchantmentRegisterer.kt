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
package cc.polarastrum.aiyatsbus.impl.registration.v12103_nms

import cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantment
import cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantmentBase
import cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantmentManager
import cc.polarastrum.aiyatsbus.core.StandardPriorities
import cc.polarastrum.aiyatsbus.core.VanillaAiyatsbusEnchantmentBase
import cc.polarastrum.aiyatsbus.core.registration.modern.ModernEnchantmentRegisterer
import cc.polarastrum.aiyatsbus.impl.registration.v12103_paper.EnchantmentHelper
import net.minecraft.core.*
import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.IChatBaseComponent
import net.minecraft.resources.MinecraftKey
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.craftbukkit.v1_21_R2.CraftRegistry
import org.bukkit.craftbukkit.v1_21_R2.CraftServer
import org.bukkit.craftbukkit.v1_21_R2.enchantments.CraftEnchantment
import org.bukkit.craftbukkit.v1_21_R2.util.CraftChatMessage
import org.bukkit.craftbukkit.v1_21_R2.util.CraftNamespacedKey
import org.bukkit.enchantments.Enchantment
import taboolib.common.LifeCycle
import taboolib.common.platform.PlatformFactory
import taboolib.common.platform.function.registerLifeCycleTask
import taboolib.library.reflex.Reflex.Companion.getProperty
import taboolib.library.reflex.Reflex.Companion.setProperty
import java.lang.reflect.Modifier
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
        .lookupOrThrow(Registries.ENCHANTMENT)

    private val itemRegistry = (Bukkit.getServer() as CraftServer).server
        .registryAccess()
        .lookupOrThrow(Registries.ITEM) as RegistryMaterials<Item>

    private val bukkitRegistry = org.bukkit.Registry.ENCHANTMENT

    private val frozenField = RegistryMaterials::class.java
        .declaredFields
        .filter { it.type.isPrimitive }[0]
        .apply { isAccessible = true }

    private val allTags = RegistryMaterials::class.java
        .declaredFields
        .filter { it.type.name.contains("TagSet") }[0]
        .apply { isAccessible = true }

    private val frozenTags = RegistryMaterials::class.java
        .declaredFields
        .filter { it.type == Map::class.java }[4]
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
        // Unfreeze NMS Item registry
        frozenField.set(itemRegistry, false)
        unregisteredIntrusiveHoldersField.set(itemRegistry, IdentityHashMap<Item, Holder.c<Item>>())

        // Unfreeze NMS Enchantment registry
        frozenField.set(enchantmentRegistry, false)
        unregisteredIntrusiveHoldersField.set(
            enchantmentRegistry,
            IdentityHashMap<NMSEnchantment, Holder.c<NMSEnchantment>>()
        )
    }

    @Suppress("UNCHECKED_CAST")
    override fun replaceRegistry() {
        val api = PlatformFactory.getAPI<AiyatsbusEnchantmentManager>()

        unfreezeRegistry()

        val newRegistryMTB =
            BiFunction<NamespacedKey, NMSEnchantment, Enchantment?> { key, registry ->
                val aiyatsbus = api.getEnchant(key)

                if (aiyatsbus != null) {
                    aiyatsbus as Enchantment
                } else {
                    // 此时若获取不到则一定是获取原版附魔
                    // 此时获取更多附魔返回值应当为 null
                    EnchantmentHelper.createCraftEnchantment(key, enchantmentRegistry.get(CraftNamespacedKey.toMinecraft(key)).get().value()) as CraftEnchantment
                }
            }

        // Update bukkit registry
        minecraftToBukkit.set(bukkitRegistry, newRegistryMTB)
        minecraftToBukkitAlt.set(bukkitRegistry, newRegistryMTB)

        // Clear the enchantment cache
        cache.set(bukkitRegistry, mutableMapOf<NamespacedKey, Enchantment>())

        // Unfreeze NMS registry
        frozenField.set(enchantmentRegistry, false)
        unregisteredIntrusiveHoldersField.set(enchantmentRegistry, IdentityHashMap<NMSEnchantment, Holder.c<NMSEnchantment>>())

        // Freeze registries when all enchantments were loaded
        freezeRegistry()
    }

    override fun freezeRegistry() {
        registerLifeCycleTask(LifeCycle.ENABLE, StandardPriorities.FREEZE_REGISTRY) {
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

            // Freeze NMS Enchantment Registry
            allTags.set(enchantmentRegistry, unboundTagSet)
            enchantmentRegistry.freeze()

            // Freeze NMS Item Registry
            val itemTagSet = allTags.get(itemRegistry)
            val itemTagsMap = HashMap<TagKey<Item>, HolderSet.Named<Item>>(
                itemTagSet.getProperty<Any>("val\$map")!! as Map<TagKey<Item>, HolderSet.Named<Item>>
            )
            val itemFrozenTags = frozenTags.get(itemRegistry) as MutableMap<TagKey<Item>, HolderSet.Named<Item>>
            itemTagsMap.forEach(itemFrozenTags::putIfAbsent)

            allTags.set(itemRegistry, unboundTagSet)
            itemRegistry.freeze()
            itemFrozenTags.forEach(itemTagsMap::putIfAbsent)

            itemTagSet.setProperty("val\$map", itemTagsMap)
            allTags.set(itemRegistry, itemTagSet)
        }
    }

    override fun register(enchant: AiyatsbusEnchantmentBase): Enchantment {
        // Clear the enchantment cache
        cache.set(bukkitRegistry, mutableMapOf<NamespacedKey, Enchantment>())

        if (enchantmentRegistry.containsKey(CraftNamespacedKey.toMinecraft(enchant.enchantmentKey))) {
            val nms = enchantmentRegistry[CraftNamespacedKey.toMinecraft(enchant.enchantmentKey)]

            if (nms.isPresent) {
                return (if (enchant.alternativeData.isVanilla) {
                    if (enchant !is VanillaAiyatsbusEnchantmentBase) throw IllegalArgumentException("Enchant ${enchant.id} must be an impl of VanillaAiyatsbusEnchantment!")
                    EnchantmentHelper.createVanillaCraftEnchantment(enchant, nms.get().value())
                } else {
                    EnchantmentHelper.createAiyatsbusCraftEnchantment(enchant, nms.get().value())
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
        val supportedItems = createItemsSet("enchant_supported", enchant.id, enchant.targets.flatMap { it.types })

        val enchantment = NMSEnchantment.enchantment(
            NMSEnchantment.definition(
                supportedItems,
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

    @Suppress("UNCHECKED_CAST")
    private fun createItemsSet(
        prefix: String,
        enchantId: String,
        materials: Collection<Material>
    ): HolderSet.Named<Item> {
        val customKey = TagKey.create(itemRegistry.key(), MinecraftKey.withDefaultNamespace("$prefix/$enchantId"))
        val holders = arrayListOf<Holder<Item>>()

        materials.forEach { material ->
            val location = CraftNamespacedKey.toMinecraft(material.key)
            val holder = itemRegistry.get(location).orElse(null) ?: return@forEach
            holders.add(holder)
        }

        itemRegistry.bindTag(customKey, holders)

        return (frozenTags.get(itemRegistry) as Map<*, *>)[customKey] as HolderSet.Named<Item>
    }
}

typealias NMSEnchantment = net.minecraft.world.item.enchantment.Enchantment
typealias NMSEnchantmentC = net.minecraft.world.item.enchantment.Enchantment.c