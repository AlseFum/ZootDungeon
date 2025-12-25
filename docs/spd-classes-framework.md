# SPD-classes 游戏引擎框架文档

## 📋 概述

**SPD-classes** 是一个基于 **LibGDX** 的2D游戏引擎框架，最初由 Oleg Dolya (watabou) 为 Pixel Dungeon 开发，后由 Evan Debenham 为 Shattered Pixel Dungeon 扩展。这个框架提供了完整的游戏开发基础设施，包括渲染、输入处理、音频、动画和数据持久化等功能。

## 🏗️ 架构概览

```
com.watabou
├── glscripts/      # OpenGL 着色器脚本
├── gltextures/     # 纹理管理系统
├── glwrap/         # OpenGL 封装层
├── input/          # 输入处理系统
├── noosa/          # 核心游戏引擎 (Noosa Engine)
│   ├── audio/      # 音频系统
│   ├── particles/  # 粒子系统
│   ├── tweeners/   # 动画补间系统
│   └── ui/         # UI 组件基础
└── utils/          # 工具类库
```

---

## 📦 模块详解

### 1. glscripts - OpenGL 着色器脚本

| 类名 | 功能描述 |
|------|----------|
| **Script** | OpenGL 着色器程序的基类，管理着色器的编译、链接和使用 |

### 2. gltextures - 纹理管理系统

| 类名 | 功能描述 |
|------|----------|
| **Atlas** | 纹理图集管理，支持从大图中提取小图区域 |
| **SmartTexture** | 智能纹理类，封装 OpenGL 纹理操作，支持纹理过滤和包装模式 |
| **TextureCache** | 纹理缓存管理器，提供纹理的加载、缓存和创建功能（如纯色纹理、渐变纹理） |

### 3. glwrap - OpenGL 封装层

| 类名 | 功能描述 |
|------|----------|
| **Attribute** | OpenGL 顶点属性封装 |
| **Blending** | 混合模式管理（透明度混合、叠加等） |
| **Framebuffer** | 帧缓冲对象封装，用于离屏渲染 |
| **Matrix** | 4x4 变换矩阵操作（平移、旋转、缩放） |
| **Program** | OpenGL 着色器程序管理 |
| **Quad** | 四边形顶点数据生成 |
| **Renderbuffer** | 渲染缓冲对象封装 |
| **Shader** | 着色器编译和管理（顶点着色器/片段着色器） |
| **Texture** | 底层纹理操作封装 |
| **Uniform** | 着色器 Uniform 变量封装 |
| **Vertexbuffer** | 顶点缓冲对象 (VBO) 封装 |

### 4. input - 输入处理系统

| 类名 | 功能描述 |
|------|----------|
| **ControllerHandler** | 游戏手柄控制器处理，支持摇杆和按钮输入 |
| **GameAction** | 游戏动作枚举基类，定义抽象的游戏操作 |
| **InputHandler** | 输入事件处理器，统一管理键盘、鼠标、触摸输入 |
| **KeyBindings** | 按键绑定管理，支持键盘和手柄的自定义绑定 |
| **KeyEvent** | 键盘事件封装，使用 Signal 模式分发事件 |
| **PointerEvent** | 指针事件封装（鼠标/触摸），支持 DOWN/UP/CANCEL/HOVER 类型 |
| **ScrollEvent** | 滚动事件封装（鼠标滚轮/触摸滚动） |

### 5. noosa - 核心游戏引擎

#### 5.1 核心类

| 类名 | 功能描述 |
|------|----------|
| **Game** | 游戏主类，实现 LibGDX ApplicationListener，管理游戏生命周期、场景切换、渲染循环 |
| **Scene** | 场景基类，游戏的顶层容器，管理整个场景的更新和渲染 |
| **Gizmo** | 所有游戏对象的基类，定义 exists/alive/active/visible 状态和生命周期方法 |
| **Group** | 游戏对象容器，管理子对象的添加、删除、更新和渲染 |
| **Visual** | 可视化对象基类，扩展 Gizmo 添加位置、尺寸、颜色、变换等属性 |

#### 5.2 渲染类

