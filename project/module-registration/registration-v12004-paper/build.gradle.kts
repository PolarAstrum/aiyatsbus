dependencies {
    compileOnly(project(":project:common"))
    compileOnly(project(":project:module-compat:compat-libreforge"))
    compileOnly("paper:v12004:12004:core")
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
}

// 子模块
taboolib { subproject = true }