package cc.polarastrum.aiyatsbus.module.ingame.command.subcommand

import cc.polarastrum.aiyatsbus.core.*
import cc.polarastrum.aiyatsbus.core.compat.EnchantRegistrationHooks
import cc.polarastrum.aiyatsbus.core.event.AiyatsbusReloadEvent
import cc.polarastrum.aiyatsbus.core.registration.modern.ModernEnchantmentRegisterer
import cc.polarastrum.aiyatsbus.core.util.Reloadables
import cc.polarastrum.aiyatsbus.module.ingame.command.AiyatsbusCommand
import cc.polarastrum.aiyatsbus.module.ingame.mechanics.*
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import taboolib.common.platform.command.subCommand
import taboolib.common.util.ResettableLazy
import taboolib.module.lang.Language
import taboolib.platform.util.onlinePlayers

/**
 * Aiyatsbus
 * com.mcstarrysky.aiyatsbus.module.command.subcommand.Reload
 *
 * @author mical
 * @since 2024/3/3 19:33
 */
val reloadSubCommand = subCommand {
    execute<CommandSender> { sender, _, _ ->
        Aiyatsbus.isRestarting = true
        val time = System.currentTimeMillis()
        (Aiyatsbus.api().getEnchantmentRegisterer() as? ModernEnchantmentRegisterer)?.unfreezeRegistry()
        Language.reload()
        AiyatsbusSettings.conf.reload()
        // 外置添加附魔的插件要在此时完成注册
        // 这样会被添加到第三方附魔列表中
        val event = AiyatsbusReloadEvent()
        event.call()
        Reloadables.execute()
        ResettableLazy.reset()
        (Aiyatsbus.api().getEnchantmentRegisterer() as? ModernEnchantmentRegisterer)?.freezeRegistry()
        Aiyatsbus.api().getDisplayManager().getSettings().conf.reload()
        Aiyatsbus.api().getSkillHandler().getSettings().conf.reload()
        AnvilSupport.conf.reload()
        EnchantingTableSupport.conf.reload()
//        ExpModifier.conf.reload()
        GrindstoneSupport.conf.reload()
        VillagerSupport.conf.reload()
        onlinePlayers.forEach(Player::updateInventory)
        AiyatsbusCommand.init() // 重新生成 TabList
        sender.sendLang("plugin-reload", System.currentTimeMillis() - time)
        EnchantRegistrationHooks.unregisterHooks()
        EnchantRegistrationHooks.registerHooks()
        Aiyatsbus.isRestarting = false
    }
}