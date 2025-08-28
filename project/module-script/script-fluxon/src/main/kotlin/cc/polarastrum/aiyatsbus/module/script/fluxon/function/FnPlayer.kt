@file:Suppress("DEPRECATION")

package cc.polarastrum.aiyatsbus.module.script.fluxon.function

import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.entity.Player
import org.tabooproject.fluxon.runtime.FluxonRuntime
import org.tabooproject.fluxon.runtime.stdlib.Coerce
import java.util.function.BiConsumer
import java.util.function.Function

/**
 * fluxon
 * org.tabooproject.fluxon.extension.bukkit.function.game.PlayerOperators
 *
 * @author lynn
 * @since 2025/7/20
 */
enum class FnPlayer(
    private val func: String,
    private val reader: Function<Player, Any?>? = null,
    private val writer: BiConsumer<Player, Any?>? = null
) {
    
    LOCALE("locale", { player -> player.locale }),

    WORLD("world", { player -> player.location.world }),

    X("x", { player -> player.location.x }),

    Y("y", { player -> player.location.y }),

    Z("z", { player -> player.location.z }),

    YAW("yaw",
        { player -> player.location.yaw },
        { player, value ->
            Coerce.asFloat(value).ifPresent { yaw ->
                val location = player.location
                location.yaw = yaw
                player.teleport(location)
            }
        }
    ),

    PITCH("pitch",
        { player -> player.location.pitch },
        { player, value ->
            Coerce.asFloat(value).ifPresent { pitch ->
                val location = player.location
                location.pitch = pitch
                player.teleport(location)
            }
        }
    ),

    BLOCK_X("blockX", { player -> player.location.blockX }),

    BLOCK_Y("blockY", { player -> player.location.blockY }),

    BLOCK_Z("blockZ", { player -> player.location.blockZ }),

    COMPASS_X("compassX", { player -> player.compassTarget.blockX }),

    COMPASS_Y("compassY", { player -> player.compassTarget.blockY }),

    COMPASS_Z("compassZ", { player -> player.compassTarget.blockZ }),

    LOCATION("location",
        { obj -> obj.location },
        { player, value ->
            if (value is Location) {
                player.teleport(value)
            }
        }
    ),

    COMPASS_TARGET("compassTarget",
        { obj -> obj.compassTarget },
        { player, value ->
            if (value is Location) {
                player.compassTarget = value
            }
        }
    ),

    BED_SPAWN("bedSpawn",
        { obj -> obj.bedSpawnLocation },
        { player, value ->
            if (value is Location) {
                player.bedSpawnLocation = value
            } else if (value == null) {
                player.bedSpawnLocation = null
            }
        }
    ),

    BED_SPAWN_X("bedSpawnX", { player ->
        val bed = player.bedSpawnLocation
        bed?.blockX
    }),

    BED_SPAWN_Y("bedSpawnY", { player ->
        val bed = player.bedSpawnLocation
        bed?.blockY
    }),

    BED_SPAWN_Z("bedSpawnZ", { player ->
        val bed = player.bedSpawnLocation
        bed?.blockZ
    }),

    NAME("name", { obj -> obj.name }),

    LIST_NAME("listName",
        { obj -> obj.playerListName },
        { player, value ->
            player.setPlayerListName(value.toString())
        }
    ),

    DISPLAY_NAME("displayName",
        { obj -> obj.displayName },
        { player, value ->
            player.setDisplayName(value.toString())
        }
    ),

    UUID("uuid", { player -> player.uniqueId.toString() }),

    GAMEMODE("gamemode",
        { player -> player.gameMode.name },
        { player, value ->
            val mode = value.toString().uppercase()
            val gameMode = when (mode) {
                "SURVIVAL", "0" -> GameMode.SURVIVAL
                "CREATIVE", "1" -> GameMode.CREATIVE
                "ADVENTURE", "2" -> GameMode.ADVENTURE
                "SPECTATOR", "3" -> GameMode.SPECTATOR
                else -> throw IllegalArgumentException("Unknown GameMode: $value")
            }
            player.gameMode = gameMode
        }
    ),

    ADDRESS("address", { player ->
        val addr = player.address
        addr?.hostString
    }),

    SNEAKING("sneaking", { obj -> obj.isSneaking }),

    SPRINTING("sprinting", { obj -> obj.isSprinting }),

    BLOCKING("blocking", { obj -> obj.isBlocking }),

    GLIDING("gliding",
        { obj -> obj.isGliding },
        { player, value ->
            Coerce.asBoolean(value).ifPresent { player.isGliding = it }
        }
    ),

    GLOWING("glowing",
        { obj -> obj.isGlowing },
        { player, value ->
            Coerce.asBoolean(value).ifPresent { player.isGlowing = it }
        }
    ),

    SWIMMING("swimming",
        { obj -> obj.isSwimming },
        { player, value ->
            Coerce.asBoolean(value).ifPresent { player.isSwimming = it }
        }
    ),

    RIPTIDING("riptiding", { obj -> obj.isRiptiding }),

    SLEEPING("sleeping", { obj -> obj.isSleeping }),

    SLEEP_TICKS("sleepTicks", { obj -> obj.sleepTicks }),

    SLEEP_IGNORED("sleepIgnored",
        { obj -> obj.isSleepingIgnored },
        { player, value ->
            Coerce.asBoolean(value).ifPresent { player.isSleepingIgnored = it }
        }
    ),

    DEAD("dead", { obj -> obj.isDead }),

    CONVERSING("conversing", { obj -> obj.isConversing }),

    LEASHED("leashed", { obj -> obj.isLeashed }),

    ON_GROUND("onGround", { obj -> obj.isOnGround }),

    IS_ONLINE("isOnline", { obj -> obj.isOnline }),

    INSIDE_VEHICLE("insideVehicle", { obj -> obj.isInsideVehicle }),

    OP("op",
        { obj -> obj.isOp },
        { player, value ->
            Coerce.asBoolean(value).ifPresent { player.isOp = it }
        }
    ),

    GRAVITY("gravity",
        { obj -> obj.hasGravity() },
        { player, value ->
            Coerce.asBoolean(value).ifPresent { player.setGravity(it) }
        }
    ),

    ATTACK_COOLDOWN("attackCooldown", { obj -> obj.attackCooldown }),

    PLAYER_TIME("playerTime",
        { obj -> obj.playerTime },
        { player, value ->
            Coerce.asLong(value).ifPresent { time -> player.setPlayerTime(time, true) }
        }
    ),

    FIRST_PLAYED("firstPlayed", { obj -> obj.firstPlayed }),

    LAST_PLAYED("lastPlayed", { obj -> obj.lastPlayed }),

    ABSORPTION_AMOUNT("absorptionAmount",
        { obj -> obj.absorptionAmount },
        { player, value ->
            Coerce.asDouble(value).ifPresent { amount ->
                player.absorptionAmount = amount.coerceAtLeast(0.0)
            }
        }
    ),

    NO_DAMAGE_TICKS("noDamageTicks",
        { obj -> obj.noDamageTicks },
        { player, value ->
            Coerce.asInteger(value).ifPresent { ticks ->
                player.noDamageTicks = ticks.coerceAtLeast(0)
            }
        }
    ),

    REMAINING_AIR("remainingAir",
        { obj -> obj.remainingAir },
        { player, value ->
            Coerce.asInteger(value).ifPresent { air ->
                player.remainingAir = air.coerceAtLeast(0)
            }
        }
    ),

    MAXIMUM_AIR("maximumAir", { obj -> obj.maximumAir }),

    EXP("exp",
        { obj -> obj.exp },
        { player, value ->
            Coerce.asFloat(value).ifPresent { exp ->
                player.exp = exp.coerceIn(0f, 1f)
            }
        }
    ),

    LEVEL("level",
        { obj -> obj.level },
        { player, value ->
            Coerce.asInteger(value).ifPresent { level ->
                player.level = level.coerceAtLeast(0)
            }
        }
    ),

    EXHAUSTION("exhaustion",
        { obj -> obj.exhaustion },
        { player, value ->
            Coerce.asFloat(value).ifPresent { player.exhaustion = it }
        }
    ),

    SATURATION("saturation",
        { obj -> obj.saturation },
        { player, value ->
            Coerce.asFloat(value).ifPresent { saturation ->
                player.saturation = saturation.coerceIn(0f, 20f)
            }
        }
    ),

    FOOD_LEVEL("foodLevel",
        { obj -> obj.foodLevel },
        { player, value ->
            Coerce.asInteger(value).ifPresent { food ->
                player.foodLevel = food.coerceIn(0, 20)
            }
        }
    ),

    HEALTH("health",
        { obj -> obj.health },
        { player, value ->
            Coerce.asDouble(value).ifPresent { health ->
                player.health = health.coerceIn(0.0, player.maxHealth)
            }
        }
    ),

    MAX_HEALTH("maxHealth",
        { obj -> obj.maxHealth },
        { player, value ->
            Coerce.asDouble(value).ifPresent { maxHealth ->
                player.maxHealth = maxHealth.coerceAtLeast(0.0)
            }
        }
    ),

    ALLOW_FLIGHT("allowFlight",
        { obj -> obj.allowFlight },
        { player, value ->
            Coerce.asBoolean(value).ifPresent { player.allowFlight = it }
        }
    ),

    FLYING("flying",
        { obj -> obj.isFlying },
        { player, value ->
            Coerce.asBoolean(value).ifPresent { player.isFlying = it }
        }
    ),

    FLY_SPEED("flySpeed",
        { obj -> obj.flySpeed },
        { player, value ->
            Coerce.asFloat(value).ifPresent { speed ->
                player.flySpeed = speed.coerceIn(-0.99f, 0.99f)
            }
        }
    ),

    WALK_SPEED("walkSpeed",
        { obj -> obj.walkSpeed },
        { player, value ->
            Coerce.asFloat(value).ifPresent { speed ->
                player.walkSpeed = speed.coerceIn(-0.99f, 0.99f)
            }
        }
    ),

    PING("ping", { obj -> obj.ping }),

    POSE("pose", { obj -> obj.pose }),

    FACING("facing", { obj -> obj.facing });

    fun getName(): String {
        return name
    }

    fun get(player: Player): Any? {
        return reader?.apply(player)
    }

    fun set(player: Player, value: Any?) {
        writer?.accept(player, value)
    }

    companion object {

        fun init() {
            with(FluxonRuntime.getInstance()) {
                for (operator in values()) {
                    registerExtensionFunction(
                        Player::class.java,
                        operator.func,
                        listOf(0, 1)
                    ) { context ->
                        if (context.arguments.isNotEmpty()) {
                            if (operator.writer != null) {
                                operator.set(context.target as Player, context.arguments[0])
                            }
                            null
                        } else {
                            if (operator.reader != null) {
                                operator.get(context.target as Player)
                            } else null
                        }
                    }
                }
            }
        }
    }
}