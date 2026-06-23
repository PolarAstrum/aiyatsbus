package cc.polarastrum.aiyatsbus.impl.nmsj21

import cc.polarastrum.aiyatsbus.core.*
import cc.polarastrum.aiyatsbus.core.util.isNull
import cc.polarastrum.aiyatsbus.impl.nms.NMSItemStack
import com.google.common.collect.Maps
import net.minecraft.core.component.DataComponents
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.AirItem
import net.minecraft.world.item.trading.MerchantOffers
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.enchantments.CraftEnchantment
import org.bukkit.craftbukkit.entity.CraftLivingEntity
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.library.reflex.Reflex.Companion.setProperty
import taboolib.module.nms.MinecraftVersion
import taboolib.module.nms.MinecraftVersion.versionId
import taboolib.module.nms.remap.DynamicOpcode
import taboolib.module.nms.remap.dynamic
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

/**
 * Aiyatsbus
 * cc.polarastrum.aiyatsbus.impl.nms.nms.DefaultMinecraftItemOperator
 *
 * @author mical
 * @since 2025/8/16 08:51
 */
class DefaultMinecraftItemOperator : MinecraftItemOperator {

    override fun getRepairCost(item: ItemStack): Int {
        return (item as CraftItemStack).handle[DataComponents.REPAIR_COST] ?: 0
    }

    override fun setRepairCost(item: ItemStack, cost: Int): ItemStack {
        return item.apply {
            (this as CraftItemStack).handle[DataComponents.REPAIR_COST] = cost
        }
    }

    override fun createItemStack(material: String, tag: String?): ItemStack {
        return try {
            Bukkit.getItemFactory().createItemStack(material + tag)
        } catch (t: Throwable) {
            throw IllegalStateException(t)
        }
    }