| 类名 | 功能描述 |
|------|----------|
| **Image** | 图像渲染类，支持纹理显示、帧裁剪、翻转、颜色调整 |
| **MovieClip** | 动画精灵类，扩展 Image 支持帧动画播放 |
| **Tilemap** | 瓦片地图渲染，用于显示大型地图 |
| **NinePatch** | 九宫格图片，支持可拉伸的UI背景 |
| **BitmapText** | 位图字体文本渲染 |
| **RenderedText** | 系统字体文本渲染 |
| **ColorBlock** | 纯色矩形块 |
| **Halo** | 光晕效果 |
| **PseudoPixel** | 伪像素点效果 |
| **SkinnedBlock** | 可平铺纹理块 |

#### 5.3 相机和脚本

| 类名 | 功能描述 |
|------|----------|
| **Camera** | 相机类，管理视口、缩放、滚动、震动效果 |
| **NoosaScript** | 默认渲染着色器脚本，支持光照 |
| **NoosaScriptNoLighting** | 无光照渲染着色器脚本 |
| **TextureFilm** | 纹理切片管理，从图集中提取帧 |
| **TextInput** | 文本输入组件 |
| **Resizable** | 可调整大小接口 |

#### 5.4 输入区域

| 类名 | 功能描述 |
|------|----------|
| **PointerArea** | 指针输入区域，处理点击、拖拽、悬停事件 |
| **ScrollArea** | 滚动输入区域，扩展 PointerArea 支持滚轮事件 |

### 6. noosa/audio - 音频系统

| 类名 | 功能描述 |
|------|----------|
| **Music** | 背景音乐管理（单例），支持播放、淡入淡出、音量控制、播放列表 |
| **Sample** | 音效管理（单例），支持音效加载、播放、延迟播放 |

### 7. noosa/particles - 粒子系统

| 类名 | 功能描述 |
|------|----------|
| **Emitter** | 粒子发射器，控制粒子的生成、位置、数量 |
| **BitmaskEmitter** | 位图遮罩发射器，基于图像形状发射粒子 |
| **PixelParticle** | 像素粒子基类 |

### 8. noosa/tweeners - 动画补间系统

| 类名 | 功能描述 |
|------|----------|
| **Tweener** | 补间动画基类，提供时间插值框架 |
| **AlphaTweener** | 透明度渐变动画 |
| **PosTweener** | 位置移动动画 |
| **ScaleTweener** | 缩放动画 |
| **CameraScrollTweener** | 相机滚动动画 |
| **Delayer** | 延迟执行器 |

### 9. noosa/ui - UI 组件基础

| 类名 | 功能描述 |
|------|----------|
| **Component** | UI 组件基类，提供位置、尺寸、布局管理 |
| **Cursor** | 鼠标光标管理 |

### 10. utils - 工具类库

#### 10.1 数据结构

| 类名 | 功能描述 |
|------|----------|
| **Point** | 整数点坐标 (x, y) |
| **PointF** | 浮点点坐标，支持向量运算 |
| **Rect** | 整数矩形区域 |
| **RectF** | 浮点矩形区域 |
| **SparseArray** | 稀疏数组实现 |
| **BArray** | 布尔数组工具 |

#### 10.2 数据持久化

| 类名 | 功能描述 |
|------|----------|
| **Bundle** | 数据序列化容器，支持 JSON 格式存储，用于游戏存档 |
| **Bundlable** | 可序列化接口，实现此接口的对象可被 Bundle 存储 |
| **FileUtils** | 文件操作工具，支持内部/外部存储 |
| **GameSettings** | 游戏设置基类，提供偏好设置管理 |

#### 10.3 数学和随机

| 类名 | 功能描述 |
|------|----------|
| **Random** | 随机数生成器，支持种子栈、各种分布（均匀、正态、加权） |
| **GameMath** | 游戏数学工具（插值、范围限制等） |
| **ColorMath** | 颜色数学运算（混合、插值、亮度调整） |
| **Graph** | 图数据结构和算法 |
| **PathFinder** | A* 寻路算法实现，支持 4/8 方向邻居 |

#### 10.4 其他工具

| 类名 | 功能描述 |
|------|----------|
| **Signal** | 观察者模式实现，用于事件分发 |
| **Callback** | 回调接口 |
| **Reflection** | 反射工具，支持类实例化 |
| **DeviceCompat** | 设备兼容性检测（iOS/Android/Desktop） |
| **PlatformSupport** | 平台支持接口 |
| **BitmapCache** | 位图缓存管理 |
| **BitmapFilm** | 位图切片工具 |

