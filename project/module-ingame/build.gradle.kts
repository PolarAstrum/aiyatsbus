repositories {
    // PacketEvents
    maven("https://repo.codemc.io/repository/maven-releases/")
}

dependencies {
    // 引入 API
    compileOnly(project(":project:common"))
    compileOnly("io.papermc.paper:paper-api:1.20.2-R0.1-SNAPSHOT")
    compileOnly("net.kyori:adventure-text-serializer-bungeecord:4.3.2")
    compileOnly("com.mojang:authlib:1.5.25")
    compileOnly("ink.ptms.core:v12107:12107:mapped")
    compileOnly("com.github.retrooper:packetevents-spigot:2.13.0")
}

// 子模块
taboolib { subproject = true }
