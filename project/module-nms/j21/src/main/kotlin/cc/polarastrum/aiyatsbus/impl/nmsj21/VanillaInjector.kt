package cc.polarastrum.aiyatsbus.impl.nmsj21

import cc.polarastrum.aiyatsbus.core.VanillaAiyatsbusEnchantment
import cc.polarastrum.aiyatsbus.core.enchantability
import cc.polarastrum.aiyatsbus.core.util.coerceBoolean
import cc.polarastrum.aiyatsbus.core.util.coerceFloat
import net.minecraft.core.Holder
import net.minecraft.core.component.DataComponents
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.item.enchantment.Enchantable
import net.minecraft.world.item.enchantment.ItemEnchantments
import org.apache.commons.lang3.mutable.MutableFloat
import org.bukkit.craftbukkit.enchantments.CraftEnchantment
import org.bukkit.craftbukkit.entity.CraftLivingEntity
import taboolib.module.incision.annotation.Operation
import taboolib.module.incision.annotation.Splice
import taboolib.module.incision.annotation.Surgeon
import taboolib.module.incision.annotation.Version
import taboolib.module.incision.api.Theatre
import taboolib.module.incision.remap.RemapRouter
import java.util.WeakHashMap

/**
 * 锋利附魔伤害结算 —— 精确拦截示例。
 *
 * 目标方法（static）：
 *   EnchantmentHelper.modifyDamage(ServerLevel, ItemStack, Entity, DamageSource, float) → float
 *
 * 原版实现：
 *   MutableFloat result = new MutableFloat(damage);
 *   runIterationOnItem(itemStack, (enchantment, level) ->
 *       enchantment.value().modifyDamage(serverLevel, level, itemStack, victim, damageSource, result));
 *   return result.floatValue();
 *
 * 本示例在 @Splice 中**不放行原逻辑**，自己复刻迭代：
 *   - 逐附魔调用真实的 Enchantment.modifyDamage()
 *   - 用 MutableFloat 前后差值拿到每个附魔的真实贡献
 *   - 对锋利做特殊处理（替换公式 / 记录真实加成）
 *   - 其他附魔原样保留
 */
@Surgeon
object VanillaInjector {

    private data class EnchantingSession(
        val enchantability: Int,
        val componentsHash: Int,
        val workingItem: ItemStack
    )

    private val enchantingSessions = WeakHashMap<ItemStack, EnchantingSession>()

    init {
        RemapRouter.nms = null
    }

    private fun workingItem(itemStack: ItemStack, enchantability: Int): ItemStack {
        synchronized(enchantingSessions) {
            val componentsHash = itemStack.components.hashCode()
            val session = enchantingSessions[itemStack]
            if (session != null && session.enchantability == enchantability && session.componentsHash == componentsHash) {
                return session.workingItem
            }
            val workingItem = itemStack.copy().apply {
                set(DataComponents.ENCHANTABLE, Enchantable(enchantability))
            }
            enchantingSessions[itemStack] = EnchantingSession(enchantability, componentsHash, workingItem)
            return workingItem
        }
    }

    fun clearEnchantingSession(itemStack: ItemStack) {
        synchronized(enchantingSessions) {
            enchantingSessions.remove(itemStack)
        }
    }

    private fun canUseEnchantingTable(itemStack: ItemStack): Boolean {
        val enchantments = itemStack.get(DataComponents.ENCHANTMENTS)
        if (enchantments == null || !enchantments.isEmpty) {
            clearEnchantingSession(itemStack)
            return false
        }
        return true
    }

    /** 在附魔台计算费用前，只为明确配置附魔能力值的目标物品补充组件。 */
    @Version(start = "1.21.2")
    @Splice(scope = "method:net.minecraft.world.item.enchantment.EnchantmentHelper#getEnchantmentCost(net.minecraft.util.RandomSource,int,int,net.minecraft.world.item.ItemStack)int")
    @Operation(id = "aiyatsbus-enchanting-table-enchantable", enabled = true)
    fun prepareEnchantable(theatre: Theatre): Any? {
        val itemStack = theatre.arg<ItemStack>(3) ?: return theatre.resume.proceed()
        if (itemStack.has(DataComponents.ENCHANTABLE)) {
            return theatre.resume.proceed()
        }
        if (!canUseEnchantingTable(itemStack)) {
            return theatre.resume.proceed()
        }

        val enchantability = itemStack.bukkitStack.enchantability
        if (enchantability <= 0) {
            return theatre.resume.proceed()
        }

        return theatre.resume.proceed(
            theatre.arg<Any>(0),
            theatre.arg<Any>(1),
            theatre.arg<Any>(2),
            workingItem(itemStack, enchantability)
        )
    }

    /** 让明确配置附魔能力值的目标物品通过附魔台的可附魔检查。 */
    @Version(start = "1.21.2")
    @Splice(scope = "method:net.minecraft.world.item.ItemStack#isEnchantable()boolean")
    @Operation(id = "aiyatsbus-enchanting-table-is-enchantable", enabled = true)
    fun isEnchantable(theatre: Theatre): Any? {
        val itemStack = theatre.selfAs<ItemStack>() ?: return theatre.resume.proceed()
        if (!itemStack.has(DataComponents.ENCHANTABLE) &&
            canUseEnchantingTable(itemStack) &&
            itemStack.bukkitStack.enchantability > 0
        ) {
            return theatre.resume.skip(true)
        }
        return theatre.resume.proceed()
    }

