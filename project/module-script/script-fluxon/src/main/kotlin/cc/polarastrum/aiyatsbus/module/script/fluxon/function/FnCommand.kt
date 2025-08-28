package cc.polarastrum.aiyatsbus.module.script.fluxon.function

import cc.polarastrum.aiyatsbus.core.util.replace
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.tabooproject.fluxon.runtime.FluxonRuntime
import taboolib.common.platform.function.console

/**
 * Aiyatsbus
 * cc.polarastrum.aiyatsbus.module.script.fluxon.function.FnCommand
 *
 * @author mical
 * @since 2025/8/27 20:02
 */
object FnCommand {

    fun init() {
        with(FluxonRuntime.getInstance()) {
            registerExtensionFunction(Player::class.java, "runCommand", 1) { context ->
                val player = context.target
                player?.performCommand(context.arguments[0].toString().replace("@sender", player.name))
            }
            registerExtensionFunction(Player::class.java, "runCommandAsOp", 1) { context ->
                val player = context.target
                if (player != null) {
                    val isOp = player.isOp
                    player.isOp = true
                    try {
                        player.performCommand(context.arguments[0].toString().replace("@sender", player.name))
                    } catch (ex: Throwable) {
                        ex.printStackTrace()
                    }
                    player.isOp = isOp
                }
            }
            registerFunction("runCommandAsConsole", 1) { context ->
                console().performCommand(context.arguments[0].toString().replace("@sender", "console"))
            }
        }
    }
}