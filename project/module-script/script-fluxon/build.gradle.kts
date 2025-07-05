repositories {
    mavenLocal()
}

dependencies {
    // 引入 API
    compileOnly(project(":project:common"))
    compileOnly("org.tabooproject.fluxon:core:1.0-SNAPSHOT")
}

// 子模块
taboolib { subproject = true }