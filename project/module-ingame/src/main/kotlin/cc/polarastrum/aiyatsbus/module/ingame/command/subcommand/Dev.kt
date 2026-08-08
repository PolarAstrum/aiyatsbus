package cc.polarastrum.aiyatsbus.module.ingame.command.subcommand

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer
import org.bukkit.entity.Player
import taboolib.common.platform.command.subCommand
import taboolib.module.chat.component

/**
 * Runtime compatibility checks for behavior that cannot be covered by unit tests
 * without a running Bukkit server and TabooLib's relocated chat implementation.
 */
val devSubCommand = subCommand {
    execute<Player> { player, _, _ ->
        val failures = ArrayList<String>()
        var passed = 0
        componentTextCases().forEach { case ->
            val failure = runCatching {
                val legacy = parseLegacyComponent(case.input)
                val candidate = parseCandidateComponent(case.input)
                case.verify(legacy)
                compareComponentSemantics(case.name, legacy, candidate)
            }.exceptionOrNull()
            if (failure == null) {
                passed++
            } else {
                failures += "${case.name}: ${failure.message ?: failure.javaClass.simpleName}"
            }
        }

        player.sendMessage("[Aiyatsbus] ComponentText compatibility: $passed/${passed + failures.size} passed")
        failures.forEach { player.sendMessage("[Aiyatsbus] FAIL $it") }
        if (failures.isEmpty()) {
            player.sendMessage("[Aiyatsbus] PASS: ComponentText semantics are compatible with the current display path.")
        } else {
            player.sendMessage("[Aiyatsbus] FAILED: do not enable the optimized display path.")
        }
    }
}

private data class ComponentTextCase(
    val name: String,
    val input: String,
    val verify: (Component) -> Unit,
)

/** The current JSON baseline used by the production display path. */
private fun parseLegacyComponent(input: String): Component {
    return GsonComponentSerializer.gson()
        .deserialize(input.component().buildColored().toRawMessage())
        .decoration(TextDecoration.ITALIC, false)
}

/** Candidate path: retain TabooLib parsing, but avoid calling ComponentText.toRawMessage(). */
private fun parseCandidateComponent(input: String): Component {
    val baseComponent = input.component().buildColored().toSpigotObject()
    return BungeeComponentSerializer.get()
        .deserialize(arrayOf(baseComponent))
        .decoration(TextDecoration.ITALIC, false)
}

private fun compareComponentSemantics(name: String, expected: Component, actual: Component) {
    val expectedSignature = expected.semanticSignature()
    val actualSignature = actual.semanticSignature()
    check(expectedSignature == actualSignature) {
        "candidate differs from legacy at $name\nlegacy=$expectedSignature\ncandidate=$actualSignature"
    }
}

private fun componentTextCases(): List<ComponentTextCase> {
    return listOf(
        ComponentTextCase("hex-braces", "&{#00FFFF}我操你妈") { component ->
            component.assertPlain("我操你妈")
            component.requireText("我操你妈").assertColor(TextColor.color(0x00FFFF))
            component.assertLoreItalicDisabled()
        },
        ComponentTextCase("simple-bold-red", "[我操你妈](b;color=red)") { component ->
            component.assertPlain("我操你妈")
            component.requireText("我操你妈").run {
                assertColor(NamedTextColor.RED)
                assertDecoration(TextDecoration.BOLD, TextDecoration.State.TRUE)
            }
            component.assertLoreItalicDisabled()
        },
        ComponentTextCase("combined-decorations", "[format](b;i;u;s;color=#12AB34)") { component ->
            component.requireText("format").run {
                assertColor(TextColor.color(0x12AB34))
                assertDecoration(TextDecoration.BOLD, TextDecoration.State.TRUE)
                assertDecoration(TextDecoration.ITALIC, TextDecoration.State.TRUE)
                assertDecoration(TextDecoration.UNDERLINED, TextDecoration.State.TRUE)
                assertDecoration(TextDecoration.STRIKETHROUGH, TextDecoration.State.TRUE)
            }
        },
        ComponentTextCase("local-style", "before[red](color=red)after") { component ->
            component.assertPlain("beforeredafter")
            component.requireText("red").assertColor(NamedTextColor.RED)
        },
        ComponentTextCase("nested-style", "[outer[inner](b)](color=red)") { component ->
            component.assertPlain("outerinner")
            component.requireEffectiveText("inner").run {
                assertDecoration(TextDecoration.BOLD, TextDecoration.State.TRUE)
            }
        },
        ComponentTextCase("click-command", "[run](command=/aiyatsbus)") { component ->
            val click = component.requireText("run").clickEvent()
                ?: error("missing click event")
            check(click.action() == ClickEvent.Action.RUN_COMMAND) { "click action=${click.action()}" }
            check(click.value() == "/aiyatsbus") { "click value=${click.value()}" }
        },
        ComponentTextCase("click-suggest", "[suggest](suggest=/aiyatsbus )") { component ->
            val click = component.requireText("suggest").clickEvent()
                ?: error("missing click event")
            check(click.action() == ClickEvent.Action.SUGGEST_COMMAND) { "click action=${click.action()}" }
            check(click.value() == "/aiyatsbus") { "click value=${click.value()}" }
        },
        ComponentTextCase("click-copy", "[copy](copy=value)") { component ->
            val click = component.requireText("copy").clickEvent()
                ?: error("missing click event")
            check(click.action() == ClickEvent.Action.COPY_TO_CLIPBOARD) { "click action=${click.action()}" }
            check(click.value() == "value") { "click value=${click.value()}" }
        },
        ComponentTextCase("hover", "[hover](hover=details)") { component ->
            check(component.requireText("hover").hoverEvent() != null) { "missing hover event" }
        },
        ComponentTextCase("font", "[font](font=minecraft:uniform)") { component ->
            check(component.requireText("font").font() == Key.key("minecraft:uniform")) {
                "font=${component.requireText("font").font()}"
            }
        },
        ComponentTextCase("gradient", "[ABC](gradient=#00FFFF,#FF0000)") { component ->
            component.assertPlain("ABC")
            val colors = component.textNodes().filter { it.content().isNotEmpty() }.mapNotNull { it.color() }.toSet()
            check(colors.size >= 2) { "gradient colors=$colors" }
        },
        ComponentTextCase("escaped-simple-syntax", "\\[text\\]\\(b\\;color=red\\)") { component ->
            component.assertPlain("[text](b;color=red)")
        },
        ComponentTextCase("multiline", "first\n[second](b;color=red)") { component ->
            component.assertPlain("first\nsecond")
            component.requireText("second").run {
                assertColor(NamedTextColor.RED)
                assertDecoration(TextDecoration.BOLD, TextDecoration.State.TRUE)
            }
        },
    )
}

