package cc.polarastrum.aiyatsbus.core.data.trigger.builtin

import cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantment
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.block.*
import org.bukkit.event.enchantment.EnchantItemEvent
import org.bukkit.event.enchantment.PrepareItemEnchantEvent
import org.bukkit.event.entity.*
import org.bukkit.event.player.*
import org.bukkit.event.weather.LightningStrikeEvent
import org.bukkit.inventory.EquipmentSlot

/**
 * 附魔事件回调接口
 *
 * 由硬编码/文件定义的内置附魔实现，用于覆盖各类事件回调。
 * 默认实现为空，实现类按需重写具体事件方法。
 *
 * @author mical
 * @since 2025/8/6 15:11
 */
@Suppress("unused")
interface EventFunctions {

    fun trigger(enchant: AiyatsbusEnchantment, level: Int, type: EventType, event: Event?, who: LivingEntity) {
    }

    fun attackEntity(enchant: AiyatsbusEnchantment, level: Int, event: EntityDamageByEntityEvent) {
    }

    fun damagedByEntity(enchant: AiyatsbusEnchantment, level: Int, event: EntityDamageByEntityEvent) {
    }

    fun damagedByBlock(enchant: AiyatsbusEnchantment, level: Int, event: EntityDamageByBlockEvent) {
    }

    fun damaged(enchant: AiyatsbusEnchantment, level: Int, event: EntityDamageEvent) {
    }

    fun advancementDone(enchant: AiyatsbusEnchantment, level: Int, event: PlayerAdvancementDoneEvent) {
    }

    fun armorstandManipulate(enchant: AiyatsbusEnchantment, level: Int, event: PlayerArmorStandManipulateEvent) {
    }

    fun bedEnter(enchant: AiyatsbusEnchantment, level: Int, event: PlayerBedEnterEvent) {
    }

    fun bedLeave(enchant: AiyatsbusEnchantment, level: Int, event: PlayerBedLeaveEvent) {
    }

    fun bucketEmpty(enchant: AiyatsbusEnchantment, level: Int, event: PlayerBucketEmptyEvent) {
    }

    fun bucketFill(enchant: AiyatsbusEnchantment, level: Int, event: PlayerBucketFillEvent) {
    }

    fun bucketEntity(enchant: AiyatsbusEnchantment, level: Int, event: PlayerBucketEntityEvent) {
    }

    fun dropItem(enchant: AiyatsbusEnchantment, level: Int, event: PlayerDropItemEvent) {
    }

    fun expChange(enchant: AiyatsbusEnchantment, level: Int, event: PlayerExpChangeEvent) {
    }

    fun fish(enchant: AiyatsbusEnchantment, level: Int, event: PlayerFishEvent) {
    }

    fun harvestBlock(enchant: AiyatsbusEnchantment, level: Int, event: PlayerHarvestBlockEvent) {
    }

    fun interactEntity(enchant: AiyatsbusEnchantment, level: Int, event: PlayerInteractAtEntityEvent) {
    }

    fun interactLeftBlock(enchant: AiyatsbusEnchantment, level: Int, event: PlayerInteractEvent) {
    }

    fun interactLeftAir(enchant: AiyatsbusEnchantment, level: Int, event: PlayerInteractEvent) {
    }

    fun interactLeft(enchant: AiyatsbusEnchantment, level: Int, event: PlayerInteractEvent) {
    }

    fun interactRightBlock(enchant: AiyatsbusEnchantment, level: Int, event: PlayerInteractEvent) {
    }

    fun interactRightAir(enchant: AiyatsbusEnchantment, level: Int, event: PlayerInteractEvent) {
    }

    fun interactRight(enchant: AiyatsbusEnchantment, level: Int, event: PlayerInteractEvent) {
    }

    fun itemBreak(enchant: AiyatsbusEnchantment, level: Int, event: PlayerItemBreakEvent) {
    }

    fun itemConsume(enchant: AiyatsbusEnchantment, level: Int, event: PlayerItemConsumeEvent) {
    }

    fun itemDamage(enchant: AiyatsbusEnchantment, level: Int, event: PlayerItemDamageEvent) {
    }

    fun itemHeld(enchant: AiyatsbusEnchantment, level: Int, event: PlayerItemHeldEvent) {
    }

    fun itemMend(enchant: AiyatsbusEnchantment, level: Int, event: PlayerItemMendEvent) {
    }

    fun levelChange(enchant: AiyatsbusEnchantment, level: Int, event: PlayerLevelChangeEvent) {
    }

    fun move(enchant: AiyatsbusEnchantment, level: Int, event: PlayerMoveEvent) {
    }

