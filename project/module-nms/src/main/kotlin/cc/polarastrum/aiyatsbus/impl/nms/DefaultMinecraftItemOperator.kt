package cc.polarastrum.aiyatsbus.impl.nms

import cc.polarastrum.aiyatsbus.core.*
import cc.polarastrum.aiyatsbus.core.util.isNull
import io.papermc.paper.adventure.PaperAdventure
import net.kyori.adventure.util.Codec
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.entity.EnumItemSlot
import net.minecraft.world.item.ItemAir
import net.minecraft.world.item.ItemEnchantedBook
import net.minecraft.world.item.Items
import net.minecraft.world.item.trading.MerchantRecipeList
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftLivingEntity
import org.bukkit.craftbukkit.v1_20_R3.inventory.CraftItemStack
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.library.reflex.Reflex.Companion.getProperty
import taboolib.library.reflex.Reflex.Companion.setProperty
import taboolib.module.nms.MinecraftVersion.versionId
import taboolib.module.nms.NMSItemTag
import java.io.IOException
import kotlin.experimental.and

/**
 * Aiyatsbus
 * cc.polarastrum.aiyatsbus.impl.nms.nms.DefaultMinecraftItemOperator
 *
 * @author mical
 * @since 2025/8/16 08:51
 */
class DefaultMinecraftItemOperator : MinecraftItemOperator {

    override fun clearEnchantingSession(item: ItemStack) {
        // 旧版本没有物品数据组件，不需要维护工作副本。
    }

    private val NBT_CODEC: Codec<Any, String, IOException, IOException> = PaperAdventure::class.java.getProperty("NBT_CODEC", isStatic = true)!!

    override fun getRepairCost(item: ItemStack): Int {
        return (NMSItemTag.asNMSCopy(item) as NMSItemStack).baseRepairCost
    }

    override fun setRepairCost(item: ItemStack, cost: Int): ItemStack {
        return Aiyatsbus.api().getMinecraftAPI().getHelper().asCraftMirror(
            (Aiyatsbus.api().getMinecraftAPI().getHelper().getCraftItemStackHandle(item) as NMSItemStack).setRepairCost(cost)
        )
    }

    override fun createItemStack(material: String, tag: String?): ItemStack {
        return try {
            if (versionId >= 11802) {
                Bukkit.getItemFactory().createItemStack(material + tag)
            } else {
                val mat = material.split(":")[1].uppercase()
                val bkItem = ItemStack(Material.valueOf(mat), 1)
                if (tag.isNullOrEmpty()) {
                    bkItem
                } else {
                    val nmsItem = NMSItemTag.asNMSCopy(bkItem)
                    val nbt = NBT_CODEC.decode(tag) as NBTTagCompound
                    (nmsItem as NMSItemStack).tag = nbt
                    NMSItemTag.asBukkitCopy(nmsItem)
                }
            }
        } catch (t: Throwable) {
            throw IllegalStateException(t)
        }
    }

    override fun adaptMerchantRecipe(merchantRecipeList: Any, player: Player) {

        fun adapt(item: Any, player: Player): Any {
            val bkItem = NMSItemTag.asBukkitCopy(item)
            if (bkItem.isNull) return item
            return NMSItemTag.asNMSCopy(bkItem.toDisplayMode(player))
        }

        val previous = merchantRecipeList as MerchantRecipeList
        for (i in 0 until previous.size) {
            with(previous[i]!!) {
                setProperty("baseCostA", adapt(baseCostA, player) as NMSItemStack)
                setProperty("costB", adapt(costB, player) as NMSItemStack)
                setProperty("result", adapt(result, player) as NMSItemStack)
            }
        }
    }

    override fun damageItemStack(item: ItemStack, amount: Int, entity: LivingEntity): ItemStack {
        var stack = item
        val nmsStack = if (stack is CraftItemStack) {
            val handle = Aiyatsbus.api().getMinecraftAPI().getHelper().getCraftItemStackHandle(stack) as NMSItemStack
            if (handle == null || handle.isEmpty) {
                return stack
            }
            handle
        } else {
            CraftItemStack.asNMSCopy(stack).also {
                stack = CraftItemStack.asCraftMirror(it)
            }
        }
        damageItemStack(nmsStack, amount, null, entity)
        return stack
    }

    /*
    /**
     * CraftLivingEntity&damageItemStack(EquipmentSlot, int)
     */
    fun damageItemStack(slot: EquipmentSlot, amount: Int, entity: LivingEntity) {
        if (MinecraftVersion.isUniversal) {
            val nmsSlot = CraftEquipmentSlot20.getNMS(slot)
            damageItemStack((entity as CraftLivingEntity20).handle.getItemBySlot(nmsSlot), amount, nmsSlot, entity)
        } else {
            val nmsSlot = CraftEquipmentSlot16.getNMS(slot)
            damageItemStack((entity as CraftLivingEntity16).handle.getEquipment(nmsSlot), amount, nmsSlot, entity)
        }
    }

