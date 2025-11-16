## 地图贴图系统指南（Tilemap Texture System）

本指南介绍如何使用 `SpriteRegistry` 管理地图贴图（地形和水纹理），支持静态和动态两种方式。

---

## 📋 系统概览

地图贴图系统现在统一通过 `SpriteRegistry` 管理，支持：
- **静态贴图**：在子类中直接返回固定路径（传统方式）
- **动态贴图**：通过 `tilemapKey` 在运行时从 `SpriteRegistry` 查询（新方式，支持 Mod）

---

## 一、当前地图系统工作原理

### 1. 地图贴图的组成

每个关卡（Level）需要两种纹理：
- **地形纹理（Tiles Texture）**：墙壁、地板、门等地形元素
- **水纹理（Water Texture）**：水面动画效果

### 2. 传统使用方式（静态）

子类重写 `tilesTex()` 和 `waterTex()` 方法：

```java
public class SewerLevel extends Level {
    @Override
    public String tilesTex() {
        return Assets.Environment.TILES_SEWERS;
    }

    @Override
    public String waterTex() {
        return Assets.Environment.WATER_SEWERS;
    }
}
```

### 3. 纹理路径定义

在 `Assets.Environment` 中定义：

```java
public static class Environment {
    public static final String TILES_SEWERS = "environment/tiles_sewers.png";
    public static final String TILES_PRISON = "environment/tiles_prison.png";
    public static final String TILES_CAVES  = "environment/tiles_caves.png";
    public static final String TILES_CITY   = "environment/tiles_city.png";
    public static final String TILES_HALLS  = "environment/tiles_halls.png";
    
    public static final String WATER_SEWERS = "environment/water0.png";
    public static final String WATER_PRISON = "environment/water1.png";
    public static final String WATER_CAVES  = "environment/water2.png";
    public static final String WATER_CITY   = "environment/water3.png";
    public static final String WATER_HALLS  = "environment/water4.png";
}
```

---

## 二、使用 SpriteRegistry 管理地图贴图（新方式）

### 1. 注册动态地图贴图

在游戏初始化或 Mod 加载时注册：

```java
import com.zootdungeon.sprites.SpriteRegistry;
import com.zootdungeon.sprites.SpriteRegistry.TilemapDef;

// 注册完整的地图贴图集（地形 + 水）
SpriteRegistry.registerTilemap(
    "mymod:shadow_forest",
    new TilemapDef(
        "mymod/tiles_shadow_forest.png",  // 地形纹理
        "mymod/water_shadow_forest.png"   // 水纹理
    )
);

// 只注册地形纹理（使用默认水纹理）
SpriteRegistry.registerTilemap(
    "mymod:crystal_cave",
    new TilemapDef("mymod/tiles_crystal_cave.png")
);
```

### 2. 在 Level 中使用动态贴图

**方式一：设置 tilemapKey（推荐）**

```java
public class ShadowForestLevel extends Level {
    
    public ShadowForestLevel() {
        // 设置动态贴图 key
        tilemapKey = "mymod:shadow_forest";
    }
    
    // 不需要重写 tilesTex() 和 waterTex()
    // Level 基类会自动从 SpriteRegistry 查询
    
    @Override
    protected boolean build() {
        // 关卡生成逻辑...
        return true;
    }
}
```

**方式二：重写方法并手动查询**

```java
public class CustomLevel extends Level {
    
    @Override
    public String tilesTex() {
        // 尝试动态查询，失败则使用 fallback
        return SpriteRegistry.tilemapTilesTextureOr(
            Assets.Environment.TILES_CAVES,  // fallback
            "mymod:custom_tilemap"           // 动态 key
        );
    }
    
    @Override
    public String waterTex() {
        return SpriteRegistry.tilemapWaterTextureOr(
            Assets.Environment.WATER_CAVES,
            "mymod:custom_tilemap"
        );
    }
}
```

### 3. 条件使用动态贴图

根据配置或条件动态选择：

