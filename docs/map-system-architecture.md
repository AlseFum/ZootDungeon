# 地图系统架构指南

## 概述

ColaDungeon 的地图系统已从线性的 `depth/branch` 模型演变为**基于图结构的架构（`LevelGraph`）**。这允许非线性进度、支线关卡和动态特殊楼层，同时保持与存档系统的向后兼容性。

此外，地图视觉效果（Tilemaps）现在通过 `SpriteRegistry` 动态管理，支持纹理包和 Mod。

---

## 1. 核心概念

### 1.1 图模型（`LevelGraph`）

不再仅仅使用 `depth`（整数）和 `branch`（整数），游戏中的每个楼层现在都是图中的一个**节点**。

#### **LevelNode**
`LevelNode` 表示一个可访问的楼层。
- **`id` (String)**：唯一逻辑标识符（例如 `"main:1"`、`"special:5"`）。
- **`depth` (int)**：难度/进度层级（向后兼容）。
- **`branch` (int)**：分支 ID（0 表示主线，100+ 表示特殊）。
- **`generated` (boolean)**：该楼层是否已被访问/生成。
- **`prevId` / `nextId`**：线性段的链接。
- **`parent...`**：特殊支线关卡的返回引用（返回到哪里）。

#### **图初始化**
- **主线路径**：启动时，`LevelGraph.initMainPath()` 创建线性节点 `"main:1"` 到 `"main:26"`。
- **特殊节点**：通过 `LevelGraph.createSpecialNode()` 动态创建。

### 1.2 向后兼容性（`depth`/`branch`）
引擎和存档系统仍然依赖 `depth` 和 `branch` 整数作为文件存储键（`level_depth_branch.dat`）。
- `LevelGraph.forDepthBranch(d, b)` 将这些坐标映射回逻辑 `LevelNode`。
- 标准分支：`0`（主线）、`1-4`（标准 Boss/任务分支）。
- 特殊分支：`100+`（为特殊节点动态分配）。

---

## 2. 关卡生成与转换

### 2.1 生成关卡（`Dungeon.newLevel`）
1. **清理**：清除当前关卡/角色。
2. **工厂**：调用 `ThemeManager.createLevel(depth, branch)` 实例化 Java 类。
3. **图更新**：调用 `LevelGraph.markGenerated(depth, branch)`。
4. **构建**：运行 `level.create()` 生成地形。
5. **出口修补**：如果是**特殊节点**：
   - 生成器找到 `REGULAR_EXIT`（向上的楼梯）。
   - 将其修补为指向节点的 `parentDepth`/`parentBranch`。
   - 这确保玩家始终可以从特殊关卡返回。

### 2.2 转换（`InterlevelScene`）
- **主线下楼**：`depth++`，`branch=0`。
- **主线上楼**：`depth--`，`branch=0`。
- **特殊传送**：
  - 由 `StoneOfLevelSelect` 等物品使用。
  - 设置 `InterlevelScene.mode = RETURN`。
  - 从目标 `LevelNode` 显式设置目标 `depth` 和 `branch`。

---

## 3. 视觉效果：地图贴图系统

地图视觉效果（地形 + 水）由 `SpriteRegistry` 管理。

### 3.1 注册（动态）
Mod 或内容包在启动时注册主题：

```java
SpriteRegistry.registerTilemap(
    "mymod:shadow_forest",
    new TilemapDef(
        "mymod/tiles_shadow_forest.png",  // 地形（16x16 网格）
        "mymod/water_shadow_forest.png"   // 水（动画帧）
    )
);
```

### 3.2 在关卡中使用
关卡使用 `tilemapKey` 指定其视觉主题：

```java
public class ShadowForestLevel extends Level {
    public ShadowForestLevel() {
        // 自动从 SpriteRegistry 获取纹理
        this.tilemapKey = "mymod:shadow_forest";
    }
}
```

基类 `Level` 处理查找：
- `tilesTex()` → 查询 `SpriteRegistry`，如果缺失则回退到默认值。
- `waterTex()` → 查询 `SpriteRegistry`，如果缺失则回退到默认值。

---

## 4. 特殊关卡示例：楼层选择石

`StoneOfLevelSelect` 物品展示了图系统的强大功能：

1. **列出已访问**：显示所有 `generated == true` 的节点。
2. **创建支线关卡**：
   - 调用 `LevelGraph.createSpecialNode(currentId, currentDepth, currentBranch)`。
   - 分配新 ID（例如 `"special:101"`）。
   - 分配新的高位分支 ID。
   - 将英雄传送到这个新坐标。
3. **返回**：
   - 新关卡正常生成。
   - 其出口自动修补为返回到起始点。

---

## 5. 存档系统集成

- **图状态**：`LevelGraph` 状态尚未完全序列化（它从 `generatedLevels` 集合重建）。
- **当前节点**：保存 `Dungeon.currentLevelId` 以了解玩家在逻辑上的位置。
- **关卡数据**：实际地形/怪物保存在由 `depth`+`branch` 键控的 `levels` bundle 中。

## 6. 未来扩展

- **JSON 图**：从 JSON 加载主线路径结构，而不是硬编码。
- **双向链接**：允许任意两个节点之间的任意连接（传送门）。
- **持久化图**：保存完整的 `LevelGraph` 结构，以允许永久性的地图更改（例如，被摧毁的桥梁）。
