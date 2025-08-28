package cc.polarastrum.aiyatsbus.module.script.fluxon.function

import org.bukkit.command.CommandSender
import org.tabooproject.fluxon.runtime.FluxonRuntime


/**
 * Aiyatsbus
 * cc.polarastrum.aiyatsbus.module.script.fluxon.function.FnFunctions
 *
 * @author mical
 * @since 2025/8/27 20:17
 */
object FnFunctions {

    fun init() {
        with(FluxonRuntime.getInstance()) {
            registerFunction("tell", listOf(1, 2)) { context ->
                val env = context.environment
                val args = context.arguments
                val sender: CommandSender
                val message: String
                if (args.size > 1) {
                    sender = args[0] as CommandSender
                    message = args[1].toString()
                } else {
                    sender = env.get("sender", -1) as CommandSender
                    message = args[0].toString()
                }
                sender.sendMessage(message)
                null
            }
        }
    }
}