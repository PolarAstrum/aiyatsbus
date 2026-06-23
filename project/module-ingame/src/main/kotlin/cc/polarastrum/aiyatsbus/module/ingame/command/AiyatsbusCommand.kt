package cc.polarastrum.aiyatsbus.module.ingame.command

import cc.polarastrum.aiyatsbus.core.*
import cc.polarastrum.aiyatsbus.module.ingame.command.subcommand.*
import cc.polarastrum.aiyatsbus.core.util.variable
import org.bukkit.command.CommandSender
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.command.*
import taboolib.common.platform.command.component.CommandBase
import taboolib.common.platform.command.component.CommandComponent
import taboolib.common.platform.command.component.CommandComponentLiteral
import taboolib.common.platform.function.adaptCommandSender
import taboolib.common.platform.function.pluginVersion
import taboolib.common.platform.function.registerLifeCycleTask
import taboolib.common.util.Strings
import taboolib.module.chat.component
import taboolib.module.nms.MinecraftVersion

/**
 * Aiyatsbus
 * com.mcstarrysky.aiyatsbus.module.command.AiyatsbusCommand
 *
 * @author mical
 * @since 2024/2/17 21:02
 */
@CommandHeader(name = "aiyatsbus", permission = "aiyatsbus.command")
object AiyatsbusCommand {

    val enchantNamesAndIds = mutableListOf<String>()

    @CommandBody(permission = "aiyatsbus.command")
    val main = mainCommand {
        createTabooLegacyHelper()
    }

    @CommandBody(permission = "aiyatsbus.command.book")
    val book = bookSubCommand

    @CommandBody(permission = "aiyatsbus.command.capability")
    val capability = capabilitySubCommand

    @CommandBody(permission = "aiyatsbus.command.dev")
    val dev = devSubCommand

    @CommandBody(permission = "aiyatsbus.command.enchant")
    val enchant = enchantSubCommand

    @CommandBody(permission = "aiyatsbus.command.menu")
    val menu = menuSubCommand

    @CommandBody(permission = "aiyatsbus.command.mode")
    val mode = modeSubCommand

    @CommandBody(permission = "aiyatsbus.command.random")
    val random = randomSubCommand

    @CommandBody(permission = "aiyatsbus.command.reload")
    val reload = reloadSubCommand

    @Awake(LifeCycle.INIT)
    fun init() {
        registerLifeCycleTask(LifeCycle.ACTIVE, 999) {
            // 生成 TabList
            enchantNamesAndIds.clear()
            enchantNamesAndIds.addAll(Aiyatsbus.api().getEnchantmentManager().getEnchants().values.map { listOf(it.basicData.id, it.basicData.originName) }.flatten())
        }
    }
}

@Awake(LifeCycle.ENABLE)
private fun commandAliases() {
    if (AiyatsbusSettings.commandAliases.isNotEmpty()) {
        command(
            name = AiyatsbusSettings.commandAliases.first(),
            aliases = AiyatsbusSettings.commandAliases.drop(1),
            permission = "aiyatsbus.command"
        ) {
            AiyatsbusCommand.main.func.invoke(this)
            fun register(body: SimpleCommandBody, component: CommandComponent) {
                component.literal(body.name, *body.aliases, optional = body.optional, permission = body.permission, hidden = body.hidden) {
                    if (body.children.isEmpty()) {
                        body.func(this)
                    } else {
                        body.children.forEach { children ->
                            register(children, this)
                        }
                    }
                }
            }
            register(AiyatsbusCommand.book, this)
            register(AiyatsbusCommand.capability, this)
            register(AiyatsbusCommand.dev, this)
            register(AiyatsbusCommand.enchant, this)
            register(AiyatsbusCommand.menu, this)
            register(AiyatsbusCommand.mode, this)
            register(AiyatsbusCommand.random, this)
            register(AiyatsbusCommand.reload, this)
        }
    }
}