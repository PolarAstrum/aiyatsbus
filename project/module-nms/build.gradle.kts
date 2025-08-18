dependencies {
    compileOnly(project(":project:common"))
    compileOnly(project(":project:common-impl"))
    compileOnly("ink.ptms.core:v12004:12004:mapped")
    compileOnly("ink.ptms.core:v11802:11802:mapped")
    compileOnly("ink.ptms.core:v11605:11605")
    compileOnly("ink.ptms:nms-all:1.0.0")
    compileOnly("paper:v12004:12004:core")
}

// 子模块
taboolib { subproject = true }