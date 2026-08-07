package cc.polarastrum.aiyatsbus.core

import cc.polarastrum.aiyatsbus.core.data.*
import cc.polarastrum.aiyatsbus.core.data.trigger.Mechanism
import cc.polarastrum.aiyatsbus.core.data.trigger.TriggerType
import cc.polarastrum.aiyatsbus.core.data.trigger.builtin.Builtin
import cc.polarastrum.aiyatsbus.core.data.trigger.builtin.EventFunctions
import taboolib.module.configuration.Configuration

/**
 * 硬编码附魔，
 * 不会被当作 Aiyatsbus 内部附魔，而是外部附魔
 *
 * @author mical
 * @since 2026/3/5 22:04
 */
interface BuiltinAiyatsbusEnchantment : AiyatsbusEnchantment {

    class Builder {

        private var basicData: BasicData? = null
        private var alternativeData: AlternativeData? = null
        private var displayer: Displayer? = null
        private var rarity: String = AiyatsbusSettings.defaultRarity
        private var limitations: MutableList<String> = mutableListOf()
        private var targets: MutableList<String> = mutableListOf()
        private var variables: MutableMap<VariableType, MutableMap<String, String>> = mutableMapOf()

        private var eventExecutor: EventFunctions = object : EventFunctions {
        }

        fun basicData(basicData: BasicData): Builder {
            this.basicData = basicData
            return this
        }

        fun alternativeData(alternativeData: AlternativeData): Builder {
            this.alternativeData = alternativeData
            return this
        }

        fun displayer(displayer: Displayer): Builder {
            this.displayer = displayer
            return this
        }

        fun rarity(rarity: String): Builder {
            this.rarity = rarity
            return this
        }

        fun addTarget(target: String): Builder {
            this.targets += target
            return this
        }

        fun removeTarget(target: String): Builder {
            this.targets -= target
            return this
        }

        fun targets(vararg targets: String): Builder {
            this.targets = targets.toMutableList()
            return this
        }

        fun targets(targets: MutableList<String>): Builder {
            this.targets = targets
            return this
        }

        fun eventExecutor(event: EventFunctions): Builder {
            this.eventExecutor = event
            return this
        }

        fun addLimitation(type: LimitType, value: String): Builder {
            this.limitations += "${type.name}:$value"
            return this
        }

        fun removeLimitation(type: LimitType, value: String): Builder {
            this.limitations -= "${type.name}:$value"
            return this
        }

        @JvmOverloads
        fun addVariable(type: VariableType, name: String, value: String, unit: String = ""): Builder {
            val map = this.variables.getOrPut(type) { mutableMapOf() }
            map[name] = "$unit:$value"
            return this
        }

        fun removeVariable(type: VariableType, vararg variables: String): Builder {
            val map = this.variables[type] ?: return this
            variables.forEach { map.remove(it) }
            return this
        }

        fun limitations(vararg value: String): Builder {
            this.limitations = value.toMutableList()
            return this
        }

        fun limitations(values: MutableList<String>): Builder {
            this.limitations = values
            return this
        }

        fun build(): BuiltinAiyatsbusEnchantmentBase {
            val config = Configuration.Companion.empty()
            config["basic"] = basicData!!.serialize()
            if (alternativeData != null) {
                config["alternative"] = alternativeData!!.serialize()
            }
            config["display"] = displayer!!.serialize()
            config["rarity"] = rarity
            config["targets"] = targets
            config["limitations"] = limitations
            for ((type, variable) in variables) {
                for ((name, value) in variable) {
                    config["variables.${type.name.lowercase()}.${name}"] = value
                }
            }
            return BuiltinAiyatsbusEnchantmentBase(basicData!!.id, config, eventExecutor).apply ench@{
                mechanism.addTrigger(TriggerType.BUILTIN, object : Builtin(this@ench), EventFunctions by eventExecutor { })
            }
        }

        /**
         * 生成并注册
         */
        fun register(): BuiltinAiyatsbusEnchantmentBase {
            return build().also { Aiyatsbus.api().getEnchantmentManager().register(it) }
        }
    }


    companion object {

        @JvmStatic
        fun builder() = Builder()
    }
}

class BuiltinAiyatsbusEnchantmentBase(
    id: String,
    config: Configuration,
    eventExecutor: EventFunctions
) : AiyatsbusEnchantmentBase(id, null, config), BuiltinAiyatsbusEnchantment, EventFunctions by eventExecutor {

    override val mechanism: Mechanism = Mechanism(null, this)
}