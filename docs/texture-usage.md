## ColaDungeon：项目里哪些地方会用到贴图（Texture Usage Map）

这份文档回答一个问题：**当前项目哪些系统“必然会读取/渲染贴图”**，以及它们大致用的是什么“贴图类型”。

### 1) 地图/地形（Tilemap / Environment）
- **地形 tilesheet**：各区域地牢地面/墙体/装饰的拼图贴图（典型路径在 `Assets.Environment.TILES_*`）
  - 主要渲染入口：`core/src/main/java/com/zootdungeon/tiles/DungeonTilemap.java`
- **水体贴图**：各区域的水体动画贴图（典型路径在 `Assets.Environment.WATER_*`）
  - 渲染入口：同 `DungeonTilemap`（以及相关水体效果）
- **战争迷雾 / 视野**：迷雾遮罩、可视网格等（典型路径在 `Assets.Environment.*`）
  - 入口：`core/src/main/java/com/zootdungeon/tiles/FogOfWar.java`
- **主题/关卡**：关卡/主题包会决定用哪一套 tilesheet、水、装饰等
  - 入口：`core/src/main/java/com/zootdungeon/levels/**`、`core/src/main/java/com/zootdungeon/levels/themes/**`

### 2) 角色与怪物（Sprites / Mobs）
- **Hero / Mob 精灵图**：角色、怪物、NPC 的动画帧贴图（典型路径在 `Assets.Sprites.*`）
  - 入口：`core/src/main/java/com/zootdungeon/sprites/HeroSprite.java`
  - 入口：`core/src/main/java/com/zootdungeon/sprites/*Sprite.java`（大量怪物/NPC）
  - 入口：`core/src/main/java/com/zootdungeon/actors/mobs/Mob.java`（与 sprite 绑定/使用）

### 3) 物品（Items）
- **静态物品大图**：`Assets.Sprites.ITEMS` + `ItemSpriteSheet`（按网格切片）
  - 入口：`core/src/main/java/com/zootdungeon/sprites/ItemSprite.java`
  - 入口：`core/src/main/java/com/zootdungeon/sprites/ItemSpriteSheet.java`
- **动态物品贴图**：运行时注册的外部贴图（按 key → rect 取用）
  - 入口：`core/src/main/java/com/zootdungeon/sprites/SpriteRegistry.java`

### 4) UI（Interfaces / HUD / Windows）
- **界面皮肤与控件贴图**：窗口边框、按钮、状态栏、菜单、图标等（典型路径在 `Assets.Interfaces.*`）
  - 入口：`core/src/main/java/com/zootdungeon/ui/**`
  - 入口：`core/src/main/java/com/zootdungeon/windows/**`
  - 入口：`core/src/main/java/com/zootdungeon/scenes/**`（场景 UI）
- **Buff / Icon 图集**：buff 小/大图、通用 icons
  - 入口：`core/src/main/java/com/zootdungeon/ui/BuffIcon.java`

### 5) 特效（Effects）
- **粒子/法术/火球等特效贴图**（典型路径在 `Assets.Effects.*`）
  - 入口：通常在各类特效/动画对象中通过纹理缓存读取并绘制（工程里分散使用）

### 6) 字体（Fonts）
- **像素字体贴图**：用于 UI 文本渲染（典型路径在 `Assets.Fonts.*`）

### 7) 启动图/插画（Splash / Banners）
- **职业/区域插画**：jpg 背景等（典型路径在 `Assets.Splashes.*`）
  - 入口：标题/欢迎/展示类场景（`core/src/main/java/com/zootdungeon/scenes/**`）

### 8) 纹理读取的“底层入口”（你想做 overlay/替换时最关键）
- **TextureCache**：几乎所有贴图最终都会经由 `TextureCache.get(...)` 读入并缓存
- **Atlas / TextureFilm**：
  - `Atlas`：用 UV rect 管理网格/命名帧（`SPD-classes/src/main/java/com/watabou/gltextures/Atlas.java`）
  - `TextureFilm`：传统按格子切片的 film（很多 sprite 动画仍在用）