---

## 🔧 核心设计模式

### 1. 场景图模式 (Scene Graph)
```
Scene
  └── Group
        ├── Visual (Image, MovieClip, etc.)
        ├── Group
        │     └── Visual
        └── Gizmo
```

### 2. 组件生命周期
```java
Gizmo {
    exists  // 是否存在于场景中
    alive   // 是否存活
    active  // 是否更新
    visible // 是否渲染
    
    update()   // 每帧更新
    draw()     // 每帧渲染
    destroy()  // 销毁时调用
    kill()     // 标记死亡
    revive()   // 复活
}
```

### 3. 事件分发 (Signal 模式)
```java
Signal<PointerEvent> pointerSignal = new Signal<>();
pointerSignal.add(listener);
pointerSignal.dispatch(event);
```

### 4. 数据序列化 (Bundle 模式)
```java
// 保存
Bundle bundle = new Bundle();
bundle.put("health", 100);
bundle.put("position", new Point(10, 20));

// 恢复
int health = bundle.getInt("health");
Point pos = bundle.get("position");
```

---

## 🎮 使用示例

### 创建基本游戏
```java
public class MyGame extends Game {
    @Override
    public void create() {
        super.create();
        switchScene(TitleScene.class);
    }
}
```

### 创建场景
```java
public class GameScene extends Scene {
    @Override
    public void create() {
        super.create();
        
        // 添加背景图片
        Image bg = new Image(Assets.BACKGROUND);
        add(bg);
        
        // 添加精灵
        MovieClip hero = new HeroSprite();
        add(hero);
    }
}
```

### 处理输入
```java
PointerArea button = new PointerArea(0, 0, 100, 50) {
    @Override
    protected void onClick(PointerEvent event) {
        // 点击处理
    }
};
add(button);
```

### 播放动画
```java
add(new AlphaTweener(sprite, 0f, 0.5f) {
    @Override
    protected void onComplete() {
        sprite.kill();
    }
});
```

---

## 🎨 如何添加画面元素

### 基本原理

Noosa 引擎使用**场景图模式**管理所有可视元素。要添加画面元素，需要：
1. 创建可视对象（继承自 Visual 或其子类）
2. 设置位置、大小、颜色等属性
3. 使用 `add()` 方法添加到场景图中

### 方法一：使用现有类（推荐）

#### 添加静态图片
```java
// 在 Scene 或 Group 中
Image myImage = new Image("textures/my_image.png");
myImage.x = 100;
myImage.y = 50;
myImage.scale.set(2.0f); // 2倍缩放
add(myImage);
```

#### 从图集中提取帧
```java
TextureFilm film = new TextureFilm("sprites/items.png", 16, 16);
Image icon = new Image("sprites/items.png");
icon.frame(film.get(5)); // 使用第5帧
icon.x = 200;
icon.y = 100;
add(icon);
```

#### 添加动画精灵
```java
MovieClip animation = new MovieClip("sprites/character.png");

// 创建动画序列
TextureFilm film = new TextureFilm("sprites/character.png", 16, 16);
MovieClip.Animation walkAnim = new MovieClip.Animation(8, true); // 8 FPS, 循环
walkAnim.frames(film, 0, 1, 2, 3); // 使用帧 0,1,2,3

animation.play(walkAnim);
animation.x = 150;
animation.y = 200;
add(animation);
```

### 方法二：创建自定义 Visual 类

```java
public class MyCustomElement extends Visual {
    
    private ColorBlock background;
    private BitmapText label;
    
    public MyCustomElement(String text) {
        super(0, 0, 100, 50);
        
        // 创建背景
        background = new ColorBlock(width, height, 0xFF336699);
        add(background);
        
        // 创建文本
        label = new BitmapText(text, PixelScene.pixelFont);
        label.x = 10;
        label.y = 15;
        add(label);
    }
    
    @Override
    public void update() {
        super.update();
        // 自定义更新逻辑
    }
}

// 使用
MyCustomElement element = new MyCustomElement("Hello World");
element.x = 300;
element.y = 150;
add(element);
```

