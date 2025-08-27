repositories {
    maven("https://libraries.minecraft.net")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.5-R0.1-SNAPSHOT")
    compileOnly("paper:v12104:12104:core")
    compileOnly("paper:v12004:12004:core")
    compileOnly("ink.ptms.core:v12105:12105:mapped")
    compileOnly(project(":project:module-nms"))
    compileOnly(project(":project:common"))
    compileOnly("com.mojang:brigadier:1.2.9")
}

// 编译配置
java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_HIGHER
    targetCompatibility = JavaVersion.VERSION_HIGHER
}

// 子模块
taboolib { subproject = true }