package cc.polarastrum.aiyatsbus.impl.nmsj21

import org.bukkit.ChatColor
import org.bukkit.Location
import taboolib.module.nms.MinecraftVersion
import taboolib.module.nms.MinecraftVersion.V1_19
import io.netty.buffer.Unpooled
import net.minecraft.core.IRegistry
import net.minecraft.network.PacketDataSerializer
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntityLiving
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.phys.Vec3
import net.minecraft.world.scores.PlayerTeam
import net.minecraft.world.scores.Scoreboard
import net.minecraft.world.scores.Team
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.craftbukkit.entity.CraftEntity
import org.bukkit.craftbukkit.util.CraftChatMessage
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerQuitEvent
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.function.registerBukkitListener
import taboolib.common.util.random
import taboolib.common.util.unsafeLazy
import taboolib.library.reflex.Reflex.Companion.getProperty
import taboolib.library.reflex.Reflex.Companion.invokeConstructor
import taboolib.module.nms.nmsProxy
import taboolib.module.nms.sendPacket
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicInteger
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.getValue

abstract class NMSGlow {

    abstract fun setEntityGlowing(entity: Entity, receiver: Player, color: ChatColor)

    abstract fun unsetEntityGlowing(entity: Entity, receiver: Player)

    abstract fun setBlockGlowing(block: Block, receiver: Player, color: ChatColor)

    abstract fun unsetBlockGlowing(block: Block, receiver: Player)

    companion object {

        val instance by unsafeLazy { nmsProxy<NMSGlow>() }
    }
}

class NMSGlowImpl : NMSGlow() {

    /** 发光生物缓存 玩家 -> (EntityId -> 发光生物数据) **/
    private val glowingEntities: ConcurrentHashMap<UUID, ConcurrentHashMap<Int, GlowingEntityData>> = ConcurrentHashMap()
    /** 玩家队伍 玩家 -> (队伍颜色 -> 队伍成员 teamId)**/
    private val teams: ConcurrentHashMap<UUID, ConcurrentHashMap<ChatColor, CopyOnWriteArraySet<String>>> = ConcurrentHashMap()
    /**
     * 发光标志位与隐形标志位
     * 两个标志位均位于 Flags 索引 0 内，分别为第 5 位和第 6 位
     */
    private val invisibleFlag: Byte = (1 shl 5).toByte()
    private val glowingFlag: Byte = (1 shl 6).toByte()
    /** 发光方块缓存 玩家 -> (方块 -> 发光方块数据) **/
    private val glowingBlocks: ConcurrentHashMap<UUID, ConcurrentHashMap<BlockKey, GlowingBlockData>> = ConcurrentHashMap()

    private val entitySharedFlagsId: Any by unsafeLazy { net.minecraft.world.entity.Entity::class.java.getProperty("DATA_SHARED_FLAGS_ID", isStatic = true)!! }

    private var index = AtomicInteger(599702 + random(0, 25565))

    /**
     * int 最大值           2,147,483,647
     * tr hologram               119,789 + (0~7763)
     * lib hologram          449,599,702
     * adyeshach npc             449,599 + (0~702)
     * aiyatsbus xray            599,702 + (0~25565)
     *
     * 客户端只需要同一次会话内唯一的实体 ID，使用负数可以避开服务端真实实体 ID。
     */
    fun nextEntityId(): Int {
        return -index.getAndIncrement()
    }

    init {
        registerBukkitListener(PlayerQuitEvent::class.java) { event ->
            glowingEntities.remove(event.player.uniqueId)
            teams.remove(event.player.uniqueId)
            glowingBlocks.remove(event.player.uniqueId)
        }
        registerBukkitListener(BlockBreakEvent::class.java, EventPriority.MONITOR) { event ->
            if (glowingBlocks[event.player.uniqueId]?.get(event.block.key()) == null) return@registerBukkitListener
            unsetBlockGlowing(event.block, event.player)
        }
        registerBukkitListener(PlayerDeathEvent::class.java) { event ->
            glowingEntities.remove(event.entity.uniqueId)
            teams.remove(event.entity.uniqueId)
            glowingBlocks.remove(event.entity.uniqueId)
        }
    }