### 方法三：创建 UI 组件

```java
public class MyButton extends Component {
    
    private NinePatch bg;
    private BitmapText label;
    
    public MyButton(String text) {
        super();
        
        bg = Chrome.get(Chrome.Type.BUTTON);
        add(bg);
        
        label = PixelScene.renderTextBlock(text, 9);
        add(label);
    }
    
    @Override
    protected void layout() {
        super.layout();
        
        bg.size(width, height);
        bg.x = x;
        bg.y = y;
        
        label.setPos(
            x + (width - label.width()) / 2,
            y + (height - label.height()) / 2
        );
    }
    
    @Override
    protected void onClick() {
        Sample.INSTANCE.play(Assets.Sounds.CLICK);
    }
}
```

### 方法四：添加粒子效果

```java
public void createExplosion(float x, float y) {
    Emitter emitter = new Emitter();
    emitter.pos(x, y);
    
    emitter.pour(new Emitter.Factory() {
        @Override
        public void emit(Emitter emitter, int index, float x, float y) {
            PixelParticle particle = (PixelParticle) emitter.recycle(PixelParticle.class);
            particle.reset(x, y);
            particle.color(0xFFFF6600); // 橙色
            particle.lifespan = 1.0f;
            particle.speed.set(Random.Float(-50, 50), Random.Float(-50, 50));
        }
    }, 0.01f);
    
    add(emitter);
}
```

### 完整示例：技能冷却指示器

```java
public class SkillCooldownIndicator extends Component {
    
    private Image skillIcon;
    private Image cooldownOverlay;
    private BitmapText cooldownText;
    private float maxCooldown;
    private float currentCooldown;
    
    public SkillCooldownIndicator(String iconTexture, float cooldown) {
        super();
        this.maxCooldown = cooldown;
        this.currentCooldown = 0;
        
        skillIcon = new Image(iconTexture);
        add(skillIcon);
        
        cooldownOverlay = new Image(TextureCache.createSolid(0x80000000));
        cooldownOverlay.visible = false;
        add(cooldownOverlay);
        
        cooldownText = new BitmapText("", PixelScene.pixelFont);
        cooldownText.visible = false;
        add(cooldownText);
        
        setSize(32, 32);
    }
    
    @Override
    protected void layout() {
        skillIcon.x = x;
        skillIcon.y = y;
        skillIcon.scale.set(width / skillIcon.width, height / skillIcon.height);
        
        cooldownOverlay.x = x;
        cooldownOverlay.y = y;
        cooldownOverlay.scale.set(width / cooldownOverlay.width, height / cooldownOverlay.height);
        
        cooldownText.x = x + (width - cooldownText.width()) / 2;
        cooldownText.y = y + (height - cooldownText.height()) / 2;
    }
    
    @Override
    public void update() {
        super.update();
        
        if (currentCooldown > 0) {
            currentCooldown -= Game.elapsed;
            
            if (currentCooldown <= 0) {
                currentCooldown = 0;
                cooldownOverlay.visible = false;
                cooldownText.visible = false;
            } else {
                cooldownOverlay.visible = true;
                cooldownText.visible = true;
                cooldownText.text(String.format("%.1f", currentCooldown));
                
                float progress = currentCooldown / maxCooldown;
                cooldownOverlay.scale.y = progress * (height / cooldownOverlay.height);
            }
        }
    }
    
    public void startCooldown() {
        currentCooldown = maxCooldown;
    }
    
    public boolean isReady() {
        return currentCooldown <= 0;
    }
}

// 使用示例
SkillCooldownIndicator fireball = new SkillCooldownIndicator("skills/fireball.png", 5.0f);
fireball.setPos(50, 50);
add(fireball);
fireball.startCooldown(); // 开始冷却
```

### 关键要点

- ✅ **场景图结构** - 所有元素必须通过 `add()` 添加到场景图
- ✅ **继承层次** - Visual → Image/MovieClip, Group → Scene/Component
- ✅ **生命周期** - `create()` 初始化，`update()` 每帧更新，`draw()` 渲染
- ✅ **坐标系统** - 左上角为原点，x 向右，y 向下
- ✅ **变换属性** - position, scale, rotation, origin, color
- ✅ **纹理管理** - 使用 TextureFilm 从图集提取帧，TextureCache 管理纹理

