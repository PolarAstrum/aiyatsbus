# Fluxon 语言参考

## 现有 API 优先

语言语法能表达某种实现，不代表应该自行实现。Aiyatsbus 脚本应优先使用 `aiyatsbus-fluxon-functions.md`、`fluxon-platform-functions.md`、`fluxon-stdlib.md` 和 `fluxon-modules.md` 已提供的函数，再考虑 `fluxon-jvm-interop.md`。已有函数能完成需求时不要用 Java 静态方法、反射或手写循环重复造轮子。

## 黄金规则

**裸标识符在表达式位置是字符串字面量，不是变量引用。** 读取变量必须用 `&name`。

```fluxon
x = 10
print(x)    // ❌ 输出字符串 "x"
print(&x)   // ✅ 输出 10
y = x + 5   // ❌ "x" + 5 → 类型错误
y = &x + 5  // ✅ 15
```

---

## 1. 基础语法

- 文件扩展名：`.fs`，但是在本插件中一般不创建专门的脚本文件，而是嵌入 YAML 文件中
- 语句分隔：换行或 `;`
- 注释：`// 行注释`、`/* 块注释 */`
- 代码块：`{ <stmt>* <expr>? }`，值为最后一个表达式的结果

### 字面量

| 类型 | 示例 |
|------|------|
| 整数 | `42` |
| 长整数 | `123L` |
| 浮点 | `0.5f` |
| 双精度 | `2.5e3` |
| 字符串 | `"hello"` 或 `'world'` |
| 布尔 | `true`、`false` |
| 空值 | `null` |
| 列表 | `[1, 2, 3]` |
| 映射 | `[host: "localhost", "port": 8080]` |

### 字符串插值

```fluxon
name = "World"
"Hello ${&name}!"              // "Hello World!"
"Sum: ${&a + &b}"              // "Sum: 30"
"Len: ${'hello'::length()}"   // "Len: 5"
```

- 单/双引号均支持插值
- `null` 值转为字符串 `"null"`
- 转义：`\${` 输出字面量 `${`，不触发插值

### 标识符

- 支持中文字符
- 支持 `-` 作为非首字符（如 `log-level`）
- 示例：`color = red`、`服务器状态 = 正常`、`log-level = info`

---

## 2. 变量与引用

### 引用运算符

| 形式 | 行为 |
|------|------|
| `&name` | 严格引用，变量未定义时报错 |
| `&?name` | 可选引用，未定义或 `null` 时返回 `null` |

### 赋值

```fluxon
x = 10              // 基本赋值
x += 5              // 复合赋值：+= -= *= /= %=
```

### 常量

全大写标识符（模式 `[A-Z][A-Z0-9_]*`）赋值字面量时自动成为编译时内联常量：

```fluxon
PI = 3.14159        // 常量，编译时内联
MAX_SIZE = 1024     // 常量
PI = 6.28           // ❌ Cannot reassign constant: PI
```

### 解构赋值

```fluxon
(a, b) = [10, 20]
for (key, value) in &map { print(&key + ":" + &value) }
```

右侧可为 list/array/map/mapEntry。

---

## 3. 运算符优先级（高→低）

1. 后缀：`f()`、`x[i]`
2. 成员访问：`obj.member`、`obj?.member`（null 短路）
3. 上下文调用：`target :: func()`、`target ?:: func()`（null 短路）
4. 一元：`!`、`-`、`await`、`&name`、`&?name`
5. 幂：`**`
6. 乘除模：`*`、`/`、`%`
7. 加减：`+`、`-`
8. 区间：`a..b`（闭区间）、`a..<b`（左闭右开）
9. 比较/类型检查：`> >= < <= == !=`、`is`
10. 逻辑：`&&`、`||`（短路求值）
11. 三元：`<cond> ? <then> : <else>`
12. Elvis：`<expr> ?: <fallback>`（左侧为 null 时取右侧）
13. 赋值：`=`、`+=`、`-=`、`*=`、`/=`、`%=`

### 类型检查 `is`

