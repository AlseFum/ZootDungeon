# EventBus 使用范例需求文档

## 1. 设计目标

### 1.1 核心目标
- **事件驱动架构**：支持基于事件类型的强类型事件系统
- **继承设计**：通过继承Event基类快速创建新的事件类型
- **流畅API**：提供基于`of`和`with`的链式调用接口
- **优先级处理**：事件处理按优先级排序执行
- **返回值收集**：支持收集所有处理器的返回值到指定的结果类型
- **多阶段处理**：支持事件处理的多阶段流程

### 1.2 设计原则
- 所有事件类型继承自Event基类
- 通过继承和重写快速定义新事件类型
- 基类提供通用的`with`, `dispatch`, `collect`方法
- 子类只需定义成员字段和`of`方法
- 每个事件类型必须定义Result内部类用于收集回调结果

## 2. 核心概念

### 2.1 Event基类

Event基类提供通用的事件处理功能：

```java
public abstract class Event<E extends Event<E, R>, R> {
    // 内部数据存储（Map）
    protected final Map<String, Object> data = new HashMap<>();
    
    // 基类提供：链式添加数据
    @SuppressWarnings("unchecked")
    public E with(String key, Object value) {
        data.put(key, value);
        return (E) this;
    }
    
    // 基类提供：分发事件
    public void dispatch() {
        // 实现分发逻辑
    }
    
    // 基类提供：收集返回值
    public ArrayList<R> collect() {
        // 实现收集逻辑
    }
}
```

### 2.2 事件类型定义（继承方式）

新的事件类型通过继承Event基类定义：

```java
public class AttackEvent extends Event<AttackEvent, AttackEvent.Result> {
    // 定义事件成员字段（从data Map中获取）
    public Char getAttacker() {
        return (Char) data.get("attacker");
    }
    
    public Char getDefender() {
        return (Char) data.get("defender");
    }
    
    // 必须实现：静态工厂方法
    public static AttackEvent of(Char attacker, Char defender, Weapon weapon) {
        return new AttackEvent()
            .with("attacker", attacker)
            .with("defender", defender)
            .with("weapon", weapon);
    }
    
    // 必须定义：结果类型
    public static class Result {
        public final int damage;
        public final boolean critical;
        
        // 必须实现：Result的of方法（用于构造返回值）
        public static Result of(int damage, boolean critical) {
            return new Result(damage, critical);
        }
        
        private Result(int damage, boolean critical) {
            this.damage = damage;
            this.critical = critical;
        }
    }
}
```

### 2.3 结果类型（Result）

每个事件类型必须定义一个Result内部类：

```java
public static class Result {
    // 结果数据字段
    public final Type1 field1;
    public final Type2 field2;
    
    // 必须实现：Result的of方法（用于构造返回值）
    public static Result of(Type1 field1, Type2 field2) {
        return new Result(field1, field2);
    }
    
    private Result(Type1 field1, Type2 field2) {
        this.field1 = field1;
        this.field2 = field2;
    }
}
```

## 3. API 规范

### 3.1 Event基类API

#### 3.1.1 基类提供的方法

```java
// 链式添加数据
public EventType with(String key, Object value) {
    data.put(key, value);
    return this;
}

// 分发事件
public void dispatch() { }

// 收集返回值
public ArrayList<Result> collect() { }
```

#### 3.1.2 子类必须实现的方法

```java
// 1. 静态工厂方法 of
public static EventType of(/* 参数列表 */) {
    return new EventType()
        .with("key1", value1)
        .with("key2", value2);
}

// 2. Result类的of方法
public static class Result {
    public static Result of(/* 参数列表 */) {
        return new Result(/* 参数 */);
    }
}
```

### 3.2 事件类型的使用

#### 3.2.1 注册处理器

```java
// 使用EventBus注册（字符串topic方式）
EventBus.on("attack-event", (AttackEvent e) -> {
    // 处理逻辑
    int damage = calculateDamage(e.getAttacker(), e.getDefender());
    return AttackEvent.Result.of(damage, false);
}, priority);

// 或者使用TypedEventBus注册（强类型方式）
EventBus.typed(AttackEvent.class).addResult((AttackEvent e) -> {
    return AttackEvent.Result.of(damage, false);
}, priority);
```

