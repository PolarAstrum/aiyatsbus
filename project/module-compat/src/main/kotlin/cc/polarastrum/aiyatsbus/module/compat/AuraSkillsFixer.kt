package cc.polarastrum.aiyatsbus.module.compat

import cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantment
import cc.polarastrum.aiyatsbus.core.isInGroup
import cc.polarastrum.aiyatsbus.module.ingame.mechanics.GrindstoneSupport
import org.bukkit.enchantments.Enchantment
import taboolib.module.incision.annotation.Operation
import taboolib.module.incision.annotation.Splice
import taboolib.module.incision.annotation.Surgeon
import taboolib.module.incision.api.Theatre

/**
 * Aiyatsbus
 * cc.polarastrum.aiyatsbus.module.compat.AuraSkillsFixer
 *
 * @author mical
 * @since 2026/7/27 00:06
 */
@Surgeon
object AuraSkillsFixer {

    @Splice(scope = "method:dev.aurelium.auraskills.bukkit.source.GrindstoneLeveler#isDisenchantable(org.bukkit.enchantments.Enchantment)boolean")
    @Operation(id = "aiyatsbus-aura-skills-grindstone-fixer", enabled = true)
    fun isDisenchantable(theatre: Theatre): Any? {
        val enchantment = theatre.arg<Enchantment>(0) as? AiyatsbusEnchantment ?: return theatre.resume.proceed()
        if (enchantment.enchantment.isInGroup(GrindstoneSupport.blacklist) || !enchantment.alternativeData.grindstoneable) {
            return theatre.resume.skip(false)
        }
        return theatre.resume.proceed()
    }
}