### 渲染流程

```
Game.render()
    ↓
Scene.draw()
    ↓
Group.draw() (遍历子对象)
    ↓
Visual.draw() (更新变换矩阵)
    ↓
Image.draw() (绑定纹理，设置着色器，渲染)
    ↓
GPU 渲染到屏幕
```

---

## 🎯 Atlas 精灵系统使用指南

### 基本概念

ColaDungeon 现在使用基于 Atlas 的现代精灵管理系统，提供比传统 TextureFilm 更强大和灵活的功能。

### 物品精灵管理

#### 注册和使用物品精灵
```java
// 1. 注册纹理并添加标签
SpriteRegistry.ItemSegment weapons = SpriteRegistry.registerItemTexture("weapons/swords.png", 16)
    .label("iron_sword")      // Frame 0
    .label("steel_sword")     // Frame 1
    .label("magic_sword");    // Frame 2

// 2. 通过名称访问精灵（推荐方式）
ImageMapping ironSword = SpriteRegistry.getItemImageMapping("iron_sword");

// 3. 在物品类中使用
public class MySword extends MeleeWeapon {
    @Override
    public int image() {
        return SpriteRegistry.itemByName("iron_sword");
    }
}
```

#### 创建精灵变体
```java
// 基于现有纹理创建变体
TextureBuilder builder = new TextureBuilder("iron_sword");
ImageMapping goldenSword = builder
    .tint(0x80FFD700)           // 金色调色
    .buildAndRegister("golden_sword");

// 使用静态方法快速创建
ImageMapping redSword = TextureBuilder.createRecolored(
    "iron_sword", 
    "red_iron_sword", 
    0x80FF0000 // 红色调色
);
```

### 怪物精灵管理

#### 注册灵活布局的 Mob 精灵
```java
// 1. 注册 Mob Atlas
MobSegment goblinSprites = SpriteRegistry.registerMobAtlas(
    "goblin", 
    "mobs/goblin_sheet.png"
);

// 2. 手动定义不同尺寸的精灵区域
goblinSprites
    .addSprite("idle_0", 0, 0, 16, 16)
    .addSprite("idle_1", 16, 0, 16, 16)
    .addSprite("walk_0", 32, 0, 16, 16)
    .addSprite("walk_1", 48, 0, 16, 16)
    .addSprite("attack", 0, 16, 24, 20); // 攻击动作使用不同尺寸

// 3. 或设置规则网格并添加动画
MobSegment orcSprites = SpriteRegistry.registerMobAtlas("orc", "mobs/orc_sheet.png")
    .setupGrid(16, 16)                    // 16x16 网格
    .addAnimation("idle", 0, 4)           // 帧 0-3 为待机动画
    .addAnimation("walk", 4, 6)           // 帧 4-9 为行走动画
    .addAnimation("attack", 10, 3);       // 帧 10-12 为攻击动画
```

#### 访问 Mob 精灵
```java
// 通过名称访问
ImageMapping goblinIdle = SpriteRegistry.getMobSprite(
    "goblin", 
    "idle_0", 
    "fallback_texture.png"
);

// 直接从段获取
ImageMapping orcWalk = orcSprites.getSprite("walk_2");
```

### 高级纹理构建

#### 基于现有纹理的复杂修改
```java
// 1. 基于现有纹理创建
TextureBuilder builder = new TextureBuilder("base_sword");

ImageMapping enchantedSword = builder
    .copyFrom("magic_glow", 2, 2)        // 复制魔法光效
    .tint(0x4000FFFF)                    // 应用青色调色
    .buildAndRegister("enchanted_sword");

// 2. 程序化生成精灵系列
for (int level = 1; level <= 10; level++) {
    String spriteName = "sword_level_" + level;
    int glowIntensity = level * 25;
    int glowColor = 0xFF000000 | (glowIntensity << 16) | (glowIntensity << 8) | 255;
    
    TextureBuilder levelBuilder = new TextureBuilder("base_sword");
    levelBuilder.tint(glowColor);
    
    if (level >= 5) {
        levelBuilder.tint(0x40FFD700); // 高级物品额外金色调色
    }
    
    levelBuilder.buildAndRegister(spriteName);
}
```