    override fun setEntityGlowing(entity: Entity, receiver: Player, color: ChatColor) {
        val teamId = if (entity is Player) entity.name else entity.uniqueId.toString()
        val flags = getEntityFlags(entity) ?: return
        setEntityGlowing0(entity.entityId, teamId, receiver, color, flags)
    }

    override fun unsetEntityGlowing(entity: Entity, receiver: Player) {
        val teamId = if (entity is Player) entity.name else entity.uniqueId.toString()
        val flags = getEntityFlags(entity) ?: return
        setEntityGlowing0(entity.entityId, teamId, receiver, null, flags)
    }

    override fun setBlockGlowing(block: Block, receiver: Player, color: ChatColor) {
        // 目前不支持空气方块发光
        if (block.type == Material.AIR) return
        val blockKey = block.key()
        val spawnLocation = Location(block.location.world, block.location.blockX.toDouble() + 0.5, block.location.blockY.toDouble(), block.location.blockZ.toDouble() + 0.5)

        // 如果不存在玩家数据
        if (!glowingBlocks.containsKey(receiver.uniqueId)) {
            // 创建发光效果并更新颜色
            val entityId = nextEntityId()
            val entityUUID = UUID.randomUUID()
            receiver.sendPacket(createDummyEntityShulkerPacket(entityId, entityUUID, spawnLocation))
            glowingBlocks.computeIfAbsent(receiver.uniqueId) { ConcurrentHashMap() }[blockKey] =
                GlowingBlockData(entityId, entityUUID.toString(), color)
            setEntityGlowing0(entityId, entityUUID.toString(), receiver, color, invisibleFlag)
        } else {
            // 如果存在方块数据
            if (glowingBlocks[receiver.uniqueId]!!.containsKey(blockKey)) {
                // 若发光颜色相同，不做任何处理
                if (glowingBlocks[receiver.uniqueId]!![blockKey]!!.color == color) return

                // 否则更新颜色
                glowingBlocks[receiver.uniqueId]!![blockKey]!!.color = color
                val entityID = glowingBlocks[receiver.uniqueId]!![blockKey]!!.entityId
                val entityUUID = glowingBlocks[receiver.uniqueId]!![blockKey]!!.entityUUID
                setEntityGlowing0(entityID, entityUUID, receiver, color, invisibleFlag)
            } else {
                // 若不存在方块数据，则注册并更新发光
                // 创建发光效果并更新颜色
                val entityId = nextEntityId()
                val entityUUID = UUID.randomUUID()
                receiver.sendPacket(createDummyEntityShulkerPacket(entityId, entityUUID, spawnLocation))
                glowingBlocks[receiver.uniqueId]!![blockKey] = GlowingBlockData(entityId, entityUUID.toString(), color)
                setEntityGlowing0(entityId, entityUUID.toString(), receiver, color, invisibleFlag)
            }
        }
    }

    override fun unsetBlockGlowing(block: Block, receiver: Player) {
        val data = glowingBlocks[receiver.uniqueId]?.remove(block.key()) ?: return
        try {
            setEntityGlowing0(data.entityId, data.entityUUID, receiver, null, invisibleFlag)
        } finally {
            receiver.sendPacket(createRemoveDummyEntityShulkerPacket(data.entityId))
        }
    }

