repositories {
    mavenLocal()
}

dependencies {
    // 引入 API
    compileOnly(project(":project:common"))
    compileOnly("org.tabooproject.fluxon:core:1.2.0")
    compileOnly("com.github.ben-manes.caffeine:caffeine:3.2.2")
}

// 子模块
taboolib { subproject = true }