### 混合尺寸精灵管理

#### 同一图集中的不同尺寸精灵
```java
MobSegment bossSprites = SpriteRegistry.registerMobAtlas("dragon_boss", "mobs/dragon_atlas.png");

bossSprites
    // 小尺寸精灵（远景）
    .addSprite("small_idle", 0, 0, 16, 16)
    .addSprite("small_fly", 16, 0, 16, 16)
    
    // 中等尺寸精灵（正常视角）
    .addSprite("medium_idle", 0, 32, 32, 32)
    .addSprite("medium_attack", 32, 32, 32, 32)
    
    // 大尺寸精灵（特写）
    .addSprite("large_roar", 0, 96, 64, 64)
    .addSprite("large_breath", 64, 96, 64, 64);

// 根据需要访问不同尺寸的精灵
ImageMapping smallDragon = bossSprites.getSprite("small_idle");
ImageMapping largeDragon = bossSprites.getSprite("large_roar");
```

### 直接使用 Atlas 绘制到画面

#### 基本用法：Atlas + Image

`Atlas` 类用于管理纹理图集中的子区域，配合 `Image` 类可以将纹理绘制到画面上。

```java
import com.watabou.gltextures.Atlas;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.Image;
import com.watabou.noosa.Scene;
import com.watabou.utils.RectF;

public class MyScene extends Scene {
    
    private Image playerSprite;
    
    @Override
    public void create() {
        super.create();
        
        // 1. 加载纹理
        SmartTexture texture = TextureCache.get("images/sprites.png");
        
        // 2. 创建 Atlas
        Atlas atlas = new Atlas(texture);
        
        // 3. 设置网格布局（假设每个精灵是 16x16 像素）
        atlas.grid(16, 16);  // 自动计算列数
        
        // 或者手动添加命名区域
        atlas.add("player", 0, 0, 16, 16);      // 左上角 16x16
        atlas.add("enemy", 16, 0, 32, 16);      // 第二个 16x16
        atlas.add("item", 0, 16, 16, 32);      // 第二行第一个
        
        // 4. 创建 Image 并设置帧
        playerSprite = new Image(texture);
        RectF frame = atlas.get("player");  // 或 atlas.get(0) 获取索引
        playerSprite.frame(frame);
        
        // 5. 设置位置
        playerSprite.x = 100;
        playerSprite.y = 100;
        
        // 6. 添加到场景（Image 继承自 Visual，可以直接添加）
        add(playerSprite);
    }
}
```

#### 使用网格索引访问

```java
// 创建规则网格的 Atlas
Atlas atlas = new Atlas(texture);
atlas.grid(16, 16);  // 16x16 的网格

// 通过索引访问（从左到右，从上到下）
Image sprite0 = new Image(texture);
sprite0.frame(atlas.get(0));  // 第 0 帧（左上角）

Image sprite5 = new Image(texture);
sprite5.frame(atlas.get(5));  // 第 5 帧
```

#### 使用命名键访问

```java
// 创建 Atlas 并添加命名区域
Atlas atlas = new Atlas(texture);
atlas.add("player_idle", 0, 0, 16, 16);
atlas.add("player_walk", 16, 0, 32, 16);
atlas.add("enemy_goblin", 0, 16, 16, 32);

// 使用命名键获取帧
Image player = new Image(texture);
player.frame(atlas.get("player_idle"));

// 切换动画帧
player.frame(atlas.get("player_walk"));
```

#### 与 TextureFilm 对比

```java
// 方式 1: 使用 TextureFilm（更高级的封装）
TextureFilm film = new TextureFilm("images/sprites.png", 16, 16);
Image sprite = new Image("images/sprites.png");
sprite.frame(film.get(0));

// 方式 2: 直接使用 Atlas（更底层，更灵活）
SmartTexture texture = TextureCache.get("images/sprites.png");
Atlas atlas = new Atlas(texture);
atlas.grid(16, 16);
Image sprite = new Image(texture);
sprite.frame(atlas.get(0));
```

#### 关键要点

