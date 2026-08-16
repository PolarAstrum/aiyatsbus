# Fluxon 内置函数与扩展函数参考

本文函数和扩展优先于通过 JVM 互操作自行实现同类工具。字符串、集合、数学或类型转换需求先查本文；已有函数可用时不要改用 Java 静态工具或手写重复算法。

## 1. 全局内置函数

### 系统函数

| 函数 | 签名 | 说明 |
|------|------|------|
| `print(obj)` | `Object -> void` | 输出到 stdout |
| `error(obj)` | `Object -> void` | 输出到 stderr |
| `sleep(ms)` | `int -> void` | 线程休眠（毫秒） |
| `forName(name)` | `String -> Class` | 按全限定名加载 Java 类 |
| `call(fn, args)` | `(Object, List) -> Object` | 动态调用函数/Lambda |
| `this()` | `-> Object` | 获取当前上下文的 target 对象 |
| `throw(obj)` | `Object -> void` | 抛出异常 |
| `g()` | `-> GlobalObject` | 获取全局对象（在上下文中访问同名全局函数） |

### 类型转换函数

| 函数 | 说明 |
|------|------|
| `string(obj)` | 转字符串 |
| `int(obj)` | 转 int（失败返回 0） |
| `intOrNull(obj)` | 转 int（失败返回 null） |
| `long(obj)` / `longOrNull(obj)` | 转 long |
| `float(obj)` / `floatOrNull(obj)` | 转 float |
| `double(obj)` / `doubleOrNull(obj)` | 转 double |
| `array(obj)` | Collection 转数组 |
| `list(obj)` | 数组转不可变 List |
| `mutableList(obj)` | 数组转可变 List |
| `typeOf(obj)` | 获取类型名（SimpleName） |
| `isString(obj)` | 是否为 String |
| `isNumber(obj)` | 是否为 Number |
| `isArray(obj)` | 是否为数组 |
| `isList(obj)` | 是否为 List |
| `isMap(obj)` | 是否为 Map |

### 数学函数

| 分类 | 函数 |
|------|------|
| 基础 | `min(a,b)`, `max(a,b)`, `clamp(val,min,max)`, `abs(n)`, `sign(n)` |
| 取整 | `round(d)`, `floor(d)`, `ceil(d)` |
| 三角 | `sin(d)`, `cos(d)`, `tan(d)`, `asin(d)`, `acos(d)`, `atan(d)`, `atan2(y,x)` |
| 指数/对数 | `exp(d)`, `log(d)`, `log10(d)`, `pow(base,exp)`, `sqrt(d)`, `cbrt(d)` |
| 随机 | `random()` → 0.0~1.0, `random(n)` → 0~n-1, `random(min,max)` → min~max-1 |
| 角度 | `rad(deg)` 角度→弧度, `deg(rad)` 弧度→角度 |
| 其他 | `lerp(a,b,t)` 线性插值, `hypot(x,y)` 斜边长度 |

全局常量：`PI`（3.14159...）、`E`（2.71828...）

---

## 2. 扩展函数速查表

通过 `target :: method()` 或 `&var :: method()` 调用。

### Object（所有对象通用）

`toString()`, `hashCode()`, `class()`, `isInstance(obj)`

### String

| 分类 | 函数 |
|------|------|
| 基础 | `length()`, `trim()`, `ltrim()`, `rtrim()` |
| 查找 | `indexOf(s)`, `indexOf(s,from)`, `lastIndexOf(s)`, `lastIndexOf(s,from)` |
| 替换 | `replace(old,new)`, `replaceAll(regex,replacement)` |
| 匹配 | `contains(s)`, `matches(regex)`, `findAll(regex)` |
| 截取 | `substring(start)`, `substring(start,end)` |
| 填充 | `padLeft(len)`, `padLeft(len,char)`, `padRight(len)`, `padRight(len,char)` |
| 转换 | `uppercase()`, `lowercase()`, `capitalize()`, `reverse()`, `repeat(n)`, `split(delim)` |
| 检查 | `startsWith(prefix)`, `startsWith(prefix,offset)`, `endsWith(suffix)`, `isEmpty()`, `isBlank()` |
| 字符 | `charAt(i)`, `charCodeAt(i)`, `toCharArray()` |

### Collection（通用集合）

`size()`, `isEmpty()`, `contains(obj)`, `toArray()`, `join()`, `join(delim)`, `random()`, `random(n)`, `add(obj)`, `remove(obj)`, `addAll(c)`, `removeAll(c)`, `clear()`

### Iterable（List/Set 等可迭代对象）

| 分类 | 函数 |
|------|------|
| 转换 | `map(fn)`, `flatMap(fn)`, `associateBy(fn)`, `associateWith(fn)` |
| 过滤 | `filter(fn)` |
| 检查 | `any(fn)`, `all(fn)`, `none(fn)` |
| 检索 | `find(fn)`, `first()`, `last()` |
| 聚合 | `countOf(fn)`, `sumOf(fn)`, `minOf(fn)`, `maxOf(fn)`, `minBy(fn)`, `maxBy(fn)` |
| 分组 | `groupBy(fn)`, `partition(fn)`, `chunked(n)` |
| 排序 | `sorted()`, `sortedDescending()`, `sortedBy(fn)`, `sortedDescendingBy(fn)`, `reversed()`, `shuffled()` |
| 截取 | `take(n)`, `drop(n)`, `takeLast(n)`, `dropLast(n)` |
| 集合运算 | `union(list)`, `intersect(list)`, `subtract(list)`, `distinct()`, `distinctBy(fn)` |
| 遍历 | `each(fn)` |

Lambda 参数约定：单参数用 `|| &it`，多参数用 `|a, b| ...`

### List（有序列表专用）

`get(i)`, `set(i,obj)`, `insert(i,obj)`, `removeAt(i)`, `indexOf(obj)`, `lastIndexOf(obj)`, `subList(from,to)`

### Map

`get(k)`, `getOrDefault(k,default)`, `put(k,v)`, `remove(k)`, `containsKey(k)`, `containsValue(v)`, `size()`, `isEmpty()`, `clear()`, `keySet()`, `values()`, `entrySet()`, `putAll(map)`, `putIfAbsent(k,v)`, `replace(k,v)`, `replaceIfMatch(k,old,new)`, `removeIfMatch(k,v)`

### Map.Entry

`key()`, `value()`

### Throwable

`message`, `localizedMessage`, `cause`, `printStackTrace`

### Domain 表达式（所有对象可用）

| 表达式 | 说明 |
|--------|------|
| `target :: with { ... }` | 执行闭包，返回最后一行的值 |
| `target :: also { ... }` | 执行闭包，返回 target 本身 |

---

## 3. 使用示例

```fluxon
// 数据处理管道
data = [
    [name: "Alice", age: 30],
    [name: "Bob", age: 25],
    [name: "Charlie", age: 35]
]
result = &data
    :: filter(|| &it :: get("age") > 25)
    :: map(|| &it :: get("name"))
    :: sorted()
// ["Alice", "Charlie"]

// 集合操作
scores = [85, 92, 78, 95, 88]
&scores :: groupBy(|| {
    when { &it >= 90 -> "A"; &it >= 80 -> "B"; else -> "C" }
})

// 字符串处理
"hello world" :: split(" ") :: map(|| &it :: capitalize()) :: join(" ")
// "Hello World"
```
