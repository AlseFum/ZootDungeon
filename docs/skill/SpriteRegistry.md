# SpriteRegistry

纹理与区域注册中心，负责管理所有游戏纹理资源的加载、切片和命名。

## 核心概念

| 概念 | 说明 |
|------|------|
| `TextureHandle` | 指向一张图片的句柄，不切片。用于只需要原始纹理的场景（如 MobSprite 配合 TextureFilm 使用）。 |
| `AtlasSheet` | 从一张图片切出多个区域（AtlasArea），支持命名和按 label 寻址。 |
| `AtlasArea` | 纹理中的一块区域数据：texture + uv rect + size。可直接通过 `toImage()` 生成 Image。 |

## 静态方法

### handle()

```java
// 注册一个纹理句柄（不切片）
SpriteRegistry.handle("mod:rat", "cola/rat.png");

// 获取已注册的句柄
TextureHandle h = SpriteRegistry.handle("mod:rat");
Object rawHandle = h.textureHandle();
```

### area()

```java
// 注册一个 sheet 并开始切片
SpriteRegistry.area("sheet.cola.arksupply", "cola/arksupply.png")
    .grid(64, 64)          // 设置格子大小
    .area("cell_0")         // 命名第一个格子（推进指针）
    .area("cell_1")         // 命名第二个格子
    .area("cell_2");        // 命名第三个格子
```

等价旧 API：`SpriteRegistry.texture(name, path)`

### getArea()

```java
// 通过 label 获取区域
AtlasArea area = SpriteRegistry.getArea("cell_0");
if (area != null) {
    Image img = area.toImage();
}

// 通过整数 id 获取区域
AtlasArea area = SpriteRegistry.getArea(1024);
```

等价旧 API：`SpriteRegistry.get(label/id)`

### byLabel()

```java
// 获取全局 image id（兼容性方法）
int id = SpriteRegistry.byLabel("cell_0");
image = new ItemSprite(id);
```

**注意**：优先使用 `getArea()` 获取 `AtlasArea`，通过 `toImage()` 生成 Image。

## AtlasSheet 切片 Helpers

### grid()

```java
// 设置格子尺寸（非必须，默认 16x16）
.grid(64, 64)
```

### area() / area(label)

```java
// 为当前指针位置分配一个区域，隐式数字 label（0, 1, 2...）
.area()

// 为当前指针位置分配一个区域，显式命名
.area("my_label")
```

### areas()

```java
// 批量为所有现有格子分配隐式数字 label
.grid(16, 16).areas()
// 结果：自动分配 "0", "1", "2" ... "143"（假设 256x256 图 = 16x16 = 256 格）
```

### row() / group()

```java
// 从 (x,y) 开始，按 w×h 尺寸，依次命名
.row(x, y, w, h).areas("a", "b", "c")
// 等价于: setXY("a", x, y, w, h) → setXY("b", x+w, y, w, h) → setXY("c", x+2w, y, w, h)

// 从 (x,y) 开始，按 rows×cols 排列
.group(x, y, rows, cols, w, h).areas("a", "b", "c", "d")
```

### setXY()

```java
// 像素坐标定义区域
.setXY("foo", 64, 128, 32, 32)

// 格子坐标定义区域（需先调用 grid()）
.grid(32, 32).setXY("bar", 2, 4)
```

### span()

```java
// 预留格子（不分配 label）
.span(10)
```

## 典型使用场景

### 场景 1：注册单格纹理（最常见）

```java
// 注册阶段（在类 static 块或初始化）
SpriteRegistry.area("sheet.cola.my_item", "cola/my_item.png")
    .grid(32, 32)
    .area("my_item_image");

// 使用阶段
image = SpriteRegistry.getArea("my_item_image").toImage();
```

### 场景 2：只注册句柄（用于 TextureFilm）

```java
// 注册阶段
SpriteRegistry.handle("mod:rat", "cola/rat.png");

// MobSprite 中使用
TextureFilm frames = textureWithFallback("mod:rat", Assets.Sprites.RAT, 16, 15);
```

### 场景 3：批量格子 + 部分命名

```java
SpriteRegistry.area("sheet.minecraft.misc", "minecraft/misc.png")
    .grid(16, 16)
    .areas()                          // 先批量分配数字 label
    .area("skel");                    // 再覆盖第一个格子的 label
// 此时 "0" 和 "skel" 都指向同一个格子
```

### 场景 4：多帧动画 sheet

```java
SpriteRegistry.area("sheet.effects.explosion", "effects/explosion.png")
    .grid(32, 32)
    .areas();
// 结果：自动分配 "0"..."15"，共 16 帧
```

## 内部机制

- 全局 `LABEL_ID_MAP`：label → 全局 image id（用于 `ItemSprite` 寻址）
- 全局 `LABEL_SHEETS_MAP`：label → 所属 sheet（用于按 label 找 sheet）
- 全局 `LABEL_HANDLES_MAP`：label → TextureHandle（用于句柄查找）
- 全局 `itemSheets`：所有 AtlasSheet 列表（用于按 id 找 sheet）
- `AtlasSheet.afterLoad()`：reload 时重建 atlas 切片和 label 映射

## 兼容性

以下为旧 API 兼容别名（已标记 `@Deprecated`，建议迁移）：

| 旧 API | 新 API |
|--------|--------|
| `SpriteRegistry.TextureSheet` | `SpriteRegistry.AtlasSheet` |
| `SpriteRegistry.ImageMapping` | `SpriteRegistry.AtlasArea` |
| `texture(name, path)` | `area(name, path)` |
| `.label()` / `.label("x")` | `.areas()` / `.area("x")` |
| `.row().label(...)` | `.row().areas(...)` |
| `.get(id/label)` | `.getArea(id/label)` |
| `SpriteRegistry.the(name)` | `SpriteRegistry.area(name)` |

## 注意事项

- `grid()` 只切格子，**不分配任何 label**，需要配合 `areas()` 或 `area()` 使用
- `area()` 同时推进指针，指向下一个未分配的格子
- `areas()` 批量分配隐式数字 label，覆盖已存在的 label
- 同一个 label 可以通过 `LABEL_ID_MAP` 找到全局 image id，用于 `ItemSprite` 或 `SpriteRegistry.byLabel()`
