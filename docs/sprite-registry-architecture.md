# SpriteRegistry 架构指南

## 概述

`SpriteRegistry` 是 Cola Dungeon 的**统一纹理与精灵管理系统**。它：

1. 管理项目中所有精灵表和纹理图集
2. 提供基于 Overlay 的材质替换（纹理包支持）
3. 使用抽象的 `Segment` 类层次结构处理不同类型的纹理
4. 确保所有纹理通过单一、集中的解析点

---

## 纹理使用地图

### 1. **环境/地图贴图纹理**
用于地牢关卡渲染、水体效果和地形特征。

| 类型 | 文件 | 使用位置 |
|------|-------|---------|
| **基础瓷砖** | `tiles_sewers.png`、`tiles_prison.png`、`tiles_caves.png`、`tiles_city.png`、`tiles_halls.png` | `DungeonTilemap`、`Level` 类 |
| **水体** | `water0.png` - `water4.png`（5 个不同区域的变体） | 地牢中的水体渲染 |
| **地形特征** | `terrain_features.png` | 树木、岩石、障碍物 |
| **自定义瓷砖** | `weak_floor.png`、`sewer_boss.png`、`prison_quest.png` 等 | Boss 关卡、任务区域 |
| **网格与调试** | `visual_grid.png`、`wall_blocking.png` | 调试可视化 |

**材质 ID**（用于 Overlay 映射）：
- `"environment.tiles.sewers"` → `tiles_sewers.png`
- `"environment.tiles.prison"` → `tiles_prison.png`
- `"environment.water.sewers"` → `water0.png`

---

### 2. **精灵纹理（角色与怪物）**
用于渲染英雄角色、敌人、NPC 和交互对象。

#### 英雄（6 个基础职业）
```
sprites/warrior.png      → 战士职业精灵
sprites/mage.png         → 法师职业精灵
sprites/rogue.png        → 盗贼职业精灵
sprites/huntress.png     → 女猎手职业精灵
sprites/duelist.png      → 决斗者职业精灵
sprites/cleric.png       → 牧师职业精灵
sprites/avatars.png      → 角色头像/头像
```

**材质 ID**：
- `"sprites.hero.warrior"` → `sprites/warrior.png`
- `"sprites.hero.mage"` → `sprites/mage.png`
- 等等

#### 敌人与怪物（30+ 种类型）
```
rat.png, brute.png, spinner.png, dm300.png, wraith.png, skeleton.png, thief.png, 
tengu.png, bat.png, elemental.png, monk.png, warlock.png, golem.png, succubus.png, 
crab.png, bee.png, crystal_wisp.png, ghost.png, 等等
```

通过 `SpriteRegistry.registerMobAtlas(key, texturePath)` 和 `SpriteRegistry.getMobSprite()` 使用。

#### 特殊精灵
```
pet.png           → 宠物/伙伴精灵
amulet.png        → 约德尔护身符
```

---

### 3. **物品纹理**
用于库存物品、装备、消耗品和拾取战利品。

| 纹理 | 尺寸 | 使用位置 |
|---------|------|---------|
| `sprites/items.png` | 16×16 网格 | 主物品表（静态，ItemSpriteSheet） |
| `sprites/item_icons.png` | 32×32 | UI 中的物品图标 |
| 自定义物品表 | 动态 | 通过 `SpriteRegistry.registerItemTexture()` 的 Mod/DLC 物品 |

通过 `SpriteRegistry.useOverlay()` 和 `SpriteRegistry.overlayMap()` 支持 Overlay。

---

### 4. **UI/界面纹理**
用于所有用户界面元素、菜单、状态显示和 HUD 元素。

| 纹理 | 用途 | 使用位置 |
|---------|---------|--------|
| `chrome.png` | 窗口框架、边框 | 所有 `Wnd*` 类、`Chrome` 类 |
| `icons.png` | 通用 UI 图标 | 各种 UI 组件 |
| `status_pane.png` | 英雄状态栏 | `StatusPane` |
| `menu_pane.png` | 菜单背景 | `MenuPane` |
| `menu_button.png` | 按钮样式 | `StyledButton` |
| `toolbar.png` | 底部操作栏 | `Toolbar` |
| `shadow.png` | 投影 | UI 组件 |
| `boss_hp.png` | Boss 血条指示器 | Boss 战斗 |
| `surface.png` | 地表/地形效果 | 视觉效果 |
| `radial_menu.png` | 径向操作菜单 | `RadialMenu` |

**材质 ID**：
- `"ui.chrome"` → `chrome.png`
- `"ui.icons"` → `icons.png`
- `"ui.buffs.small"` → `buffs.png`
- `"ui.buffs.large"` → `large_buffs.png`
- `"ui.toolbar"` → `toolbar.png`

