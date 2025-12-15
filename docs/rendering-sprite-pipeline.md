## GameScene 渲染与 Sprite/Tilemap 显示机制（Scene → Group → Visual）

本文件说明 `com.zootdungeon.scenes.GameScene` 是如何把“逻辑对象”(Level/Mob/Heap 等) 显示成屏幕上的“图片”(Tile/Sprite/特效/UI) 的。

核心结论：

- **`GameScene` 不直接画贴图**，它把一堆可绘制对象（Noosa 的 `Visual`，如 `Tilemap/Image/MovieClip`）按顺序 `add(...)` 进场景。
- **绘制顺序基本由 add 的顺序决定**：先 add 的先画，后 add 的后画（覆盖在上层）。为了更好管理层级，`GameScene` 用多个 `Group` 分层。
- **“sprite 显示正确”主要依赖两件事**：
  - **选对 texture + frame**（UV/切片）：角色用 `TextureFilm` 定义动画帧；物品用 `ItemSpriteSheet` 或 `SpriteRegistry`；地形用 `DungeonTileSheet` 计算 tile index。
  - **格子坐标 → 屏幕坐标转换一致**：`DungeonTilemap.SIZE(16)` + `PixelScene.align(...)` 做像素对齐，保证不抖动/不偏移。

---

### 1) GameScene 的“分层渲染”结构

在 `GameScene.create()` 中，典型层级大致如下（命名可能随版本调整）：

- **地形层（terrain）**
  - `water`（水面贴图块）
  - `ripples`（水波纹特效）
  - `tiles`（地面/墙等基础地形 `DungeonTerrainTilemap`）
  - `customTiles`（自定义 tile）
  - `visualGrid`（辅助显示的网格/调试层）
  - `terrainFeatures`（植物/陷阱等地表要素）
- **关卡额外视觉层**
  - `levelVisuals`（`Dungeon.level.addVisuals()` 返回的额外视觉对象）
  - `floorEmitters`（地面粒子发射器）
- **物品层（heaps）**
  - 地上物品堆、箱子、骨堆等（`Heap` → `ItemSprite`）
- **单位层（mobs）**
  - `hero`（`HeroSprite`）
  - 各种 `Mob`（`Mob.sprite()` → `CharSprite`）
- **特效层**
  - `emitters/effects/overFogEffects/...` 等（飘字、光效、粒子等）
- **UI 层（uiCamera）**
  - `MenuPane/StatusPane/Toolbar/GameLog/Window/...`

实用经验：

- 想让某个效果“盖住怪物但不盖住 UI”，通常应放在 `effects`（world camera）而不是 UI 组件里。
- 想让 UI 永远在最上层，需要将 `camera` 设为 `PixelScene.uiCamera` 并 add 到 UI 区域（或用现有 UI 容器）。

---

### 2) 角色/怪物：`Mob.sprite()` + `sprite.link(mob)` 决定“显示正确”

#### 2.1 Sprite 是如何创建的

`Mob.sprite()` 一般只负责创建“对应的 sprite 类实例”（每个怪物在 `spriteClass` 里指定自己的 `CharSprite` 子类）：

```java
public CharSprite sprite() {
    return Reflection.newInstance(spriteClass);
}
```

`GameScene` 在把 mob 加到舞台时，会创建 sprite 并加入 `mobs` 组，然后 `link`：

```java
CharSprite sprite = mob.sprite();
sprite.visible = Dungeon.level.heroFOV[mob.pos];
mobs.add(sprite);
sprite.link(mob);
```

#### 2.2 `CharSprite.link(Char)` 做了什么

`link` 的作用是把“逻辑对象 Char”绑定到“视觉对象 CharSprite”，并同步初始位置/状态：

- **绑定引用**：`this.ch = ch; ch.sprite = this;`
- **初始位置**：`place(ch.pos)`（格子 → 屏幕）
- **朝向/阴影**：`turnTo(...)`、`renderShadow = true`
- **状态效果**：`ch.updateSpriteState()`（燃烧/隐身/护盾等视觉状态）

因此：如果你发现怪物“贴图对，但位置/状态不对”，通常优先检查：

