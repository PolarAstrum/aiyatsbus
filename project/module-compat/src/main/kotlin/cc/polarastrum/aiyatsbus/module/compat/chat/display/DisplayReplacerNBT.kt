/*
 *  Copyright (C) 2022-2024 PolarAstrumLab
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cc.polarastrum.aiyatsbus.module.compat.chat.display

import com.google.gson.JsonObject
import cc.polarastrum.aiyatsbus.core.Aiyatsbus
import cc.polarastrum.aiyatsbus.core.toDisplayMode
import cc.polarastrum.aiyatsbus.core.util.JSON_PARSER
import cc.polarastrum.aiyatsbus.core.util.isValidJson
import cc.polarastrum.aiyatsbus.module.compat.chat.DisplayReplacer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.bukkit.entity.Player
import taboolib.module.configuration.Configuration
import taboolib.module.configuration.Type

/**
 * Aiyatsbus
 * com.mcstarrysky.aiyatsbus.module.compat.chat.display.DisplayReplacerNBT
 *
 * @author mical
 * @since 2024/8/18 16:38
 */
object DisplayReplacerNBT : DisplayReplacer {

    private val gson = GsonComponentSerializer.gson()

    override fun apply(component: Any, player: Player): Any {
        var json = when (component) {
            is Component -> gson.serialize(component) // Adventure Component
            is String -> component // Json
            else -> Aiyatsbus.api().getMinecraftAPI().getHelper().componentToJson(component) // 大胆假设是 IChatBaseComponent
        }

        // 尝试修复 Source: '' 的警告
        if (!json.isValidJson()) return component

        try {
            // 弱者做法: 二次解析, 防止 GsonComponentSerializer 把单引号解析成 \u0027
            json = Configuration.loadFromString(json, Type.FAST_JSON).saveToString()
        } catch (_: Throwable) {
            return component
        }

        val stacks = extractHoverEvents(json)

        for (stack in stacks) {
            val id = stack.get("id").asString
            val tag = stack.get("tag")?.asString ?: continue

            val item = Aiyatsbus.api().getMinecraftAPI().getItemOperator().createItemStack(id, tag)
            val display = item.toDisplayMode(player)

            val target = display.displayName().hoverEvent(display.asHoverEvent())
            val targetStack = extractHoverEvents(gson.serialize(target)).first().get("tag")
            if (targetStack != null) {
                json = json.replace(
                    tag.flat(),
                    targetStack.asString.flat()
                )
            }
        }

        return when (component) {
            is Component -> gson.deserialize(json)
            is String -> json
            else -> Aiyatsbus.api().getMinecraftAPI().getHelper().componentFromJson(json)
        }
    }

    private fun extractHoverEvents(jsonString: String): List<JsonObject> {
        val jsonObject = JSON_PARSER.parse(jsonString).asJsonObject
        val hoverEvents = mutableListOf<JsonObject>()

        findHoverEvents(jsonObject, hoverEvents)

        return hoverEvents
    }

    private fun findHoverEvents(jsonObject: JsonObject, hoverEvents: MutableList<JsonObject>) {
        for (entry in jsonObject.entrySet()) {
            val value = entry.value
            if (value.isJsonObject) {
                if (entry.key == "hoverEvent" && value.asJsonObject.has("action") && value.asJsonObject.get("action").asString == "show_item") {
                    hoverEvents.add(value.asJsonObject.get("contents").asJsonObject)
                } else {
                    findHoverEvents(value.asJsonObject, hoverEvents)
                }
            } else if (value.isJsonArray) {
                for (element in value.asJsonArray) {
                    if (element.isJsonObject) {
                        findHoverEvents(element.asJsonObject, hoverEvents)
                    }
                }
            }
        }
    }

    private fun String.flat(): String {
        return this.replace("\"", "\\\"")
    }
}