package cc.polarastrum.aiyatsbus.core.data.trigger.artifact

import cc.polarastrum.aiyatsbus.core.Aiyatsbus
import cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantment
import cc.polarastrum.aiyatsbus.core.data.trigger.TriggerType
import cc.polarastrum.aiyatsbus.core.data.trigger.builtin.Builtin
import cc.polarastrum.aiyatsbus.core.util.spawnCircleParticles
import cc.polarastrum.aiyatsbus.core.util.spawnRNAParticles
import cc.polarastrum.aiyatsbus.core.util.spawnSimpleParticle
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.Ageable
import org.bukkit.entity.Ghast
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Slime
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.inventory.EquipmentSlot
import taboolib.library.configuration.ConfigurationSection
import taboolib.library.xseries.XMaterial
import taboolib.library.xseries.particles.XParticle
import kotlin.jvm.optionals.getOrNull
import kotlin.math.min

/**
 * Aiyatsbus
 * cc.polarastrum.aiyatsbus.core.AiyatsbusArtifactHandler
 *
 * @author mical
 * @since 2026/3/17 00:31
 */
class Artifact(
    root: ConfigurationSection,
    enchant: AiyatsbusEnchantment,
    val particle: XParticle? = XParticle.of(root.getString("particle")).getOrNull(),
    val amount: Int = root.getInt("amount"),
    val specialized: Boolean = root.getBoolean("specialized", false),
    val options: Any? = root.getConfigurationSection("options")?.let { section ->
        when {
            section.contains("type") -> XMaterial.matchXMaterial(section.getString("type")!!).getOrNull()?.get()?.createBlockData()
            section.contains("red") && section.contains("green") && section.contains("blue") && section.contains("size") -> Particle.DustOptions(
                Color.fromRGB(section.getInt("red"), section.getInt("green"), section.getInt("blue")), section.getDouble("size").toFloat()
            )
            else -> null
        }
    }
) : Builtin(enchant, root, TriggerType.ARTIFACT) {

    /**
     * 专精附魔每两秒产生粒子效果
     * 普通附魔在鞘翅滑翔或装备鞋子时产生环形粒子效果
     */
    override fun tickTask(level: Int, slot: EquipmentSlot, player: Player, stamp: Int) {
        if (specialized) {
            spawnSpecializedParticle(player.location, slot)
            return
        }
        // 正在滑翔，且在胸甲位置
        if (player.isGliding && slot == EquipmentSlot.CHEST) {
            spawnSimpleParticle(player.location)
        }
        if (slot == EquipmentSlot.FEET) {
            spawnCircleParticle(player.location.clone().add(0.0, 0.2, 0.0), 2)
        }
    }

    /**
     * 专精/普通粒子附魔都在挖掘方块时产生简单粒子效果
     */
    override fun blockBreak(level: Int, event: BlockBreakEvent) {
        val triggers = Aiyatsbus.api().getArtifactHandler().getSettings().blocks
        if (triggers.contains("*") || triggers.contains(event.block.type.name)) {
            spawnSimpleParticle(event.block.location.clone().add(0.5, 0.5, 0.5))
        }
    }

    /**
     * 专精/普通粒子附魔都在攻击实体时产生双螺旋粒子效果
     */
    override fun attackEntity(level: Int, event: EntityDamageByEntityEvent) {
        if (((event.damager as? Player)?.attackCooldown ?: 1.0f) < 0.9f) {
            return
        }
        val entity = event.entity as? LivingEntity ?: return
        val size = when (entity) {
            is Slime -> min(entity.size, 3)
            is Ageable -> if (entity.isAdult) 2 else 1
            is Ghast -> 3
            else -> 2
        }
        spawnRNAParticle(entity.location.clone().add(0.0, 1.0, 0.0), size)
    }

    fun spawnRNAParticle(location: Location, size: Int) {
        with(Aiyatsbus.api().getArtifactHandler().getSettings()) {
            particle?.spawnRNAParticles(location, amount, options, height[size - 1], range[size - 1])
        }
    }

    fun spawnSimpleParticle(location: Location) {
        particle?.spawnSimpleParticle(location, amount, options)
    }

    fun spawnCircleParticle(location: Location, size: Int) {
        with(Aiyatsbus.api().getArtifactHandler().getSettings()) {
            particle?.spawnCircleParticles(location, amount, options, range[size - 1])
        }
    }

    /**
     * 生成专精粒子
     * 每一秒生成一次
     */
    @Suppress("REDUNDANT_ELSE_IN_WHEN")
    fun spawnSpecializedParticle(location: Location, slot: EquipmentSlot) {
        val type = Aiyatsbus.api().getArtifactHandler().getSettings().customParticleType[slot] ?: return
        val (range, height1) = Aiyatsbus.api().getArtifactHandler().getSettings().customParticleData[slot] ?: return
        val height2 = when (slot) {
            EquipmentSlot.HAND, EquipmentSlot.OFF_HAND -> 1.2
            EquipmentSlot.FEET -> 0.1
            EquipmentSlot.LEGS -> 0.6
            EquipmentSlot.CHEST -> 1.15
            EquipmentSlot.HEAD -> 1.8
            else -> 1.825 // FIXME: Body
        }
        val loc = location.clone().add(0.0, height2, 0.0)
        when (type) {
            "RNA" -> particle?.spawnRNAParticles(loc, amount, options, height1, range, 10, 2)
            "CIRCLE" -> particle?.spawnCircleParticles(loc.add(0.0, height1, 0.0), amount, options, range)
            "SIMPLE" -> particle?.spawnSimpleParticle(loc, amount, options)
        }
    }
}