---

### 5. **增益与图标纹理**
用于状态效果图标、能力图标和 UI 指示器。

| 纹理 | 尺寸 | 用途 |
|---------|------|---------|
| `buffs.png` | 16×16 | 小增益图标 |
| `large_buffs.png` | 32×32 | 大增益图标（突出显示时） |
| `talent_icons.png` | 可变 | 天赋/能力图标 |
| `talent_button.png` | 可变 | 天赋选择按钮 |
| `hero_icons.png` | 可变 | 英雄职业图标 |
| `badges.png` | 可变 | 成就徽章 |

**材质 ID**：
- `"ui.buffs.small"` → `buffs.png`
- `"ui.buffs.large"` → `large_buffs.png`

通过 `SpriteRegistry.resolveBuffTexture(large)` 和 `SpriteRegistry.resolveUiIconsTexture()` 使用。

---

### 6. **特效纹理**
用于视觉效果、粒子、法术和冲击效果。

| 纹理 | 用途 |
|---------|---------|
| `effects/effects.png` | 通用游戏特效 |
| `effects/fireball.png` | 火焰法术效果 |
| `effects/specks.png` | 粒子效果、闪光 |
| `effects/spell_icons.png` | 法术能力图标 |
| `effects/text_icons.png` | 浮动文本指示器 |

---

### 7. **字体纹理**
用于文本渲染，特别是复古/像素字体。

```
fonts/pixel_font.png  → 游戏文本的像素化字体
```

---

### 8. **启动画面**
用于角色选择、关卡转换和过场画面。

| 类型 | 文件 |
|------|-------|
| **英雄启动画面** | `warrior.jpg`、`mage.jpg`、`rogue.jpg`、`huntress.jpg`、`duelist.jpg`、`cleric.jpg` |
| **关卡启动画面** | `sewers.jpg`、`prison.jpg`、`caves.jpg`、`city.jpg`、`halls.jpg` |

在 `TitleScene`、`HeroSelectScene`、`AmuletScene`、`InterlevelScene` 中使用。

---

## 架构：Segment + Overlay 系统

### Segment 层次结构

`Segment` 基类提供：
- 通过 `ensureLoaded()` 延迟加载纹理
- 用于 Overlay 解析的材质 ID（通过 `.as(materialId)`）
- 用于帧/精灵布局的 Atlas 管理
- Overlay 栈更改时自动重新加载

```
Segment<S extends Segment<S>>  (抽象基类)
  ├── ItemSegment              (带标签的物品网格)
  ├── MobSegment               (怪物精灵、命名区域、动画)
  ├── IconSegment              (UI 图标，基于网格)
  ├── UiSegment                (UI 框架，基于网格)
  └── TileSegment              (瓷砖表，基于网格)
```

### Segment 工作原理

1. **注册**：每个 Segment 使用基础纹理句柄创建
   ```java
   ItemSegment seg = SpriteRegistry.registerItemTexture("sprites/items.png", 16);
   seg.as("items.main");  // 设置材质 ID 用于 Overlay 解析
   seg.label("sword").label("shield");  // 添加命名帧
   ```

2. **材质解析**：获取精灵时，首先检查 Overlay
   ```java
   // 在 Segment.ensureLoaded() 内部：
   Object handle = SpriteRegistry.resolveMaterial(materialId, baseTextureHandle);
   // 如果 Overlay "my_pack" 有 "items.main" → 使用该值而不是 baseTextureHandle
   ```

3. **Overlay 栈**（底部 → 顶部）：索引越高，优先级越高
   ```
   栈：["default_pack", "my_pack", "custom_override"]
                                       ↑ 首先检查
   ```

---

### Overlay API

#### 推送/提升 Overlay 优先级
```java
SpriteRegistry.useOverlay("texture_pack_name");
// 将 Overlay 添加到栈顶，如果已存在则移动到顶部
```

#### 在 Overlay 中映射材质 → 纹理
```java
SpriteRegistry.overlayMap("my_pack", "items.main", "mods/items_custom.png");
SpriteRegistry.overlayMap("my_pack", "ui.chrome", "mods/ui_custom.png");
```

#### 解析材质（由 Segment 内部使用）
```java
Object texture = SpriteRegistry.resolveMaterial("items.main", fallbackHandle);
// 返回第一个 Overlay 映射，如果未找到则返回 fallback
```

#### 调试：查看 Overlay 栈
```java
List<String> stack = SpriteRegistry.overlays();
for (String key : stack) {
    System.out.println(key);  // 按优先级顺序打印 Overlay 键
}
```