    fun setEntityGlowing0(entityId: Int, teamId: String, receiver: Player, color: ChatColor?, otherSharedFlags: Byte) {
        // 如果不存在玩家数据
        if (!glowingEntities.containsKey(receiver.uniqueId)) {
            // 判断颜色是否为 null，如果是则直接返回
            if (color == null) return
            // 创建发光效果并更新颜色
            glowingEntities.computeIfAbsent(receiver.uniqueId) { ConcurrentHashMap() }[entityId] = GlowingEntityData(teamId, color, otherSharedFlags)
            createGlowing(receiver, entityId)
        } else {
            // 如果存在目标数据
            if (glowingEntities[receiver.uniqueId]!!.containsKey(entityId)) {
                // 若颜色为 null，则移除目标数据并返回
                if (color == null) {
                    destroyGlowing(receiver, entityId)
                    glowingEntities[receiver.uniqueId]!!.remove(entityId)
                    return
                }
                // 若发光颜色相同，不做任何处理
                if (glowingEntities[receiver.uniqueId]!![entityId]!!.color == color) return

                // 否则更新颜色
                glowingEntities[receiver.uniqueId]!![entityId]!!.color = color
                setGlowingColor(receiver, entityId)
            } else {
                // 判断颜色是否为 null，如果是不执行任何操作
                if (color == null) return
                // 否则设置颜色并更新
                glowingEntities[receiver.uniqueId]!![entityId] = GlowingEntityData(teamId, color, otherSharedFlags)
                createGlowing(receiver, entityId)
            }
        }
    }

    fun createGlowing(player: Player, entityId: Int) {
        val targetData = glowingEntities[player.uniqueId]!![entityId]!!
        player.sendPacket(createSetGlowingPacket(entityId, targetData))
        setGlowingColor(player, entityId)
    }

    fun destroyGlowing(player: Player, entityId: Int) {
        val data = glowingEntities[player.uniqueId]!![entityId]!!
        player.sendPacket(createDestroyGlowingPacket(entityId, data))
        unsetGlowingColor(player, entityId)
    }

    fun setGlowingColor(player: Player, entityId: Int) {
        val data = glowingEntities[player.uniqueId]!![entityId]!!
        val create = if (!teams.containsKey(player.uniqueId)) true else !teams[player.uniqueId]!!.containsKey(data.color)
        if (create) {
            player.sendPacket(createColorBasedTeamPacket(data.color))
        }
        player.sendPacket(createAddEntityToColorBasedTeamPacket(data.color, data.teamId))
        teams.computeIfAbsent(player.uniqueId) { ConcurrentHashMap() }.computeIfAbsent(data.color) { CopyOnWriteArraySet() }.add(data.teamId)
    }

