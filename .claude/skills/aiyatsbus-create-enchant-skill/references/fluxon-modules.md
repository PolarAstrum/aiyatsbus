# Fluxon Import 模块参考

通用 Fluxon 脚本可以使用 `import 'fs:xxx'`（字符串或标识符均可，分号可选）。Aiyatsbus 附魔 YAML 的脚本块已经自动导入以下五个模块，通常不需要手动 `import`：`fs:time`、`fs:crypto`、`fs:reflect`、`fs:io`、`fs:jvm`。本文中的 `import` 主要用于说明模块归属和通用 Fluxon 用法。

模块 API 优先于 JVM 自实现。需要时间、文件、加密编码或反射能力时先查本文；已有模块函数能完成需求时不要调用等价 Java 静态方法。例如获取当前毫秒时间戳必须使用：

```fluxon
timestamp = now()
```

不要写成 `static java.lang.System.currentTimeMillis()`。

---

## fs:time -- 时间模块

```fluxon
import 'fs:time'
```

### 全局函数

| 函数 | 说明 |
|------|------|
| `now()` | 当前时间戳（毫秒） |
| `time()` | 获取时间工具对象 |

### TimeObject 方法（通过 `time :: xxx()` 调用）

| 分类 | 方法 |
|------|------|
| 基础 | `now()`, `nowSeconds()`, `nano()` |
| 格式化 | `formatDateTime(pattern?)`, `formatTimestamp(ts, pattern?)`, `parseDateTime(str, pattern)` |
| 时间组件 | `year()`, `month()`, `day()`, `hour()`, `minute()`, `second()`, `weekday()` |
| 从时间戳提取 | `yearFromTimestamp(ts)`, `monthFromTimestamp(ts)`, `dayFromTimestamp(ts)`, `hourFromTimestamp(ts)`, `minuteFromTimestamp(ts)`, `secondFromTimestamp(ts)` |
| 时间计算 | `addDays(ts, n)`, `addHours(ts, n)`, `addMinutes(ts, n)`, `addSeconds(ts, n)` |
| 时间差 | `daysBetween(ts1, ts2)`, `hoursBetween(ts1, ts2)`, `minutesBetween(ts1, ts2)`, `secondsBetween(ts1, ts2)` |
| 比较 | `isToday(ts)`, `isYesterday(ts)`, `isTomorrow(ts)`, `isBetween(ts, start, end)` |
| 边界 | `startOfDay(ts)`, `endOfDay(ts)`, `startOfMonth(ts)`, `endOfMonth(ts)`, `startOfYear(ts)`, `endOfYear(ts)` |

### 示例

```fluxon
// 格式化当前时间
now = time :: formatDateTime("yyyy-MM-dd HH:mm:ss")
print("当前时间: " + &now)

// 时间计算
ts = time :: parseDateTime("2024-01-01", "yyyy-MM-dd")
days = time :: daysBetween(&ts, time :: now())
print("已过 " + &days + " 天")

// 块形式
time :: {
    formatted = formatTimestamp(0L, "yyyy-MM-dd")
    print(&formatted)
}
```

---

## fs:io -- 文件/路径模块

```fluxon
import 'fs:io'
```

### 全局函数

| 函数 | 说明 |
|------|------|
| `path(str)` | 创建 Path |
| `path(str, sub)` | 创建 Path（带子路径） |
| `file(str)` | 创建 File |
| `file(parent, child)` | 创建 File（父+子） |

### File 扩展函数

| 分类 | 方法 |
|------|------|
| 属性 | `name()`, `path()`, `absolutePath()`, `canonicalPath()`, `parent()`, `parentFile()`, `extension()`, `nameWithoutExtension()` |
| 状态 | `exists()`, `isFile()`, `isDirectory()`, `length()`, `lastModified()` |
| 读取 | `readText()`, `readLines()`, `readBytes()` |
| 写入 | `writeText(str)`, `writeLines(list)`, `writeBytes(bytes)`, `appendText(str)` |
| 操作 | `createNewFile()`, `mkdir()`, `mkdirs()`, `delete()`, `deleteOnExit()`, `renameTo(target)` |
| 复制/移动 | `copyTo(target)`, `copyTo(target, replace)`, `moveTo(target)`, `moveTo(target, replace)` |
| 递归 | `deleteRecursively()`, `copyRecursively(target)`, `copyRecursively(target, replace)` |
| 遍历 | `list()`, `listFiles()`, `walk()`, `walk(depth)` |
| 转换 | `toPath()` |

### Path 扩展函数

| 分类 | 方法 |
|------|------|
| 操作 | `resolve(str)`, `relativize(path)`, `normalize()`, `toAbsolutePath()`, `toRealPath()` |
| 属性 | `name()`, `parent()`, `root()` |
| 状态 | `exists()`, `notExists()`, `isDirectory()`, `isRegularFile()`, `isSymbolicLink()` |
| 遍历 | `walk()`, `walk(depth)` |
| 转换 | `toFile()` |

### 示例

```fluxon
import 'fs:io'

f = file("config.txt")
if &f :: exists() {
    content = &f :: readText()
    lines = &content :: split("\n")
    &lines :: each(|| print(&it))
}

file("output.txt") :: writeText("Hello Fluxon!")
```