```java
public class AdaptiveLevel extends Level {
    
    private boolean useCustomTheme = false;
    
    public AdaptiveLevel() {
        // 根据某些条件决定是否使用自定义主题
        if (Dungeon.depth > 10 && SpriteRegistry.hasTilemap("mymod:dark_theme")) {
            tilemapKey = "mymod:dark_theme";
            useCustomTheme = true;
        }
    }
    
    @Override
    public String tilesTex() {
        if (tilemapKey != null) {
            return SpriteRegistry.tilemapTilesTextureOr(
                Assets.Environment.TILES_HALLS,
                tilemapKey
            );
        }
        return Assets.Environment.TILES_HALLS;
    }
    
    @Override
    public String waterTex() {
        if (tilemapKey != null) {
            return SpriteRegistry.tilemapWaterTextureOr(
                Assets.Environment.WATER_HALLS,
                tilemapKey
            );
        }
        return Assets.Environment.WATER_HALLS;
    }
}
```

---

## 三、SpriteRegistry API 速查

### 注册相关

```java
// 注册地图贴图（地形 + 水）
SpriteRegistry.registerTilemap(String key, TilemapDef def)

// 创建 TilemapDef
new TilemapDef(String tilesTexture, String waterTexture)
new TilemapDef(String tilesTexture)  // 只有地形纹理
```

### 查询相关

```java
// 查询地形纹理（带 fallback）
String tiles = SpriteRegistry.tilemapTilesTextureOr(
    String fallbackTexture,
    String key
);

// 查询水纹理（带 fallback）
String water = SpriteRegistry.tilemapWaterTextureOr(
    String fallbackTexture,
    String key
);

// 检查是否已注册
boolean exists = SpriteRegistry.hasTilemap(String key);

// 获取完整定义
TilemapDef def = SpriteRegistry.getTilemapDef(String key);
```

---

## 四、完整示例：创建自定义地图主题

### 步骤 1：准备贴图资源

将贴图文件放入 `assets` 目录：
```
core/src/main/assets/
  └── mymod/
      ├── tiles_shadow_forest.png  (16x16 切片的地形贴图)
      └── water_shadow_forest.png  (水面动画贴图)
```

### 步骤 2：注册贴图

在游戏启动时或 Mod 初始化时：

```java
import com.zootdungeon.sprites.SpriteRegistry;
import com.zootdungeon.sprites.SpriteRegistry.TilemapDef;

public class MyModInit {
    public static void init() {
        // 注册暗影森林主题
        SpriteRegistry.registerTilemap(
            "mymod:shadow_forest",
            new TilemapDef(
                "mymod/tiles_shadow_forest.png",
                "mymod/water_shadow_forest.png"
            )
        );
        
        // 注册水晶洞穴主题（复用默认水纹理）
        SpriteRegistry.registerTilemap(
            "mymod:crystal_cave",
            new TilemapDef("mymod/tiles_crystal_cave.png")
        );
    }
}
```

### 步骤 3：创建使用自定义贴图的关卡

```java
package com.zootdungeon.levels;

import com.zootdungeon.Assets;
import com.zootdungeon.sprites.SpriteRegistry;

public class ShadowForestLevel extends Level {
    
    public ShadowForestLevel() {
        // 方式 1：直接设置 tilemapKey（最简单）
        tilemapKey = "mymod:shadow_forest";
    }
    
    // 或者方式 2：重写方法提供 fallback
    @Override
    public String tilesTex() {
        return SpriteRegistry.tilemapTilesTextureOr(
            Assets.Environment.TILES_CAVES,  // fallback 到洞穴贴图
            "mymod:shadow_forest"
        );
    }
    
    @Override
    public String waterTex() {
        return SpriteRegistry.tilemapWaterTextureOr(
            Assets.Environment.WATER_CAVES,
            "mymod:shadow_forest"
        );
    }
    
    @Override
    protected boolean build() {
        // 关卡生成逻辑
        // ...
        return true;
    }
    
    @Override
    protected void createMobs() {
        // 生成怪物
    }
    
    @Override
    protected void createItems() {
        // 生成物品
    }
}
```

---

## 五、贴图规格要求

### 地形贴图（Tiles Texture）

- **尺寸**：16x16 像素切片
- **格式**：PNG（支持透明）
- **布局**：16 列网格，按 `DungeonTileSheet` 定义的顺序排列
- **参考**：查看 `environment/tiles_sewers.png` 等原版贴图

### 水纹理（Water Texture）

- **尺寸**：16x16 像素切片
- **格式**：PNG（支持透明）
- **布局**：水面动画帧 + 边缘拼接变体
- **参考**：查看 `environment/water0.png` 等原版贴图

---

## 六、与现有系统的兼容性

### ✅ 完全兼容

