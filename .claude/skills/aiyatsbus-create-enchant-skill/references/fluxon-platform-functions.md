# Fluxon 平台函数

本文描述 Fluxon 平台层提供的 String 扩展、任务调度和 Folia 区域调度函数。它们不是 Aiyatsbus 的 `aiy:*` 游戏函数。

Aiyatsbus 附魔脚本可以直接使用这些函数，不需要手动导入平台模块。已有平台函数可以满足需求时，不要通过 JVM 互操作重复实现。

## String 扩展

以下函数通过 String 扩展调用。读取字符串变量时，必须使用 `&变量`：

```fluxon
text = "1h30m"
millis = &text::parseMillis()
```

### `parseMillis`

```text
String::parseMillis() -> Long
```

把时间跨度解析为毫秒。解析时会先转换为小写，支持以下单位：

| 单位 | 毫秒换算 |
| --- | ---: |
| `d` | `86400000` |
| `h` | `3600000` |
| `m` | `60000` |
| `s` | `1000` |

支持组合值和小数：

```fluxon
day = "1d"::parseMillis()
duration = "1d1h30m"::parseMillis()
seconds = "2.5s"::parseMillis()
```

没有单位的尾部数字不会加入结果，因此 `"100"::parseMillis()` 返回 `0L`。空字符串或没有有效单位时也返回 `0L`。单位对应的数字无法转换为小数时会抛出转换异常。

### `parseUUID`

```text
String::parseUUID() -> UUID
```

```fluxon
uuidText = "550e8400-e29b-41d4-a716-446655440000"
uuid = &uuidText::parseUUID()
```

### `colored`

```text
String::colored() -> String
```

```fluxon
message = "&a成功"::colored()
```

这是平台层 String 扩展，不属于 Aiyatsbus `aiy:common` 模块。

### `uncolored`

```text
String::uncolored() -> String
```

```fluxon
plain = "&a成功"::uncolored()
```

这是平台层 String 扩展，不属于 Aiyatsbus `aiy:common` 模块。

### `parseToHexColor`

```text
String::parseToHexColor() -> String
```

```fluxon
hexColor = "#FF8800"::parseToHexColor()
```

## 任务函数

任务的 `delay` 和 `period` 单位都是 tick。任务函数接收 Lambda，并返回 `PlatformTask?`。

| 函数 | 语义 |
| --- | --- |
| `run(fn)` | 同步立即执行 |
| `runAsync(fn)` | 异步立即执行 |
| `runLater(delay, fn)` | 同步延迟执行 |
| `runAsyncLater(delay, fn)` | 异步延迟执行 |
| `runTimer(delay, period, fn)` | 同步周期执行 |
| `runAsyncTimer(delay, period, fn)` | 异步周期执行 |

```fluxon
run(|| common::debug("立即执行"))

task = runLater(20L, || {
    common::debug("延迟 20 tick 执行")
})

periodic = runTimer(0L, 20L, || {
    common::debug("每 20 tick 执行")
})
```

任务注册失败时返回值可能为 `null`。异步任务中的 Lambda 不要直接调用非线程安全的 Bukkit API。周期任务会注册到当前 Fluxon 脚本资源中，脚本资源释放时自动取消。

取消任务：

```fluxon
if &periodic != null {
    &periodic::cancel()
}
```

## `submit` 任务构建器

```text
submit() -> TaskBuilder
TaskBuilder::async() -> TaskBuilder
TaskBuilder::delay(ticks: Long) -> TaskBuilder
TaskBuilder::period(ticks: Long) -> TaskBuilder
TaskBuilder::run(fn: Function) -> PlatformTask?
```

```fluxon
task = submit()
    ::delay(20L)
    ::period(20L)
    ::run(|| common::debug("每 20 tick 执行"))
```

`async()`、`delay(...)` 和 `period(...)` 返回构建器自身；调用 `run(...)` 后才会注册任务。

## Folia 区域调度

Folia 扩展为 `TaskBuilder` 提供：

```text
TaskBuilder::on(target) -> TaskBuilder
TaskBuilder::scheduler(enabled: Boolean) -> TaskBuilder
```

当前 `on(...)` 支持 `Entity`、`Location`、`Block` 和 `Chunk`：

```fluxon
task = submit()
    ::on(&entity)
    ::delay(1L)
    ::period(20L)
    ::run(|| common::debug("在实体所属区域执行"))
```

```fluxon
task = submit()
    ::on(&location)
    ::scheduler(true)
    ::run(|| common::debug("区域调度"))
```

不支持的目标类型会抛出错误。`on(...)` 和 `scheduler(...)` 是否可用取决于当前运行环境是否加载 Folia 扩展；不要在没有该扩展的平台上假定它们存在。

## 资源和容器

任务对象属于运行时资源，不是持久化数据。需要在多个触发器之间保存手动创建的任务时，可以使用附魔私有 `container`；单一任务用途直接使用整个容器，多个任务用途才嵌套 Map。Ticker、Skill 或 Listener 重载时，不要假定手动创建的任务会自动执行触发器的退出清理。