    fun unsetGlowingColor(player: Player, entityId: Int) {
        val data = glowingEntities[player.uniqueId]!![entityId]!!
        player.sendPacket(createRemoveEntityColorBasedTeamPacket(data.color, data.teamId))
        teams[player.uniqueId]?.get(data.color)?.remove(data.teamId)
        val destroy = teams[player.uniqueId]?.get(data.color)?.isEmpty() != false
        if (destroy) {
            player.sendPacket(createDestroyColorBasedTeamPacket(data.color))
            teams[player.uniqueId]?.remove(data.color)
            if (teams[player.uniqueId]?.isEmpty() != false) {
                teams.remove(player.uniqueId)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun getEntityFlags(entity: Entity): Byte? {
        return ((entity as CraftEntity).handle).entityData.get(entitySharedFlagsId as EntityDataAccessor<Byte>)
    }

    fun createByteMeta(index: Int, value: Byte): SynchedEntityData.DataValue<Byte> {
        return SynchedEntityData.DataItem(EntityDataAccessor(index, EntityDataSerializers.BYTE), value).value()
    }

    fun createSetGlowingPacket(entityId: Int, data: GlowingEntityData): ClientboundSetEntityDataPacket {
        return ClientboundSetEntityDataPacket(entityId, listOf(createByteMeta(0, data.otherSharedFlags or glowingFlag)))
    }

    fun createDestroyGlowingPacket(entityId: Int, data: GlowingEntityData): ClientboundSetEntityDataPacket {
        return ClientboundSetEntityDataPacket(entityId, listOf(createByteMeta(0, data.otherSharedFlags)))
    }

    fun createColorBasedTeamPacket(color: ChatColor): ClientboundSetPlayerTeamPacket {
        // CREATE, REMOVE, UPDATE, ADD_ENTITIES, REMOVE_ENTITIES -> 0, 1, 2, 3, 4
        // 构造器是私有的
        return ClientboundSetPlayerTeamPacket::class.java.invokeConstructor(
            "glow-$color",
            0,
            Optional.of(ClientboundSetPlayerTeamPacket.Parameters(PlayerTeam(Scoreboard(), "glow-$color").apply {
                this.collisionRule = Team.CollisionRule.NEVER
                this.color = CraftChatMessage.getColor(color)
                this.isAllowFriendlyFire = false
                this.setSeeFriendlyInvisibles(false)
            })),
            emptyList<String>()
        )
    }

    fun createDestroyColorBasedTeamPacket(color: ChatColor): ClientboundSetPlayerTeamPacket {
        return ClientboundSetPlayerTeamPacket::class.java.invokeConstructor(
            "glow-$color",
            1,
            Optional.empty<ClientboundSetPlayerTeamPacket.Parameters>(),
            emptyList<String>()
        )
    }

    fun createAddEntityToColorBasedTeamPacket(color: ChatColor, teamId: String): ClientboundSetPlayerTeamPacket {
        return ClientboundSetPlayerTeamPacket::class.java.invokeConstructor(
            "glow-$color",
            3,
            Optional.empty<ClientboundSetPlayerTeamPacket.Parameters>(),
            listOf(teamId)
        )
    }

    fun createRemoveEntityColorBasedTeamPacket(color: ChatColor, teamId: String): ClientboundSetPlayerTeamPacket {
        return ClientboundSetPlayerTeamPacket::class.java.invokeConstructor(
            "glow-$color",
            4,
            Optional.empty<ClientboundSetPlayerTeamPacket.Parameters>(),
            listOf(teamId)
        )
    }

    fun createDummyEntityShulkerPacket(entityId: Int, entityUUID: UUID, location: Location): Any {
        // 计算视角
        val yaw = (location.yaw * 256.0f / 360.0f).toInt()
        val pitch = (location.pitch * 256.0f / 360.0f).toInt()

        return if (MinecraftVersion.isLower(V1_19)) {
            PacketPlayOutSpawnEntityLiving(PacketDataSerializer(Unpooled.buffer()).also {
                it.writeVarInt(entityId)
                it.writeUUID(entityUUID)
                it.writeVarInt(IRegistry.ENTITY_TYPE.getId(EntityTypes.SHULKER))
                it.writeDouble(location.x)
                it.writeDouble(location.y)
                it.writeDouble(location.z)
                // yRot -> yaw
                it.writeByte(yaw)
                // xRot -> pitch
                it.writeByte(pitch)
                // yHeadRot -> yaw
                it.writeByte(yaw)
                it.writeShort(0)
                it.writeShort(0)
                it.writeShort(0)
            })
        } else {
            ClientboundAddEntityPacket(
                entityId,
                entityUUID,
                location.x,
                location.y,
                location.z,
                // xRot -> pitch
                location.pitch,
                // yRot -> yaw
                location.yaw,
                EntityType.SHULKER,
                0,
                Vec3.ZERO,
                0.0
            )
        }
    }

    fun createRemoveDummyEntityShulkerPacket(entityId: Int): Any {
        return ClientboundRemoveEntitiesPacket(entityId)
    }

    class GlowingBlockData(
        val entityId: Int,
        val entityUUID: String,
        var color: ChatColor
    )

    data class BlockKey(
        val world: UUID,
        val x: Int,
        val y: Int,
        val z: Int,
    )

    private fun Block.key(): BlockKey {
        return BlockKey(world.uid, x, y, z)
    }

    class GlowingEntityData(
        val teamId: String,
        var color: ChatColor,
        val otherSharedFlags: Byte,
    )
}