- 现有的 Level 子类无需修改，继续使用静态方式
- 新关卡可以选择使用动态方式
- 动态查询失败时自动 fallback 到静态路径

### 🔄 渐进迁移

可以逐步将现有关卡迁移到动态系统：

```java
// 旧代码（仍然有效）
public class SewerLevel extends Level {
    @Override
    public String tilesTex() {
        return Assets.Environment.TILES_SEWERS;
    }
}

// 新代码（可选升级）
public class SewerLevel extends Level {
    public SewerLevel() {
        tilemapKey = "core:sewers";  // 如果注册了的话
    }
    
    @Override
    public String tilesTex() {
        // 先尝试动态，失败则用静态
        return SpriteRegistry.tilemapTilesTextureOr(
            Assets.Environment.TILES_SEWERS,
            tilemapKey
        );
    }
}
```

---

## 七、常见问题

### Q: 必须使用 SpriteRegistry 吗？
**A:** 不是。传统的直接返回路径的方式仍然有效。SpriteRegistry 是为了支持 Mod 和动态内容。

### Q: 如何测试自定义贴图？
**A:** 
1. 将贴图放入 `assets` 目录
2. 在游戏启动时注册
3. 创建测试关卡使用该 key
4. 如果加载失败，会自动使用 fallback

### Q: 可以在运行时切换贴图吗？
**A:** 可以。修改 `tilemapKey` 后重新加载关卡即可。但注意纹理缓存可能需要清理。

### Q: 如何调试贴图加载问题？
**A:** 
```java
// 检查是否注册
if (SpriteRegistry.hasTilemap("mymod:theme")) {
    System.out.println("Tilemap registered!");
} else {
    System.out.println("Tilemap not found!");
}

// 获取定义
TilemapDef def = SpriteRegistry.getTilemapDef("mymod:theme");
if (def != null) {
    System.out.println("Tiles: " + def.tilesTexture);
    System.out.println("Water: " + def.waterTexture);
}
```

---

## 八、最佳实践

### ✅ 推荐做法

1. **使用命名空间**：key 格式为 `"mod:name"`，避免冲突
2. **提供 fallback**：始终提供静态纹理作为后备
3. **集中注册**：在一个地方统一注册所有贴图
4. **文档化**：为自定义贴图编写说明文档

### ❌ 避免做法

1. 不要在关卡构造函数外修改 `tilemapKey`
2. 不要假设动态贴图一定存在
3. 不要在 `tilesTex()`/`waterTex()` 中进行复杂计算
4. 不要忘记提供 fallback

---

## 九、示例代码集合

### 示例 1：简单的自定义主题

```java
// 注册
SpriteRegistry.registerTilemap(
    "example:ice_cave",
    new TilemapDef("example/tiles_ice.png", "example/water_ice.png")
);

// 使用
public class IceCaveLevel extends Level {
    public IceCaveLevel() {
        tilemapKey = "example:ice_cave";
    }
}
```

### 示例 2：条件主题切换

```java
public class DynamicLevel extends Level {
    public DynamicLevel() {
        // 根据深度选择主题
        if (Dungeon.depth < 5) {
            tilemapKey = "mod:theme_light";
        } else if (Dungeon.depth < 15) {
            tilemapKey = "mod:theme_dark";
        } else {
            tilemapKey = "mod:theme_hell";
        }
    }
    
    @Override
    public String tilesTex() {
        return SpriteRegistry.tilemapTilesTextureOr(
            Assets.Environment.TILES_HALLS,
            tilemapKey
        );
    }
}
```

### 示例 3：Mod 批量注册

```java
public class MyMod {
    public static void registerTilemaps() {
        String[] themes = {"forest", "desert", "volcano", "ice"};
        
        for (String theme : themes) {
            SpriteRegistry.registerTilemap(
                "mymod:" + theme,
                new TilemapDef(
                    "mymod/tiles_" + theme + ".png",
                    "mymod/water_" + theme + ".png"
                )
            );
        }
    }
}
```

---

## 十、总结

通过 `SpriteRegistry` 管理地图贴图，你可以：

✅ **保持兼容性**：现有代码无需修改  
✅ **支持动态内容**：Mod 可以注册自定义贴图  
✅ **简化管理**：统一的注册和查询接口  
✅ **提供后备方案**：动态查询失败时自动 fallback  

开始使用时，建议从简单的 `tilemapKey` 设置开始，然后根据需要逐步扩展功能。