---

## fs:crypto -- 加密/编码模块

```fluxon
import 'fs:crypto'
```

### 工具对象

| 对象 | 获取方式 | 方法 |
|------|----------|------|
| HashObject | `hash()` 或 `hash` | `md5(str)`, `sha1(str)`, `sha256(str)`, `sha384(str)`, `sha512(str)` |
| Base64Object | `base64()` 或 `base64` | `encode(str, charset?)`, `decode(str, charset?)` |
| UnicodeObject | `unicode()` 或 `unicode` | `encode(str)`, `decode(str)` |
| HexObject | `hex()` 或 `hex` | `encode(str)`, `decode(str)` |

### 示例

```fluxon
import 'fs:crypto'

hashed = hash :: sha256("password123")
print("SHA256: " + &hashed)

encoded = base64 :: encode("Hello World")
decoded = base64 :: decode(&encoded)
print(&decoded)  // "Hello World"

hexStr = hex :: encode("hello")
print(&hexStr)   // "68656c6c6f"
```

---

## fs:jvm -- JVM 字节码注入模块

需要 Java Agent 支持（`-javaagent` 启动参数）。

```fluxon
import 'fs:jvm'
```

### API

| 函数 | 签名 | 说明 |
|------|------|------|
| `inject(target, type, handler)` | `(String, String, Lambda) -> String` | 注入方法拦截器，返回注入 ID |
| `restore(idOrTarget)` | `String -> Boolean` | 撤销注入 |
| `injections()` | `-> List<Map>` | 列出所有活跃注入 |

target 格式：
- 简单：`"com.example.Foo::bar"`（匹配所有重载）
- 精确：`"com.example.Foo::bar(Ljava/lang/String;)V"`（JVM 描述符）

type：`"before"`（前置拦截）或 `"replace"`（完全替换）

handler 参数：`|self, arg1, arg2, ...| { ... }`
- 实例方法：`self` 为 `this`，其余为方法参数
- 静态方法：所有参数为方法参数

### 示例

```fluxon
import 'fs:jvm'

// 前置拦截
id = jvm :: inject("com.example.Service::login", "before", |self, username, password| {
    print("用户尝试登录: " + &username)
})

// 方法替换
jvm :: inject("com.example.Config::getFlag", "replace", |self, name| {
    return true  // 强制所有开关返回 true
})

// 撤销
jvm :: restore(&id)
jvm :: restore("com.example.Service::login")  // 按目标撤销全部

// 列出注入
for item in jvm :: injections() {
    print(&item.id + " -> " + &item.target)
}
```

---

## fs:reflect -- 反射模块

为 Class/Method/Field/Constructor 提供扩展函数。

```fluxon
import 'fs:reflect'
```

### Class 扩展

| 分类 | 方法 |
|------|------|
| 属性 | `name()`, `simpleName()`, `canonicalName()`, `typeName()`, `packageName()` |
| 检查 | `isInterface()`, `isArray()`, `isPrimitive()`, `isAnnotation()`, `isEnum()` |
| 修饰符 | `modifiers()`, `isPublic()`, `isPrivate()`, `isProtected()`, `isAbstract()`, `isFinal()`, `isStatic()` |
| 层级 | `superclass()`, `interfaces()`, `isAssignableFrom(cls)`, `isInstance(obj)` |
| 成员查找 | `methods()`, `declaredMethods()`, `fields()`, `declaredFields()`, `constructors()`, `declaredConstructors()` |
| 精确查找 | `method(name)`, `declaredMethod(name)`, `field(name)`, `declaredField(name)`, `constructor(paramTypes)`, `declaredConstructor(paramTypes)` |
| 实例化 | `newInstance()`, `cast(obj)` |

### Method 扩展

`invoke(instance, args...)`, `name()`, `parameterTypes()`, `returnType()`, `parameterCount()`, `modifiers()`, `setAccessible(bool)`, `isAccessible()`, `declaringClass()`, `exceptionTypes()`, `isPublic()`, `isPrivate()`, `isStatic()`, `isFinal()`, `isAbstract()`, `isBridge()`, `isSynthetic()`, `isVarArgs()`

### Field 扩展

`get(instance)`, `set(instance, value)`, `name()`, `type()`, `modifiers()`, `setAccessible(bool)`, `isAccessible()`, `declaringClass()`, `isPublic()`, `isPrivate()`, `isStatic()`, `isFinal()`, `isVolatile()`, `isTransient()`, `isSynthetic()`, `isEnumConstant()`

### Constructor 扩展

`newInstance(args...)`, `parameterTypes()`, `parameterCount()`, `modifiers()`, `setAccessible(bool)`, `isAccessible()`, `declaringClass()`, `exceptionTypes()`, `isPublic()`, `isPrivate()`, `isSynthetic()`, `isVarArgs()`, `name()`

### 示例

```fluxon
import 'fs:reflect'

cls = forName("java.lang.String")
&cls :: methods() :: each(|| print(&it :: name()))

m = &cls :: method("substring")
result = &m :: invoke("hello world", 6)
print(&result)  // "world"
```