    fun pickUpArrow(enchant: AiyatsbusEnchantment, level: Int, event: PlayerPickupArrowEvent) {
    }

    fun portal(enchant: AiyatsbusEnchantment, level: Int, event: PlayerPortalEvent) {
    }

    fun recipeDiscover(enchant: AiyatsbusEnchantment, level: Int, event: PlayerRecipeDiscoverEvent) {
    }

    fun respawn(enchant: AiyatsbusEnchantment, level: Int, event: PlayerRespawnEvent) {
    }

    fun riptide(enchant: AiyatsbusEnchantment, level: Int, event: PlayerRiptideEvent) {
    }

    fun shearEntity(enchant: AiyatsbusEnchantment, level: Int, event: PlayerShearEntityEvent) {
    }

    fun swapHandItems(enchant: AiyatsbusEnchantment, level: Int, event: PlayerSwapHandItemsEvent) {
    }

    fun takeLecternBook(enchant: AiyatsbusEnchantment, level: Int, event: PlayerTakeLecternBookEvent) {
    }

    fun teleport(enchant: AiyatsbusEnchantment, level: Int, event: PlayerTeleportEvent) {
    }

    fun toggleSneak(enchant: AiyatsbusEnchantment, level: Int, event: PlayerToggleSneakEvent) {
    }

    fun toggleSprint(enchant: AiyatsbusEnchantment, level: Int, event: PlayerToggleSprintEvent) {
    }

    fun toggleFlight(enchant: AiyatsbusEnchantment, level: Int, event: PlayerToggleFlightEvent) {
    }

    fun unleashEntity(enchant: AiyatsbusEnchantment, level: Int, event: PlayerUnleashEntityEvent) {
    }

    fun blockBreak(enchant: AiyatsbusEnchantment, level: Int, event: BlockBreakEvent) {
    }

    fun blockDamageAbort(enchant: AiyatsbusEnchantment, level: Int, event: BlockDamageEvent) {
    }

    fun blockDamage(enchant: AiyatsbusEnchantment, level: Int, event: BlockDamageEvent) {
    }

    fun blockDispenseArmor(enchant: AiyatsbusEnchantment, level: Int, event: BlockDispenseArmorEvent) {
    }

    fun blockDispense(enchant: AiyatsbusEnchantment, level: Int, event: BlockDispenseEvent) {
    }

    fun blockDropItem(enchant: AiyatsbusEnchantment, level: Int, event: BlockDropItemEvent) {
    }

    fun blockFertilize(enchant: AiyatsbusEnchantment, level: Int, event: BlockFertilizeEvent) {
    }

    fun blockMultiPlace(enchant: AiyatsbusEnchantment, level: Int, event: BlockMultiPlaceEvent) {
    }

    fun blockPlace(enchant: AiyatsbusEnchantment, level: Int, event: BlockPlaceEvent) {
    }

    fun signChange(enchant: AiyatsbusEnchantment, level: Int, event: SignChangeEvent) {
    }

    fun notePlay(enchant: AiyatsbusEnchantment, level: Int, event: NotePlayEvent) {
    }

    fun enchantItem(enchant: AiyatsbusEnchantment, level: Int, event: EnchantItemEvent) {
    }

    fun prepareItemEnchant(enchant: AiyatsbusEnchantment, level: Int, event: PrepareItemEnchantEvent) {
    }

    fun lightningStrike(enchant: AiyatsbusEnchantment, level: Int, event: LightningStrikeEvent) {
    }

    fun tickTask(enchant: AiyatsbusEnchantment, level: Int, slot: EquipmentSlot, player: Player, stamp: Int) {
    }

    fun shootBow(enchant: AiyatsbusEnchantment, level: Int, event: EntityShootBowEvent) {
    }

    fun death(enchant: AiyatsbusEnchantment, level: Int, event: EntityDeathEvent) {
    }

    fun projectileLaunch(enchant: AiyatsbusEnchantment, level: Int, event: ProjectileLaunchEvent) {
    }

    fun projectileHitBlock(enchant: AiyatsbusEnchantment, level: Int, event: ProjectileHitEvent) {
    }

    fun projectileHitEntity(enchant: AiyatsbusEnchantment, level: Int, event: ProjectileHitEvent) {
    }

    fun kill(enchant: AiyatsbusEnchantment, level: Int, event: EntityDeathEvent) {
    }

    fun hunger(enchant: AiyatsbusEnchantment, level: Int, event: FoodLevelChangeEvent) {
    }

    fun regainHealth(enchant: AiyatsbusEnchantment, level: Int, event: EntityRegainHealthEvent) {
    }

    fun beTargeted(enchant: AiyatsbusEnchantment, level: Int, event: EntityTargetLivingEntityEvent) {
    }
}