     */

    /**
     * CraftLivingEntity#damageItemStack0
     */
    private fun damageItemStack(nmsStack: Any, amount: Int, enumItemSlot: Any?, entity: LivingEntity) {
        nmsStack as NMSItemStack
        // 1.20.4 -> hurtAndBreak(int, EntityLiving, Consumer<EntityLiving>)
        // 1.20.5, 1.21 -> hurtAndBreak(int, EntityLiving, EnumItemSlot), 自动广播事件
        nmsStack.hurtAndBreak(amount, (entity as CraftLivingEntity).handle) { entityLiving ->
            (enumItemSlot as? EnumItemSlot)?.let { entityLiving.broadcastBreakEvent(it) }
        }
    }

    override fun getEnchants(item: ItemStack): Map<AiyatsbusEnchantment, Int> {
        val handle = (if (item is CraftItemStack) Aiyatsbus.api().getMinecraftAPI().getHelper().getCraftItemStackHandle(item) else CraftItemStack.asNMSCopy(item)) as NMSItemStack
        val enchantmentNBT = if (handle.item == Items.ENCHANTED_BOOK)
            ItemEnchantedBook.getEnchantments(handle)
        else handle.enchantmentTags

        val map = HashMap<AiyatsbusEnchantment, Int>()

        for (base in enchantmentNBT) {
            val compound = base as NBTTagCompound
            val key = NamespacedKey.fromString(compound.getString("id"))
            val level = ('\uffff'.code.toShort() and compound.getShort("lvl")).toInt()
            if (key != null) {
                val enchant = aiyatsbusEt(key)
                if (enchant != null) {
                    map[enchant] = level
                }
            }
        }
        return map
    }

    override fun getFastEnchants(item: ItemStack): Array<Array<Any>> {
        val handle = (if (item is CraftItemStack) Aiyatsbus.api().getMinecraftAPI().getHelper().getCraftItemStackHandle(item) else CraftItemStack.asNMSCopy(item)) as NMSItemStack
        val enchantmentNBT = if (handle.item == Items.ENCHANTED_BOOK)
            ItemEnchantedBook.getEnchantments(handle)
        else handle.enchantmentTags

        val array = Array<Array<Any>>(enchantmentNBT.size) { arrayOf() }

        enchantmentNBT.forEachIndexed { i, base ->
            val compound = base as NBTTagCompound
            val key = NamespacedKey.fromString(compound.getString("id"))
            val level = ('\uffff'.code.toShort() and compound.getShort("lvl")).toInt()
            if (key != null) {
                val enchant = aiyatsbusEt(key)
                if (enchant != null) {
                    array[i] = arrayOf(enchant, level)
                }
            }
        }
        return array
    }

    override fun getEnchantLevel(item: ItemStack, enchant: AiyatsbusEnchantment): Int? {
        val handle = (if (item is CraftItemStack) Aiyatsbus.api().getMinecraftAPI().getHelper().getCraftItemStackHandle(item) else CraftItemStack.asNMSCopy(item)) as NMSItemStack
        val enchantmentNBT = if (handle.item == Items.ENCHANTED_BOOK)
            ItemEnchantedBook.getEnchantments(handle)
        else handle.enchantmentTags

        for (base in enchantmentNBT) {
            val compound = base as NBTTagCompound
            val key = compound.getString("id")
            if (key != enchant.id) {
                continue
            }
            return ('\uffff'.code.toShort() and compound.getShort("lvl")).toInt()
        }
        return null
    }

    override fun isUnbreakable(item: ItemStack): Boolean {
        val handle = (if (item is CraftItemStack) Aiyatsbus.api().getMinecraftAPI().getHelper().getCraftItemStackHandle(item) else CraftItemStack.asNMSCopy(item)) as NMSItemStack
        return handle.tag?.getBoolean("Unbreakable") == true
    }

    override fun isAir(item: ItemStack?): Boolean {
        if (item == null) return true
        if (item.amount == 0) return true
        val handle = (if (item is CraftItemStack) Aiyatsbus.api().getMinecraftAPI().getHelper().getCraftItemStackHandle(item) else CraftItemStack.asNMSCopy(item)) as NMSItemStack
        return handle.item is ItemAir
    }
}

typealias NMSItemStack = net.minecraft.world.item.ItemStack