```fluxon
"hello" is string   // true
123 is int           // true
null is string       // false（null 始终返回 false）
obj is java.util.ArrayList  // 支持完全限定类名
```

类型别名（不区分大小写）：`string`、`int`、`long`、`float`、`double`、`boolean`、`list`、`map`、`set`

---

## 4. 函数与 Lambda

### 函数定义（仅顶层，不可嵌套）

```fluxon
// 表达式体
def add(a, b) = &a + &b

// 块体（= 可省略）
def abs(n) {
    if &n >= 0 { return &n }
    return -&n
}

// 无括号参数
def add a, b = &a + &b

// 带类型注解
def process(center: com.example.Location, radius: int) = { ... }

// 异步/同步修饰
async def fetchData() = { ... }   // 线程池异步执行，返回 CompletableFuture
sync def updateUI() = { ... }     // 主线程执行（需宿主运行时支持）
```

### Lambda

```fluxon
// 带参数
inc = |x| &x + 1
sum = |a, b| &a + &b

// 块体
process = |x| {
    y = &x * 2
    &y + 1
}

// 隐式参数 it（|| 语法自动绑定第一个实参到 it）
doubled = [1, 2, 3] :: map(|| &it * 2)              // [2, 4, 6]
lengths = ["hi", "world"] :: map(|| &it :: length()) // [2, 5]

// 动态调用 Lambda
fn = |x| &x + 1
call(&fn, [5])  // 6
```

### 递归

```fluxon
def fib(n) = if &n <= 1 then 1 else fib(&n - 1) + fib(&n - 2)
```

---

## 5. 控制流（均为表达式，有返回值）

### if

```fluxon
grade = if &score >= 90 then "A" else "B"   // then 关键字可省略
result = if &x > 0 { "positive" } else { "non-positive" }
if &debug { print("debug mode") }           // 无 else 时返回 null
```

### when

```fluxon
// 条件模式
label = when {
    &n % 2 == 0 -> "even"
    &n > 100    -> "big"
    else        -> "odd"
}

// 值匹配 + 范围
bucket = when &y {
    in 0..10   -> "small"
    in 11..100 -> "medium"
    else       -> "large"
}

// 类型匹配
typeLabel = when &obj {
    is int    -> "integer"
    is string -> "text"
    is list   -> "list"
    else      -> "unknown"
}
```

条件/值/类型/范围匹配可混合使用。

### 循环

```fluxon
for i in 1..5 { print(&i) }           // 闭区间 [1,5]
for i in 0..<10 { print(&i) }         // 左闭右开 [0,10)
for (k, v) in &map { print(&k) }      // 解构迭代
while &j < 10 { j += 1 }
// break / continue 仅循环体内有效
```

### 三元 / Elvis

```fluxon
result = &x > 0 ? "pos" : "neg"
name = &?username ?: "anonymous"       // null 时取右侧
```

### 异常处理（也是表达式）

```fluxon
result = try {
    throw("boom")
} catch (e) {
    "caught: " + &e.message
} finally {
    print("cleanup")
}
```

- `throw(<expr>)` 抛出异常
- `catch` 可带变量 `(e)` 或省略
- `finally` 可选

---

## 6. 上下文调用 `::`

`::` 是 Fluxon 的核心特性，在目标对象上调用扩展函数。

```fluxon
// 单次调用
target :: method(args)

// 块形式（共享同一 target）
target :: {
    method1()
    method2()
}

// 安全形式（target 为 null 时返回 null，不执行右侧）
target ?:: method(args)
target ?:: { ... }
```

### 语法糖

左侧是标识符且紧跟 `::` 时，允许省略 `()`：

```fluxon
time :: formatTimestamp(0L)   // 等价于 time() :: formatTimestamp(0L)
```

### 关键区分

```fluxon
&list :: size()    // ✅ 对变量 list 调用扩展函数
list :: size()     // ❌ list 被当作函数调用 list()，再对返回值调用 size()
```

变量必须加 `&`，裸标识符会被当作函数名。