- **Atlas.get()** 返回 `RectF`（UV 坐标，范围 0-1）
- **Image.frame(RectF)** 设置要显示的纹理区域
- **Image** 继承自 `Visual`，可添加到 `Group` 或 `Scene`
- 渲染由 `Image.draw()` 自动处理，无需手动调用
- 位置通过 `x`、`y` 设置，支持 `scale`、`angle` 等变换

### 迁移指南

#### 从 TextureFilm 迁移到 Atlas
```java
// 旧方式（仍然兼容）
TextureFilm film = new TextureFilm(texture, 16, 16);
RectF frame = film.get(5);

// 新方式（推荐）
ItemSegment segment = SpriteRegistry.registerItemTexture("items.png", 16);
segment.label("my_item"); // 同时添加到 Atlas

// 按名称访问（更高效）
ImageMapping item = segment.get("my_item");

// 按 ID 访问（向后兼容）
ImageMapping itemById = segment.get(SpriteRegistry.itemByName("my_item"));
```

### 性能优化建议

#### 最佳实践
- 🎯 **优先使用命名访问** - `getItemImageMapping(name)` 比 ID 查找更高效
- 📦 **合理组织图集** - 将相关精灵放在同一个 Atlas 中
- 🎨 **利用 TextureBuilder** - 创建精灵变体时使用增强功能
- 🔄 **渐进式迁移** - 新功能使用 Atlas，现有代码保持不变

#### 注意事项
- Atlas 查找比 TextureFilm 索引访问更快
- 支持不规则精灵布局，减少纹理浪费
- 命名访问使代码更易读和维护
- 完全向后兼容，无需修改现有代码

---

## 🎯 Atlas 精灵系统使用指南

### 基本概念

ColaDungeon 现在使用基于 Atlas 的现代精灵管理系统，提供比传统 TextureFilm 更强大和灵活的功能。

### 物品精灵管理

#### 注册和使用物品精灵

```java
// 1. 注册纹理并添加标签
SpriteRegistry.ItemSegment weapons = SpriteRegistry.registerItemTexture("weapons/swords.png", 16)
    .label("iron_sword")      // Frame 0
    .label("steel_sword")     // Frame 1
    .label("magic_sword");    // Frame 2

// 2. 通过名称访问精灵（推荐）
SpriteRegistry.ImageMapping ironSword = SpriteRegistry.getItemImageMapping("iron_sword");

// 3. 在物品类中使用
public class MySword extends MeleeWeapon {
    @Override
    public int image() {
        return SpriteRegistry.itemByName("iron_sword");
    }
}
```

#### 创建精灵变体

```java
// 基于现有纹理创建变体
TextureBuilder builder = new TextureBuilder("iron_sword");
SpriteRegistry.ImageMapping goldenSword = builder
    .tint(0x80FFD700)           // 金色调色
    .buildAndRegister("golden_sword");

// 使用静态方法快速创建
SpriteRegistry.ImageMapping redSword = TextureBuilder.createRecolored(
    "iron_sword", 
    "red_iron_sword", 
    0x80FF0000 // 红色调色
);
```

### 怪物精灵管理

#### 灵活的 Mob 精灵布局

```java
// 1. 注册 Mob Atlas（支持不同尺寸精灵）
SpriteRegistry.MobSegment goblinSprites = SpriteRegistry.registerMobAtlas(
    "goblin", 
    "mobs/goblin_sheet.png"
);

// 2. 手动定义精灵区域（适用于不规则布局）
goblinSprites
    .addSprite("idle_0", 0, 0, 16, 16)
    .addSprite("idle_1", 16, 0, 16, 16)
    .addSprite("walk_0", 32, 0, 16, 16)
    .addSprite("attack", 0, 16, 24, 20); // 不同尺寸

// 3. 或使用规则网格
SpriteRegistry.MobSegment orcSprites = SpriteRegistry.registerMobAtlas(
    "orc", 
    "mobs/orc_sheet.png"
);

orcSprites
    .setupGrid(16, 16)                    // 16x16 网格
    .addAnimation("idle", 0, 4)           // 帧 0-3 为 idle
    .addAnimation("walk", 4, 6);          // 帧 4-9 为 walk

// 4. 访问 Mob 精灵
SpriteRegistry.ImageMapping goblinIdle = SpriteRegistry.getMobSprite(
    "goblin", 
    "idle_0", 
    "fallback_texture.png"
);
```

