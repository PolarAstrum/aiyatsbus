repositories {
    maven("https://libraries.minecraft.net")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")
    compileOnly("io.papermc.paper:paper-api:1.21.5-R0.1-SNAPSHOT")
    compileOnly("paper:v12104:12104:core")
    compileOnly("paper:v12111:12111:core")
    compileOnly("ink.ptms.core:v11802:11802-minimize:mapped")
    compileOnly(project(":project:module-nms"))
    compileOnly(project(":project:common"))
    compileOnly("com.mojang:brigadier:1.2.9")
    compileOnly("com.mojang:authlib:1.5.25")
    compileOnly("io.netty:netty-all:4.1.86.Final")
}

// 编译配置
java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

// 子模块
taboolib { subproject = true }
