package cc.polarastrum.aiyatsbus.module.ingame.builtin

import cc.polarastrum.aiyatsbus.core.data.trigger.builtin.Builtin
import cc.polarastrum.aiyatsbus.core.data.trigger.builtin.EventType
import cc.polarastrum.aiyatsbus.core.util.isNull
import com.destroystokyo.paper.event.entity.EntityJumpEvent
import org.bukkit.entity.*
import org.bukkit.event.entity.*
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.SubscribeEvent
import java.util.*

/**
 * Iweleth
 * cc.polarastrum.iweleth.listener.entries.ListenerEntity
 *
 * @author mical
 * @since 2025/8/6 16:54
 */
object ListenerEntity {

    private val projectiles = HashMap<UUID, ItemStack?>()

    @SubscribeEvent(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onDamage(e: EntityDamageEvent) {
        Builtin.execute(e.entity, EventType.DAMAGED, e, EquipmentSlot.HAND, EquipmentSlot.OFF_HAND,
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onTarget(e: EntityTargetLivingEntityEvent) {
        Builtin.execute(e.entity, EventType.BE_TARGETED, e, EquipmentSlot.HAND, EquipmentSlot.OFF_HAND,
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)
    }

    // @SubscribeEvent(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onSpawnItem(e: ItemSpawnEvent) {
        /*
        Item item = event.getEntity();
        if (item.getThrower() != null) {
            NBTUtils.write("thrown", item.getPersistentDataContainer(), PersistentDataType.INTEGER, 1);
        }
         */
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onDamageByBlock(e: EntityDamageByBlockEvent) {
        Builtin.execute(e.entity, EventType.DAMAGED_BY_BLOCK, e, EquipmentSlot.HAND, EquipmentSlot.OFF_HAND,
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onDamageByEntity(e: EntityDamageByEntityEvent) {
        // 2025/12/28 小张：为什么要忽视被横扫和风暴的伤害事件？
        // if (e.cause == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) return
        // if (e.cause == EntityDamageEvent.DamageCause.THORNS) return

        var damager = e.damager
        val damagee = e.entity
        if (damager is Projectile) {
            val projectile = damager
            if (projectile.shooter is LivingEntity) {
                damager = damager.shooter as LivingEntity
            }
            if (projectile is AbstractArrow || projectile is FishHook) {
                val item = projectiles[projectile.uniqueId]
                if (!item.isNull) {
                    Builtin.execute(damager, item!!, EventType.ATTACK_ENTITY, e, EquipmentSlot.HAND)
                }
            }
            if (projectile is Trident) {
                Builtin.execute(damager, projectile.item, EventType.ATTACK_ENTITY, e,
                    EquipmentSlot.HAND)
            }
        } else {
            Builtin.execute(damager, EventType.ATTACK_ENTITY, e, EquipmentSlot.HAND, EquipmentSlot.OFF_HAND)
        }
        Builtin.execute(damagee as? LivingEntity ?: return, EventType.DAMAGED_BY_ENTITY, e, EquipmentSlot.HAND, EquipmentSlot.OFF_HAND,
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onLaunchProjectile(e: EntityShootBowEvent) {
        Builtin.execute(e.entity, EventType.SHOOT_BOW, e, EquipmentSlot.HAND, EquipmentSlot.OFF_HAND,
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)
        projectiles += e.projectile.uniqueId to e.bow
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onDeath(e: EntityDeathEvent) {
        Builtin.execute(e.entity, EventType.DEATH, e, EquipmentSlot.HAND, EquipmentSlot.OFF_HAND,
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onJump(e: EntityJumpEvent) {
        Builtin.execute(e.entity, EventType.ENTITY_JUMP, e, EquipmentSlot.HAND, EquipmentSlot.OFF_HAND,
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onProjectileLaunch(e: ProjectileLaunchEvent) {
        Builtin.execute(e.entity.shooter as? LivingEntity ?: return, EventType.PROJECTILE_LAUNCH, e, EquipmentSlot.HAND, EquipmentSlot.OFF_HAND)
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onRegainHealth(e: EntityRegainHealthEvent) {
        Builtin.execute(e.entity, EventType.REGAIN_HEALTH, e, EquipmentSlot.HAND, EquipmentSlot.OFF_HAND,
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onProjectileHit(e: ProjectileHitEvent) {
        if (e.entity.shooter is LivingEntity) {
            if (e.entity is Trident) {
                val trident = e.entity as Trident
                if (e.hitBlock != null) {
                    Builtin.execute(e.entity.shooter as LivingEntity, EventType.PROJECTILE_HIT_BLOCK, e, trident.item,
                        EquipmentSlot.HAND, EquipmentSlot.OFF_HAND)
                }
                if (e.hitEntity != null) {
                    Builtin.execute(e.entity.shooter as LivingEntity, EventType.PROJECTILE_HIT_ENTITY, e, trident.item,
                        EquipmentSlot.HAND, EquipmentSlot.OFF_HAND)
                }
            } else {
                if (e.hitBlock != null) {
                    Builtin.execute(e.entity.shooter as LivingEntity, EventType.PROJECTILE_HIT_BLOCK, e, EquipmentSlot.HAND, EquipmentSlot.OFF_HAND)
                }
                if (e.hitEntity != null) {
                    Builtin.execute(e.entity.shooter as LivingEntity, EventType.PROJECTILE_HIT_ENTITY, e, EquipmentSlot.HAND, EquipmentSlot.OFF_HAND)
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onKill(e: EntityDeathEvent) {
        val damageEvent = e.entity.lastDamageCause
        if (damageEvent is EntityDamageByEntityEvent) {
            if (damageEvent.damager is LivingEntity) {
                Builtin.execute(damageEvent.damager, EventType.KILL, e, EquipmentSlot.HAND, EquipmentSlot.OFF_HAND)
            }
            if (damageEvent.damager is Projectile) {
                Builtin.execute((damageEvent.damager as Projectile).shooter as? LivingEntity ?: return, EventType.KILL, e, EquipmentSlot.HAND, EquipmentSlot.OFF_HAND)
            }
        }
    }
}