### 高级纹理构建

#### 基于现有纹理的复杂修改

```java
// 1. 基于现有纹理创建复杂变体
TextureBuilder builder = new TextureBuilder("base_sword");

SpriteRegistry.ImageMapping enchantedSword = builder
    .copyFrom("magic_glow", 2, 2)        // 添加魔法光效
    .tint(0x4000FFFF)                    // 青色调色
    .buildAndRegister("enchanted_sword");

// 2. 程序化生成精灵系列
for (int level = 1; level <= 10; level++) {
    String spriteName = "sword_level_" + level;
    int glowIntensity = level * 25;
    int glowColor = 0xFF000000 | (glowIntensity << 16) | (glowIntensity << 8) | 255;
    
    TextureBuilder.createRecolored("base_sword", spriteName, glowColor);
}
```

#### 不同尺寸精灵混合使用

```java
// 在同一个 Atlas 中使用不同尺寸的精灵
SpriteRegistry.MobSegment bossSprites = SpriteRegistry.registerMobAtlas(
    "dragon_boss", 
    "mobs/dragon_atlas.png"
);

bossSprites
    // 小精灵（远景）
    .addSprite("small_idle", 0, 0, 16, 16)
    .addSprite("small_fly", 16, 0, 16, 16)
    
    // 中等精灵（正常视角）
    .addSprite("medium_idle", 0, 32, 32, 32)
    .addSprite("medium_attack", 32, 32, 32, 32)
    
    // 大精灵（特写）
    .addSprite("large_roar", 0, 96, 64, 64);
```

### 迁移指南

#### 从 TextureFilm 迁移到 Atlas

```java
// 旧方式（仍然兼容）
TextureFilm film = new TextureFilm(texture, 16, 16);
RectF frame = film.get(5);

// 新方式（推荐）
SpriteRegistry.ItemSegment segment = SpriteRegistry.registerItemTexture("items.png", 16);
segment.label("my_item"); // 同时添加到 Atlas

// 按名称访问（更高效）
SpriteRegistry.ImageMapping item = segment.get("my_item");

// 按 ID 访问（向后兼容）
SpriteRegistry.ImageMapping itemById = segment.get(SpriteRegistry.itemByName("my_item"));
```

### 最佳实践

#### 推荐的使用模式

1. **命名访问优先** - 使用 `getItemImageMapping(name)` 而非数字 ID
2. **合理组织图集** - 将相关精灵放在同一个 Atlas 中
3. **利用变体功能** - 使用 TextureBuilder 创建精灵变体而非重复资源
4. **渐进式迁移** - 新功能使用 Atlas，现有代码保持不变

#### 性能优化建议

- ✅ **Atlas 查找更快** - 命名访问比索引查找效率更高
- ✅ **纹理共享** - 多个精灵共享同一张大纹理
- ✅ **批量渲染** - 相同纹理的精灵可以批量处理
- ✅ **内存优化** - 支持不规则布局，减少纹理浪费

---

## 🚨 开发注意事项

### glwrap 模块
**glwrap** 模块是 OpenGL 的底层封装，**一般情况下不需要修改**：

- ✅ **稳定可靠** - 经过充分测试的 OpenGL 封装
- ✅ **性能优化** - 针对游戏渲染优化
- ❌ **避免修改** - 除非有特殊的渲染需求

**开发建议**：
- 专注于 `noosa` 层的现有类
- 使用 `Image`、`Visual`、`Camera` 等高级抽象
- 如需图形功能扩展，优先在 `noosa` 层添加新类

### 模块依赖关系
```
游戏逻辑 (core/)
    ↓
noosa 引擎
    ↓
glwrap + gltextures + input
    ↓
LibGDX
```

---

## 📝 版权信息

- **Pixel Dungeon** - Copyright (C) 2012-2015 Oleg Dolya
- **Shattered Pixel Dungeon** - Copyright (C) 2014-2024 Evan Debenham
- **License**: GNU General Public License v3.0

---

## 🔗 依赖

- **LibGDX** - 跨平台游戏开发框架
- **org.json** - JSON 解析库

---

*本文档基于 ColaDungeon 项目的 SPD-classes 模块生成*