#### 3.2.2 创建和触发事件

```java
// 方式1: 使用of创建，然后with添加数据（如果of中没有包含所有数据）
AttackEvent event = AttackEvent.of(attacker, defender, weapon)
    .with("bonus", 10);

// 方式2: 直接使用of创建（如果of已经包含了所有数据）
AttackEvent event = AttackEvent.of(attacker, defender, weapon);

// 分发事件
event.dispatch();

// 收集返回值
ArrayList<AttackEvent.Result> results = event.collect();

// 事件实例可以多次使用
event.dispatch();
var results2 = event.collect();
```

### 3.3 Result类型的使用

#### 3.3.1 在处理器中返回结果

```java
EventBus.on("attack-event", (AttackEvent e) -> {
    // 使用Result.of()构造返回值
    return AttackEvent.Result.of(damage, critical);
}, priority);

// 或者返回null表示跳过
EventBus.on("attack-event", (AttackEvent e) -> {
    if (shouldSkip(e)) {
        return null; // 跳过此处理器
    }
    return AttackEvent.Result.of(damage, critical);
}, priority);
```

## 4. 事件处理流程

### 4.1 基本流程

1. **创建事件**：使用 `EventType.of(...)` 创建事件实例，可用 `with()` 添加数据
2. **分发事件**：调用 `event.dispatch()` 触发所有注册的处理器
3. **处理执行**：按优先级顺序执行所有处理器
4. **收集结果**：收集所有非null的返回值到 `ArrayList<Result>`
5. **后续处理**：基于收集的结果进行后续操作

**注意**：事件实例可以多次使用，可以多次调用`dispatch()`或`collect()`方法。

### 4.2 多阶段处理

```
阶段1: EventType1.of(...).collect()
    ↓ (处理结果)
阶段2: EventType2.of(stage1Results).collect()
    ↓ (处理结果)
阶段3: EventType3.of(stage2Results).collect()
    ↓
最终结算
```

## 5. 优先级机制

### 5.1 优先级规则
- **数值越大，优先级越高**
- 相同优先级按注册顺序执行
- 支持任意整数范围

### 5.2 优先级使用

```java
// 高优先级（先执行）
EventBus.on("event-topic", (EventType e) -> { ... }, 100);

// 中优先级
EventBus.on("event-topic", (EventType e) -> { ... }, 50);

// 低优先级（后执行）
EventBus.on("event-topic", (EventType e) -> { ... }, 10);
```

## 6. Topic机制

### 6.1 Topic概念
- **默认topic**：每个事件类型有一个默认topic（通常是类名）
- **自定义topic**：可以指定自定义topic名称
- Topic用于区分不同的处理场景

### 6.2 Topic使用

```java
// 在默认topic上注册和分发
EventBus.on(AttackEvent.class.getName(), (AttackEvent e) -> { ... });
AttackEvent.of(...).dispatch();

// 在自定义topic上注册和分发
EventBus.on("custom-topic", (AttackEvent e) -> { ... });
// 需要在dispatch中指定topic（如果支持）
```

## 7. 返回值处理

### 7.1 返回值规则
- 处理器返回 `Result` 实例表示有值结果
- 处理器返回 `null` 表示空结果，跳过此处理器
- 只有非null的返回值会被收集到结果列表

### 7.2 返回值收集

```java
// 收集所有非null返回值
ArrayList<Result> results = event.collect();

// 处理结果列表
for (Result result : results) {
    // 处理每个结果
}
```

## 8. 创建新事件类型的步骤

### 8.1 实现步骤检查清单

- [ ] 1. 创建类并继承 `Event<YourEvent, YourEvent.Result>`
- [ ] 2. 实现静态工厂方法 `of(...)`（使用`with()`添加数据）
- [ ] 3. 定义 `Result` 内部类
- [ ] 4. `Result` 类实现静态工厂方法 `of(...)`
- [ ] 5. （推荐）添加访问方法（getter）从data Map获取字段

### 8.2 完整模板示例