private fun Component.assertPlain(expected: String) {
    val actual = PlainTextComponentSerializer.plainText().serialize(this)
    check(actual == expected) { "plain expected=<$expected> actual=<$actual>" }
}

private fun Component.assertLoreItalicDisabled() {
    check(decoration(TextDecoration.ITALIC) == TextDecoration.State.FALSE) {
        "root italic=${decoration(TextDecoration.ITALIC)}"
    }
}

private fun Component.requireText(content: String): TextComponent {
    return textNodes().firstOrNull { it.content() == content }
        ?: error("missing text node <$content>; nodes=${textNodes().map { it.content() }}")
}

private fun Component.textNodes(): List<TextComponent> {
    return buildList {
        fun visit(component: Component) {
            if (component is TextComponent) add(component)
            component.children().forEach(::visit)
        }
        visit(this@textNodes)
    }
}

private data class SemanticNode(
    val text: String,
    val color: TextColor?,
    val decorations: Map<TextDecoration, TextDecoration.State>,
    val click: String?,
    val hover: String?,
    val font: Key?,
    val children: List<SemanticNode>,
)

private fun Component.semanticSignature(): SemanticNode {
    fun visit(component: Component, parentColor: TextColor?, parentDecorations: Map<TextDecoration, TextDecoration.State>): SemanticNode {
        val color = component.color() ?: parentColor
        val decorations = TextDecoration.values().associateWith { decoration ->
            component.decoration(decoration).takeUnless { it == TextDecoration.State.NOT_SET }
                ?: parentDecorations[decoration]
                ?: TextDecoration.State.NOT_SET
        }
        return SemanticNode(
            text = (component as? TextComponent)?.content() ?: "",
            color = color,
            decorations = decorations,
            click = component.clickEvent()?.let { "${it.action()}:${it.value()}" },
            hover = component.hoverEvent()?.toString(),
            font = component.font(),
            children = component.children().map { visit(it, color, decorations) },
        )
    }
    return visit(this, null, emptyMap())
}

private data class EffectiveTextNode(
    val component: TextComponent,
    val color: TextColor?,
    val decorations: Map<TextDecoration, TextDecoration.State>,
)

private fun Component.requireEffectiveText(content: String): EffectiveTextNode {
    return effectiveTextNodes().firstOrNull { it.component.content() == content }
        ?: error("missing effective text node <$content>; nodes=${effectiveTextNodes().map { it.component.content() }}")
}

private fun Component.effectiveTextNodes(): List<EffectiveTextNode> {
    return buildList {
        fun visit(
            component: Component,
            inheritedColor: TextColor?,
            inheritedDecorations: Map<TextDecoration, TextDecoration.State>,
        ) {
            val effectiveColor = component.color() ?: inheritedColor
            val effectiveDecorations = TextDecoration.values().associateWith { decoration ->
                component.decoration(decoration).takeUnless { it == TextDecoration.State.NOT_SET }
                    ?: inheritedDecorations[decoration]
                    ?: TextDecoration.State.NOT_SET
            }
            if (component is TextComponent) {
                add(EffectiveTextNode(component, effectiveColor, effectiveDecorations))
            }
            component.children().forEach { visit(it, effectiveColor, effectiveDecorations) }
        }
        visit(this@effectiveTextNodes, null, emptyMap())
    }
}

private fun TextComponent.assertColor(expected: TextColor) {
    check(color() == expected) { "text=<${content()}> color expected=$expected actual=${color()}" }
}

private fun TextComponent.assertDecoration(decoration: TextDecoration, expected: TextDecoration.State) {
    val actual = decoration(decoration)
    check(actual == expected) { "text=<${content()}> $decoration expected=$expected actual=$actual" }
}

private fun EffectiveTextNode.assertColor(expected: TextColor) {
    check(color == expected) {
        "text=<${component.content()}> effective color expected=$expected actual=$color local=${component.color()}"
    }
}

private fun EffectiveTextNode.assertDecoration(decoration: TextDecoration, expected: TextDecoration.State) {
    val actual = decorations[decoration] ?: TextDecoration.State.NOT_SET
    check(actual == expected) {
        "text=<${component.content()}> effective $decoration expected=$expected actual=$actual local=${component.decoration(decoration)}"
    }
}
