package cc.polarastrum.aiyatsbus.module.script.fluxon.function.game

import cc.polarastrum.aiyatsbus.core.util.Vectors
import cc.polarastrum.aiyatsbus.core.util.checkIfIsNPC
import cc.polarastrum.aiyatsbus.core.util.equippedItems
import cc.polarastrum.aiyatsbus.core.util.isBehind
import cc.polarastrum.aiyatsbus.core.util.realDamage
import cc.polarastrum.aiyatsbus.module.script.fluxon.FluxonScriptHandler
import cc.polarastrum.aiyatsbus.module.script.fluxon.relocate.FluxonRelocate
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.util.Vector
import org.tabooproject.fluxon.runtime.FluxonRuntime
import org.tabooproject.fluxon.runtime.FunctionSignature.returns
import org.tabooproject.fluxon.runtime.Type
import org.tabooproject.fluxon.runtime.java.Export
import org.tabooproject.fluxon.runtime.java.Optional
import taboolib.common.LifeCycle
import taboolib.common.Requires
import taboolib.common.platform.Awake
import taboolib.library.xseries.XPotion
import taboolib.module.nms.getI18nName

/**
 * Aiyatsbus
 * cc.polarastrum.aiyatsbus.module.script.fluxon.function.game.FnEntity
 *
 * @author mical
 * @since 2026/1/2 00:09
 */
@Requires(missingClasses = ["!org.tabooproject.fluxon.ParseScript"])
@FluxonRelocate
object FnEntity {

    val TYPE = Type.fromClass(FnEntity::class.java)!!

    @Awake(LifeCycle.LOAD)
    fun init() {
        FluxonScriptHandler.DEFAULT_PACKAGE_AUTO_IMPORT += "aiy:entity"
        with(FluxonRuntime.getInstance()) {
            registerFunction("aiy:entity", "entity", returns(TYPE).noParams()) { it.setReturnRef(FnEntity) }
            exportRegistry.registerClass(FnEntity::class.java, "aiy:entity")
        }
    }

    @Export
    fun equippedItems(entity: LivingEntity): Map<EquipmentSlot, ItemStack> {
        return entity.equippedItems
    }

    @Export
    fun realDamage(entity: LivingEntity, damage: Double, @Optional by: Entity?) {
        entity.realDamage(damage, by)
    }

    @Export
    fun entityName(entity: Entity, @Optional player: Player?): String {
        return if (entity is Player) entity.name else entity.customName ?: entity.getI18nName(player)
    }

    @Export
    fun addSafetyVelocity(entity: LivingEntity, vector: Vector, @Optional checkKnockback: Boolean?) {
        Vectors.addVelocity(entity, vector, checkKnockback ?: false)
    }

    @Export
    fun isBehind(entity1: LivingEntity, entity2: LivingEntity): Boolean {
        return entity1.isBehind(entity2)
    }

    @Export
    fun addPotionEffect(
        entity: LivingEntity,
        type: String,
        duration: Int,
        amplifier: Int,
        @Optional ambient: Boolean?,
        @Optional particles: Boolean?,
        @Optional icon: Boolean?
    ) {
        entity.addPotionEffect(
            PotionEffect(
                XPotion.of(type).orElseThrow().potionEffectType ?: return,
                duration, amplifier, ambient ?: true, particles ?: true, icon ?: true
            )
        )
    }

    @Export
    fun getActivePotionEffect(entity: LivingEntity, type: String): PotionEffect? {
        return entity.activePotionEffects.filter {
            it.type == (XPotion.of(type).orElseThrow().potionEffectType)
        }.firstOrNull()
    }

    @Export
    fun hasPotionEffect(entity: LivingEntity, type: String): Boolean {
        return entity.hasPotionEffect(XPotion.of(type).orElseThrow().potionEffectType ?: return false )
    }

    @Export
    fun removePotionEffect(entity: LivingEntity, type: String) {
        entity.removePotionEffect(XPotion.of(type).orElseThrow().potionEffectType ?: return)
    }

    @Export
    fun isNPC(entity: Entity?): Boolean = entity.checkIfIsNPC()

    /**
     * 较为常用
     */
    @Export
    fun isLivingEntity(entity: Entity?): Boolean = entity is LivingEntity

    /**
     * 较为常用
     */
    @Export
    fun isPlayer(entity: Entity?): Boolean = entity is Player
}