- 有没有走到 `sprite.link(mob)`
- `ch.pos` 是否正确
- `updateSpriteState()` 是否被调用/是否缺状态映射

---

### 3) 物品/掉落：`Heap` → `ItemSprite.link(heap)`

地上的物品堆 `Heap` 的显示通常由 `ItemSprite` 负责。加入关卡时大致流程：

```java
ItemSprite sprite = heap.sprite = (ItemSprite) heaps.recycle(ItemSprite.class);
sprite.revive();
sprite.link(heap);
heaps.add(sprite);
```

`ItemSprite.link(heap)` 关键点：

- `view(heap)`：决定当前 heap 显示的图标（单个物品、箱子、骨堆等）
- `visible = heap.seen`：物品是否可见与“是否被看见”绑定
- `place(heap.pos)`：用 heap 的格子坐标摆放

---

### 4) 地形 Tile：`DungeonTilemap` + `DungeonTerrainTilemap.getTileVisual(...)`

地形显示属于“tilemap 渲染”：

- `DungeonTilemap` 基于 16×16 的 `TextureFilm` 将 tilesheet 切片。
- `updateMap()` 时遍历每个 cell，把 `Level.map[pos]`（逻辑 tile id，例如 `Terrain.WALL`）映射成 `data[pos]`（tilesheet 上的“视觉索引”）。

核心抽象点在 `getTileVisual(pos, tile, flat)`：子类（比如 `DungeonTerrainTilemap`）负责实现映射逻辑。

`DungeonTerrainTilemap.getTileVisual` 常见规则：

- 先查 `DungeonTileSheet.directVisuals`：一些 tile 可以直接映射到固定视觉索引。
- 水/深渊等需要根据周边 tile 做“拼接”(stitching)：
  - 例如水会看上下左右的地形，用 `DungeonTileSheet.stitchWaterTile(...)` 得到正确边缘/角落。
- raised/flat 视角会走不同映射（`flat` 参数）。

因此：如果你发现“地形贴图不对/边缘拼接不对”，通常要看：

- 关卡 `Level.tilesTex()` 返回了哪张 tilesheet
- `DungeonTileSheet` 对这个 `Terrain.xxx` 的映射是否正确
- 邻居格子的 tile 值是否符合 stitch 逻辑的预期

---

### 5) 格子坐标为何能对齐：`worldToCamera` + `PixelScene.align`

角色与物品都采用“格子坐标 → 世界坐标”的统一换算方式：

- 基础尺度：`DungeonTilemap.SIZE == 16`
- 对齐方式：`PixelScene.align(Camera.main, value)` 把坐标对齐到“设备像素网格”，避免摄像机缩放时产生亚像素抖动。

如果你看到 sprite “抖动/模糊”，通常是：

- 位置没有走 `PixelScene.align(...)`
- camera zoom 与 UI zoom 混用，导致坐标体系不一致

---

### 6) 排查清单：从“看到一个图”追到“具体 spritesheet/frame”

当你在游戏里看到某个东西，想知道它到底来自哪张图、用的是哪一帧，可按类型追踪：

- **Hero（主角）**
  - `HeroSprite` 构造中决定 spritesheet
  - `updateArmor()` 用 `TextureFilm` 选帧定义动画
- **Mob（怪物）**
  - 先找该怪物类的 `spriteClass`
  - 找对应 `CharSprite` 子类：它通常在构造里 `texture(...)` 并定义 `idle/run/attack/...` 帧序列
- **Item/Heap（物品/箱子/骨堆）**
  - `Item.image()` 返回一个 int 索引
  - `ItemSprite.frame(image)`：
    - 常规：`Assets.Sprites.ITEMS + ItemSpriteSheet.film.get(image)`
    - 自定义映射（如 `image >= 6000`）：走 `SpriteRegistry.mapItemImage(image)`，返回 `texture + RectF`
- **地形 Tile**
  - `Level.map[pos]` 的逻辑 tile id（`Terrain.xxx`）
  - `DungeonTerrainTilemap.getTileVisual(...)` 映射到 `DungeonTileSheet` 的视觉索引
  - 最终从 `tileset.get(visualIndex)` 得到 UV


