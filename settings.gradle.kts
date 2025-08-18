rootProject.name = "Aiyatsbus"

include(
    "plugin",
    // 接口
    "project:common",
    // 实现
    "project:common-impl",
    // 运行平台
    "project:module-bukkit",
    // 与其他插件兼容模块
    "project:module-compat",
    // 玩家交互代码
    "project:module-ingame",
    // 语言系统
    "project:module-language",
    // NMS 实现
    "project:module-nms",
    // 1.20.5 NMS 实现
    "project:module-nms:j21",
    // 旧版本自定义附魔注册器
    "project:module-registration:registration-legacy",
    // 1.20.4 (1.20.3) 自定义附魔注册器
    "project:module-registration:registration-v12004-paper",
    "project:module-registration:registration-v12004-vanilla",
    // 跳过 1.20.5, 1.20.6
    // 1.21 (1.21.1) 自定义附魔注册器
    "project:module-registration:registration-v12100-paper",
    "project:module-registration:registration-v12100-vanilla",
    // 1.21.3 (1.21.2) 自定义附魔注册器
    "project:module-registration:registration-v12103-paper",
    "project:module-registration:registration-v12103-vanilla",
    // 1.21.4 (1.21.5, 1.21.6, 1.21.7) 自定义附魔注册器
    "project:module-registration:registration-v12104-paper",
    "project:module-registration:registration-v12104-vanilla",
    // 脚本
    "project:module-script",
    // Fluxon 脚本实现
    "project:module-script:script-fluxon",
    // Kether 脚本实现
    "project:module-script:script-kether",
)