---

### ImageMapping：与 noosa.Image 的桥梁

`ImageMapping` 是一个简单的数据结构，保存解析后的纹理 + 帧信息：

```java
public static class ImageMapping {
    public SmartTexture texture;    // 实际加载的纹理
    public RectF rect;              // 纹理内的 UV 帧
    public float height;            // 像素高度（用于缩放）
    public int size;                // 帧大小（16、32、64 等）
}
```

**在渲染中的使用**：
```java
ImageMapping map = SpriteRegistry.mapItemImage(imageId);
if (map != null) {
    // ItemSprite 示例：
    texture = map.texture;
    frame(map.rect);
    scale.set(16f / map.size);  // 如果 size != 16 则缩放
}
```

此设计允许 `noosa.Image`（及其子类）无缝工作：
- `texture` → 传递给 `image.texture()`
- `rect` → 传递给 `image.frame()`
- `height` / `size` → 用于缩放/透视

---

## 完整使用示例

### 步骤 1：注册自定义物品表
```java
// 在静态初始化器或 TexturePackManager 中：
ItemSegment customItems = SpriteRegistry.registerItemTexture("mods/items_cola.png", 16)
    .as("items.cola")                    // 设置材质 ID
    .label("cola_potion")
    .label("cola_bomb");
```

### 步骤 2：定义 Overlay
```java
// 创建纹理包 Overlay：
SpriteRegistry.useOverlay("summer_pack");

// 将材质映射到自定义纹理：
SpriteRegistry.overlayMap("summer_pack", "items.main", "packs/summer/items.png");
SpriteRegistry.overlayMap("summer_pack", "ui.chrome", "packs/summer/chrome.png");
SpriteRegistry.overlayMap("summer_pack", "sprites.hero.warrior", "packs/summer/warrior.png");
```

### 步骤 3：获取与渲染
```java
// 物品渲染（ItemSprite）：
ImageMapping map = SpriteRegistry.getItemImageMapping("cola_potion");
if (map != null) {
    sprite.texture = map.texture;
    sprite.frame(map.rect);
}

// 怪物渲染（MobSprite）：
ImageMapping mobMap = SpriteRegistry.getMobSprite("orc_warrior", "attack", Assets.Sprites.BRUTE);
if (mobMap != null) {
    sprite.texture = mobMap.texture;
    sprite.frame(mobMap.rect);
}

// UI 渲染（Chrome）：
Object chromeHandle = SpriteRegistry.resolveUiIconsTexture();
SmartTexture chromeTexture = TextureCache.get(chromeHandle);
```

---

## 材质 ID 命名约定

使用点分隔的层次结构名称：

```
<category>.<subcategory>.<specific>

示例：
  items.main              → 主物品表
  items.cola              → Cola Dungeon 自定义物品
  ui.chrome               → UI 窗口框架
  ui.buffs.small          → 小增益图标
  ui.buffs.large          → 大增益图标
  ui.icons                → 通用 UI 图标
  sprites.hero.warrior    → 战士英雄精灵
  sprites.mob.orc         → 兽人怪物精灵
  environment.tiles.sewers → 下水道瓷砖
  environment.water.sewers → 下水道水体
```

---

## 未来：TexturePackManager 集成

未来的 `TexturePackManager` 应该：

1. 加载 JSON 纹理包定义
2. 提取 Overlay 键和材质映射
3. 调用 `SpriteRegistry.useOverlay(overlayKey)`
4. 为每个映射调用 `SpriteRegistry.overlayMap()`
5. 可选地重新加载所有 Segment 以获取新纹理

示例 JSON 结构：
```json
{
  "pack_name": "summer_pack",
  "author": "Cola Team",
  "version": "1.0",
  "materials": {
    "items.main": "textures/items.png",
    "ui.chrome": "textures/chrome.png",
    "sprites.hero.warrior": "textures/warrior.png"
  }
}
```

---

## 总结

| 组件 | 用途 |
|-----------|---------|
| **Segment** | 拥有 Atlas，管理纹理加载 + Overlay 解析 |
| **Overlay 栈** | 优先使用纹理包材质而非默认值 |
| **材质 ID** | 纹理解析的稳定标识符（通过 `.as()`） |
| **ImageMapping** | 结果结构（纹理 + 帧）传递给 noosa.Image |
| **SpriteRegistry** | 所有 Segment + Overlay 的单例协调器 |

此设计实现了：
- ✅ 集中式纹理管理
- ✅ 纹理包支持（Overlay）
- ✅ 延迟加载 + 自动重新加载
- ✅ 完整的 noosa.Image 兼容性
- ✅ 易于扩展新的 Segment 类型