```java
package com.zootdungeon.event;

import com.zootdungeon.utils.Event;
import java.util.ArrayList;

/**
 * 示例事件类型
 */
public class YourEvent extends Event<YourEvent, YourEvent.Result> {
    
    // ========== 访问方法（推荐） ==========
    
    public Type1 getField1() {
        @SuppressWarnings("unchecked")
        Type1 value = (Type1) data.get("field1");
        return value;
    }
    
    public Type2 getField2() {
        @SuppressWarnings("unchecked")
        Type2 value = (Type2) data.get("field2");
        return value;
    }
    
    // ========== 必须实现：静态工厂方法 ==========
    
    /**
     * 创建事件实例
     */
    public static YourEvent of(Type1 field1, Type2 field2) {
        return new YourEvent()
            .with("field1", field1)
            .with("field2", field2);
    }
    
    // ========== 必须定义：结果类型 ==========
    
    public static class Result {
        public final ReturnType1 resultField1;
        public final ReturnType2 resultField2;
        
        /**
         * 创建结果实例（必须实现）
         */
        public static Result of(ReturnType1 resultField1, ReturnType2 resultField2) {
            return new Result(resultField1, resultField2);
        }
        
        private Result(ReturnType1 resultField1, ReturnType2 resultField2) {
            this.resultField1 = resultField1;
            this.resultField2 = resultField2;
        }
    }
}
```

### 8.3 使用示例

```java
// 注册处理器
EventBus.on(YourEvent.class.getName(), (YourEvent e) -> {
    // 通过访问方法获取数据
    Type1 field1 = e.getField1();
    Type2 field2 = e.getField2();
    
    // 处理逻辑
    ReturnType1 result1 = process(field1, field2);
    
    // 使用Result.of()返回结果
    return YourEvent.Result.of(result1, result2);
}, 100);

// 创建事件
var event = YourEvent.of(value1, value2);

// 可以继续使用with添加数据
event.with("additional", value3);

// 分发事件
event.dispatch();

// 收集返回值
var results = event.collect();

// 事件实例可以多次使用
event.dispatch();
var results2 = event.collect();
```

## 9. 实现要求

### 9.1 Event基类必须提供
- ✅ `with(String key, Object value)` - 链式添加数据
- ✅ `dispatch()` - 分发事件
- ✅ `collect()` - 收集返回值
- ✅ 内部数据存储 `Map<String, Object> data`

### 9.2 事件类型（子类）必须实现
- ✅ 继承 `Event<EventType, EventType.Result>`
- ✅ 实现静态工厂方法 `of(...)`
- ✅ 定义 `Result` 内部类
- ✅ `Result` 类实现静态工厂方法 `of(...)`
- ✅ （推荐）提供访问方法（getter）从data Map中获取字段

### 9.3 可选实现
- （可选）`dispatch(String topic)` - 在指定topic上分发
- （可选）`collect(String topic)` - 在指定topic上收集
- （可选）自定义访问方法 - 提供类型安全的字段访问

### 9.4 性能要求
- 优先级排序应在注册时完成
- 返回值收集应高效
- 尽量减少内存分配

## 10. 注意事项

1. **类型安全**：访问data Map时需要进行类型转换，使用`@SuppressWarnings("unchecked")`抑制警告
2. **字段命名**：`with()`方法中的key应该与访问方法中的key一致
3. **of方法完整性**：`of()`方法应该包含所有必需的数据
4. **Result的of方法**：必须实现，用于在处理器中构造返回值
5. **私有构造函数**：Result类应该使用私有构造函数，强制使用`of()`方法
6. **事件实例可重用**：`of()`方法返回的事件实例可以多次调用`dispatch()`或`collect()`

## 11. 待讨论问题

1. **Topic命名规范**：Topic的命名规范和默认值规则？
2. **错误处理**：处理器抛出异常时的处理策略？
3. **异步支持**：是否需要支持异步事件处理？
4. **事件取消**：是否需要支持事件处理的中途取消机制？
5. **EventBus集成**：Event基类如何与EventBus系统集成？
6. **类型安全**：如何在使用data Map时保证类型安全？
7. **性能优化**：data Map的实现是否需要优化？
