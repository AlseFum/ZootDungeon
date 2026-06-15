# 存档系统参考指南

## 概述

`SaveManager` 负责 ColaDungeon 中的所有数据持久化。它使用**基于 Bundle 的系统**（类似于 Android Bundles）将游戏状态序列化为 JSON 文件。

**关键文件类型：**
- `game.dat`（或 `save-X.dat`）：包含全局游戏状态的主存档文件。
- `depth_X_Y.dat`：单个关卡数据（位于存档 bundle 内）。
- `global.json`：跨存档的全局设置/解锁数据。

---

## 1. 快速参考（速查表）

### 核心方法

| 方法 | 用途 |
|--------|-------|
| `SaveManager.saveGame(slot, bundle)` | 将当前游戏状态保存到指定槽位。 |
| `SaveManager.loadGame(slot)` | 从指定槽位加载主游戏 bundle。 |
| `SaveManager.saveLevel(slot, d, b, bundle)` | 保存特定关卡的数据。 |
| `SaveManager.loadLevel(slot, d, b)` | 加载特定关卡的数据。 |
| `SaveManager.deleteSlot(slot)` | 完全删除一个存档槽位。 |

### 数据流程
```
Dungeon.saveAll()
  -> 创建 Bundle "game"
  -> 保存 Hero、Inventory 等
  -> SaveManager.saveGame(game)
  -> SaveManager.saveLevel(currentLevel)
```

---

## 2. Bundle 系统 (`com.watabou.utils.Bundle`)

`Bundle` 类是序列化的基本单位。

### 写入数据
```java
Bundle bundle = new Bundle();
bundle.put("name", "Warrior");
bundle.put("level", 5);
bundle.put("inventory", inventoryArray); // 自动处理 Bundlable[]
```

### 读取数据
```java
String name = bundle.getString("name");
int level = bundle.getInt("level");
// 恢复实现了 Bundlable 的对象
hero.restoreFrom(bundle); 
```

### `Bundlable` 接口
任何需要保存的类都必须实现 `Bundlable`：
```java
public interface Bundlable {
    void storeIn(Bundle bundle);
    void restoreFrom(Bundle bundle);
}
```

---

## 3. 存档文件结构

### 槽位结构
一个存档槽位（例如 `save-0.dat`）实际上是一个数据集合，根据平台实现（`DeviceCompat`）的不同，可能被压缩或存储在受管理的目录中。

**逻辑层次结构：**
```
存档槽位
  ├── hero (Bundle)
  │     ├── class
  │     ├── strength
  │     └── inventory
  ├── dungeon (Bundle)
  │     ├── depth
  │     ├── branch
  │     └── generated_levels (Array)
  └── levels (BundleMap)
        ├── depth_1_0 (Bundle: 关卡数据)
        ├── depth_2_0 (Bundle: 关卡数据)
        └── ...
```

---

## 4. 全局数据 (`global.json`)

跨所有游戏运行的共享数据（徽章、解锁、排行榜）由特定的管理器类处理，这些类委托 `SaveManager` 进行 I/O 操作。

- **Badges**：成就/功绩。
- **Rankings**：高分列表。
- **Settings**：音频/图形偏好设置。

---

## 5. 关卡序列化

关卡数据量较大，因此与主英雄状态分开保存。

### `Level.storeIn(Bundle)`
- **Tiles**：行程编码的地形数组。
- **Mobs**：所有 `Mob` 实体的列表。
- **Items**：`Heap`（掉落物品）列表。
- **Blobs**：气体/火焰地图（`Blob` 实体）。
- **Plants**：植被状态。

### `Level.restoreFrom(Bundle)`
- 重新实例化 Mobs/Items/Plants。
- 重建地图可视化状态。

---

## 6. 版本控制

存档系统包含版本检查（`ColaDungeon.version`）以处理迁移。
- 加载时，如果 `saveVersion < currentVersion`，可能会运行更新脚本。
- 当 `bundle.get()` 返回来自旧版本的可选数据时，始终检查 null 值。