    /** 点击附魔台按钮时，为原版重新生成附魔列表提供同样的临时能力组件。 */
    @Version(start = "1.21.2")
    @Splice(scope = "method:net.minecraft.world.item.enchantment.EnchantmentHelper#selectEnchantment(net.minecraft.util.RandomSource,net.minecraft.world.item.ItemStack,int,java.util.stream.Stream)java.util.List")
    @Operation(id = "aiyatsbus-enchanting-table-select-enchantment", enabled = true)
    fun selectEnchantable(theatre: Theatre): Any? {
        val itemStack = theatre.arg<ItemStack>(1) ?: return theatre.resume.proceed()
        if (itemStack.has(DataComponents.ENCHANTABLE)) {
            return theatre.resume.proceed()
        }
        if (!canUseEnchantingTable(itemStack)) {
            return theatre.resume.proceed()
        }

        val enchantability = itemStack.bukkitStack.enchantability
        if (enchantability <= 0) {
            return theatre.resume.proceed()
        }

        return theatre.resume.proceed(
            theatre.arg<Any>(0),
            workingItem(itemStack, enchantability),
            theatre.arg<Any>(2),
            theatre.arg<Any>(3)
        )
    }

    @Version(start = "26.1")
    @Splice(scope = "method:net.minecraft.world.item.enchantment.EnchantmentHelper#modifyDamage(net.minecraft.server.level.ServerLevel,net.minecraft.world.item.ItemStack,net.minecraft.world.entity.Entity,net.minecraft.world.damagesource.DamageSource,float)float")
    @Operation(id = "aiyatsbus-vanilla-injector-modifyDamage", enabled = true)
    fun modifyDamage(theatre: Theatre): Any? {
        val serverLevel = theatre.arg<ServerLevel>(0) ?: return theatre.resume.proceed()
        val itemStack = theatre.arg<ItemStack>(1) ?: return theatre.resume.proceed()
        val victim = theatre.arg<Entity>(2) ?: return theatre.resume.proceed()
        val damageSource = theatre.arg<DamageSource>(3) ?: return theatre.resume.proceed()
        val baseDamage = theatre.arg<Float>(4) ?: return theatre.resume.proceed()

        val enchantments: ItemEnchantments = itemStack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY)

        val result = MutableFloat(baseDamage)
        if (enchantments.isEmpty) return theatre.resume.skip(result.toFloat())

        val entity = damageSource.entity?.bukkitEntity
        val bukkitItem = itemStack.bukkitStack

        // 不确定如果是射箭的时候力量来源是不是玩家
        if (entity !is CraftLivingEntity) return theatre.resume.skip(result.toFloat())

        for (entry in enchantments.entrySet()) {
            @Suppress("UNCHECKED_CAST")
            val holder = entry.key as Holder<Enchantment>
            val level = entry.intValue

//            println(holder.registeredName)

            when {
                holder.`is`(Enchantments.SHARPNESS) || holder.`is`(Enchantments.SMITE) || holder.`is`(Enchantments.IMPALING) -> {
                    // TODO: 性能优化 CraftEnchantment.minecraftHolderToBukkit(holder)
                    val aiyatsbusEt = CraftEnchantment.minecraftHolderToBukkit(holder) as VanillaAiyatsbusEnchantment
                    // 如果没有开启注入工具，则正常用原版的加成
                    if (aiyatsbusEt.injector == null || aiyatsbusEt.injector?.enable == false) {
                        // 用原版加成
                        holder.value().modifyDamage(serverLevel, level, itemStack, victim, damageSource, result)
                        continue
                    }
                    val vars: HashMap<String, Any?> = hashMapOf(
                        "player" to entity,
                        "item" to bukkitItem,
                        "enchant" to aiyatsbusEt,
                        "level" to level,
                        "maxLevel" to aiyatsbusEt.basicData.maxLevel
                    )
                    vars += aiyatsbusEt.variables.variables(level, bukkitItem, false)
                    // 执行前置脚本，看看用户有没有阻断附魔
                    val before = aiyatsbusEt.injector?.before?.execute(entity, vars)
                    if (!before.coerceBoolean(true)) {
                        // 阻断了，就不让附魔提供额外伤害
                        continue
                    }
                    val variable = aiyatsbusEt.injector?.value
                    // 用户没定义，或者定义了一个不存在的变量
                    if (variable == null || !aiyatsbusEt.variables.leveled.containsKey(variable)) {
                        // 用原版加成
                        holder.value().modifyDamage(serverLevel, level, itemStack, victim, damageSource, result)
                    } else {
                        val customBonus = aiyatsbusEt.variables.leveled(variable, level, false).coerceFloat()
                        result.add(customBonus)
                    }
                    // 执行后置脚本，这里不会对附魔的效果造成影响
                    aiyatsbusEt.injector?.after?.execute(entity, vars)
                }
                else -> {
                    // 其他附魔原样调用
                    holder.value().modifyDamage(serverLevel, level, itemStack, victim, damageSource, result)
                }
            }
        }

        return result.toFloat()
    }
}
