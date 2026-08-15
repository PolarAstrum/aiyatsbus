repositories {
    mavenLocal()
}

dependencies {
    // 引入 API
    compileOnly(project(":project:common"))
    compileOnly("io.papermc.paper:paper-api:1.20.2-R0.1-SNAPSHOT")
    compileOnly("org.tabooproject.fluxon:core:1.7.2")
    compileOnly("org.tabooproject.fluxon.plugin:core:1.1.10")
    compileOnly("org.tabooproject.fluxon.plugin:common:1.1.10")
    compileOnly("org.tabooproject.fluxon.plugin:platform-bukkit:1.1.10")
    // Reflex Remapper
    compileOnly("org.ow2.asm:asm:9.8")
    compileOnly("org.ow2.asm:asm-util:9.8")
    compileOnly("org.ow2.asm:asm-commons:9.8")
}

// 子模块
taboolib { subproject = true }