    override fun adaptMerchantRecipe(merchantRecipeList: Any, player: Player) {

        fun adapt(item: Any, player: Player): Any {
            val bkItem = CraftItemStack.asCraftMirror(item as NMSItemStack)
            if (bkItem.isNull) return item
            return (bkItem.toDisplayMode(player) as CraftItemStack).handle
        }

        val previous = merchantRecipeList as MerchantOffers
        for (i in 0 until previous.size) {
            with(previous[i]!!) {
                baseCostA.setProperty("itemStack", adapt(baseCostA.itemStack, player))
                setProperty("costB", Optional.ofNullable(costB.getOrNull()?.also { it.setProperty("itemStack", adapt(it.itemStack, player)) }))
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

    /**
     * CraftLivingEntity#damageItemStack0
     */
    private fun damageItemStack(nmsStack: Any, amount: Int, enumItemSlot: Any?, entity: LivingEntity) {
        // 1.20.5, 1.21 -> hurtAndBreak(int, EntityLiving, EnumItemSlot), 自动广播事件
        return (nmsStack as NMSItemStack).hurtAndBreak(amount, (entity as CraftLivingEntity).handle, null)
    }

    private fun resourceLocationGetPath(resourceLocation: Any): String {
        if (versionId > 12110) {
            return dynamic(
                DynamicOpcode.INVOKEVIRTUAL,
                "net.minecraft.resources.Identifier#getPath()java.lang.String;",
                resourceLocation
            ) as String
        }
        return dynamic(
            DynamicOpcode.INVOKEVIRTUAL,
            "net.minecraft.resources.ResourceLocation#getPath()java.lang.String;",
            resourceLocation
        ) as String
    }

    private fun nmsEnchNamespacedKey(resourceKey: ResourceKey<*>): Any {
        if (versionId > 12110) {
            return dynamic(
                DynamicOpcode.INVOKEVIRTUAL,
                "net.minecraft.resources.ResourceKey#identifier()net.minecraft.resources.Identifier;",
                resourceKey
            ) as Any
        }
        return dynamic(
            DynamicOpcode.INVOKEVIRTUAL,
            "net.minecraft.resources.ResourceKey#location()net.minecraft.resources.ResourceLocation;",
            resourceKey
        ) as Any
    }

    override fun getEnchants(item: ItemStack): Map<AiyatsbusEnchantment, Int> {
        val handle: NMSItemStack = if (item is CraftItemStack) item.handle else CraftItemStack.asNMSCopy(item)
        val stored = handle.get(DataComponents.STORED_ENCHANTMENTS) ?: handle.get(DataComponents.ENCHANTMENTS) ?: return emptyMap()
        val entries = stored.entrySet()
        if (entries.isEmpty()) {
            return emptyMap()
        }
        val map = Maps.newHashMapWithExpectedSize<AiyatsbusEnchantment, Int>(entries.size)
        for (entry in entries) {
            map[aiyatsbusEtOrThrow(
                resourceLocationGetPath(nmsEnchNamespacedKey(entry.key.unwrapKey().get()))
            )] = entry.value
        }
        return map
    }

    override fun getFastEnchants(item: ItemStack): Array<Array<Any>> {
        val handle: NMSItemStack = if (item is CraftItemStack) item.handle else CraftItemStack.asNMSCopy(item)
        val stored = handle.get(DataComponents.STORED_ENCHANTMENTS) ?: handle.get(DataComponents.ENCHANTMENTS) ?: return emptyArray()
        val entries = stored.entrySet()
        if (entries.isEmpty()) {
            return emptyArray()
        }
        val array = Array<Array<Any>>(entries.size) { arrayOf() }
        entries.forEachIndexed { i, entry ->
            array[i] = arrayOf(aiyatsbusEtOrThrow(
                resourceLocationGetPath(nmsEnchNamespacedKey(entry.key.unwrapKey().get()))
            ), entry.value)
        }
        return array
    }

    override fun getEnchantLevel(item: ItemStack, enchant: AiyatsbusEnchantment): Int? {
        val handle: NMSItemStack = if (item is CraftItemStack) item.handle else CraftItemStack.asNMSCopy(item)
        val stored = handle.get(DataComponents.STORED_ENCHANTMENTS) ?: handle.get(DataComponents.ENCHANTMENTS) ?: return null
        return if (MinecraftVersion.isHigherOrEqual(MinecraftVersion.V1_21)) {
            stored.getLevel(CraftEnchantment.bukkitToMinecraftHolder(enchant.enchantment))
        } else {
            dynamic(
                DynamicOpcode.INVOKEVIRTUAL,
                "net.minecraft.world.item.enchantment#getLevel(net.minecraft.world.item.enchantment.Enchantment;)I",
                stored,
                enchant.enchantment
            ) as Int
        }
    }

    override fun isUnbreakable(item: ItemStack): Boolean {
        val handle: NMSItemStack = if (item is CraftItemStack) item.handle else CraftItemStack.asNMSCopy(item)

        /**
         * java.lang.IncompatibleClassChangeError: Found interface net.minecraft.core.component.DataComponentHolder, but class was expected
         * 所以这里为了避免这个问题, 不能用父类/接口函数 net.minecraft.core.component.DataComponentHolder#get
         * 要用 net.minecraft.world.item.ItemStack#get
         *
         * - [INVOKEVIRTUAL] — 调用实例方法（含 abstract / interface 方法）。
         *   JVM 会在运行时根据对象实际类型进行虚分派，因此无论目标方法声明在
         *   class、abstract class 还是 interface 上，一律使用此操作码即可。
         *   不需要区分 INVOKEINTERFACE，transformer 会根据目标类型自动处理
         */
        if (versionId > 12104) {
            return dynamic(
                DynamicOpcode.INVOKEVIRTUAL,
                "net.minecraft.world.item.ItemStack#get(net.minecraft.core.component.DataComponentType;)java.lang.Object;",
                handle,
                DataComponents.UNBREAKABLE
            ) != null
        }
        val unbreakable = dynamic(
            DynamicOpcode.INVOKEVIRTUAL,
            "net.minecraft.world.item.ItemStack#get(net.minecraft.core.component.DataComponentType;)java.lang.Object;",
            handle,
            DataComponents.UNBREAKABLE
        ) ?: return false
        return dynamic(
            DynamicOpcode.INVOKEVIRTUAL,
            "net.minecraft.world.item.component.Unbreakable#showInTooltip()Z",
            unbreakable
        ) as Boolean
    }

    override fun isAir(item: ItemStack?): Boolean {
        if (item == null) return true
        if (item.amount == 0) return true
        val handle = (if (item is CraftItemStack) Aiyatsbus.api().getMinecraftAPI().getHelper().getCraftItemStackHandle(item) else CraftItemStack.asNMSCopy(item)) as NMSItemStack
        return handle.item is AirItem
    }
}