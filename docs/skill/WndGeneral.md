# WndGeneral

通用表单窗口，支持单窗和多标签页模式。

## Table Of Content

- [快速使用](#快速使用)
- [Builder API](#builder-api)
- [PaneBuilder（标签页内容）](#panebuilder标签页内容)
- [HRowBuilder（横向布局）](#hrowbuilder横向布局)
- [部件说明](#部件说明)
- [注意事项](#注意事项)

---

## 快速使用

### 简单文本窗口

```java
WndGeneral.show("调试", "深度: " + Dungeon.depth, "COST: " + Dungeon.cost);
```

### 构建器模式

```java
WndGeneral.make()
    .title("状态")
    .row("深度", Dungeon.depth)
    .row("金币", Dungeon.gold)
    .button("确定", () -> {})
    .show();
```

### 多标签页

```java
WndGeneral.make()
    .title("多页")
    .tab("甲", p -> p.line("第一页"))
    .tab("乙", p -> p.option("动作", () -> {}))
    .show();
```

---

## Builder API

`WndGeneral.make()` 返回 `Builder`，支持以下方法：

| 方法 | 说明 |
|------|------|
| `.title(String)` | 设置窗口标题 |
| `.row(String, Object)` | 添加一行，显示为 `"label: value"` |
| `.line(String)` | 添加纯文本行 |
| `.option(String, Runnable)` | 添加选项按钮，点击后自动关闭窗口 |
| `.button(String, Runnable)` | 添加底部按钮，点击后**不自动关闭** |
| `.switchRow(String, boolean, Consumer<Boolean>)` | 添加开关行 |
| `.hrow(Consumer<HRowBuilder>)` | 添加横向布局行 |
| `.inputRow(String, String, int, Consumer<String>)` | 添加文本输入按钮，点击弹出 `WndTextInput` |
| `.tab(String, Consumer<PaneBuilder>)` | 添加标签页 |
| `.show()` | 显示窗口 |

---

## PaneBuilder（标签页内容）

用于 `.tab()` 中定义标签页内容，与 `Builder` 共享相同方法：

| 方法 | 说明 |
|------|------|
| `.row(String, Object)` | 标签值行 |
| `.line(String)` | 纯文本行 |
| `.option(String, Runnable)` | 选项按钮 |
| `.button(String, Runnable)` | 普通按钮 |
| `.switchRow(String, boolean, Consumer<Boolean>)` | 开关 |
| `.hrow(Consumer<HRowBuilder>)` | 横向布局 |
| `.inputRow(String, String, int, Consumer<String>)` | 文本输入 |

---

## HRowBuilder（横向布局）

用于 `.hrow()` 中定义一行内的多列：

```java
p.hrow(r -> r
    .line("名称")
    .line("值")
);
p.hrow(r -> r
    .button("左", () -> {})
    .button("中", () -> {})
    .button("右", () -> {})
);
```

| 方法 | 说明 |
|------|------|
| `.line(String)` | 添加文本单元格 |
| `.button(String, Runnable)` | 添加按钮单元格 |

最后一个单元格自动填满剩余宽度。

---

## 部件说明

### line()

纯文本行，用于分隔或说明。

```java
p.line("===== 开关 =====");
p.line("普通文本行内容");
```

### row()

标签-值行，显示为 `"label: value"`。

```java
p.row("当前等级", hero.lvl);
p.row("金币", Dungeon.gold);
```

### option()

点击后**自动关闭窗口**的选项按钮。

```java
p.option("确认", () -> doSomething());
```

### button()

点击后**不自动关闭**的按钮，用于底部操作按钮。

```java
p.button("确定", () -> {
    save();
});
```

### switchRow()

复选框样式的开关，不自动关闭窗口。

```java
p.switchRow("调试模式", false, on -> {
    GLog.p("调试模式: " + on);
});
```

### hrow()

横向布局，支持文本和按钮混合：

```java
p.hrow(r -> r
    .line("标签")
    .line("值")
);
p.hrow(r -> r
    .button("左", () -> {})
    .button("中", () -> {})
    .button("右", () -> {})
);
```

### inputRow()

点击后弹出 `WndTextInput` 文本输入框：

```java
p.inputRow("输入名字", "默认", 20, text -> {
    GLog.p("输入: " + text);
});
```

参数：`label, 初始值, 最大字符数, 回调`

---

## 注意事项

1. **开关 vs 选项**：开关不自动关闭，选项点击后关闭。
2. **标签页切换**：每次切换标签会重新构建内容。
3. **长内容滚动**：内容超出高度时自动启用滚动。
4. **横向布局**：最后一个单元格自动填满剩余宽度。
5. **滚动列表点击**：`InteractiveScrollPane` 使用 `NEVER_BLOCK` 让事件穿透，按钮仍可点击。
