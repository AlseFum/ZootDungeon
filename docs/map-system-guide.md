## 地图系统与 LevelGraph 说明

本文件说明 ColaDungeon 现在的地图系统结构、`LevelGraph` 图模型，以及 `StoneOfLevelSelect` 相关的特殊楼层机制，方便后续扩展和调试。

---

### 1. 旧系统回顾：基于 depth/branch 的线性结构

- **核心状态在 `Dungeon`**
  - `public static int depth;`
  - `public static int branch;`
  - `public static Level level;`
- **关卡生成**
  - 通过 `ThemeManager.createLevel(depth, branch)` 根据当前 `depth/branch` 生成或加载对应的 `Level` 子类。
  - 存档系统用 `(depth, branch)` 组合来区分不同楼层，每层一个 `SaveManager.saveLevel(save, depth, branch, bundle)`。
- **上下楼层**
  - 楼梯/传送门使用 `LevelTransition`：
    - `destDepth` / `destBranch` 决定目标楼层。
    - `destType` 决定目标层入口类型（`REGULAR_ENTRANCE` / `REGULAR_EXIT` 等）。
  - `InterlevelScene` 根据 `Mode`（`DESCEND` / `ASCEND` / `RETURN` 等）和当前 `depth/branch` 来选择下一层。

在这种设计下，地图逻辑是“深度线性”的，特殊地图通常被塞到某个固定 `depth/branch` 上，而不是作为一个独立的节点存在。

---

### 2. 新系统：LevelGraph 图结构

为支持“命名地图 + 网状结构 + 随时添加特殊楼层”，引入了 `LevelGraph`。

#### 2.1 LevelGraph.LevelNode

`LevelGraph.LevelNode` 是一个轻量级的关卡节点描述：

- **字段**
  - `String id`  
    地图的逻辑 ID，例如：
    - 主线：`"main:1"`, `"main:2"`, …  
    - 特殊图：`"special:0"`, `"special:1"`, …
  - `int depth`, `int branch`  
    对应底层引擎和存档系统使用的 `(depth, branch)`，保持与旧系统兼容。
  - `String nextId`, `String prevId`  
    主线用来表达上下层关系（相当于原来的 `depth+1 / depth-1`）。
  - `boolean generated`  
    标记该层是否已经被实际生成过（用来驱动调试工具，如 `StoneOfLevelSelect` 的楼层列表）。
  - `Class<? extends Level> levelClass`（目前预留，尚未大量使用）。
  - `boolean special`  
    是否为特殊层（side-level），不是主线线性 progression 的一部分。
  - `int parentDepth`, `int parentBranch`, `String parentId`  
    对于 `special == true` 的节点，记录它是从哪个楼层“分支出来”的，用作回城目标。

#### 2.2 主线初始化：initMainPath

启动或读档时，`LevelGraph` 会初始化主线路径：

- `LevelGraph.initMainPath(int maxDepth)`：
  - 创建 `"main:1" .. "main:maxDepth"` 的节点：
    - `id = "main:" + depth`
    - `depth = 1..maxDepth`, `branch = 0`
    - `prevId/nextId` 串成一条线性链。
  - 当前默认 `DEFAULT_MAIN_MAX_DEPTH = 26`，即 1–26 楼主线。

#### 2.3 由 depth/branch 映射回节点

`LevelGraph.forDepthBranch(int depth, int branch)`：

- 若存在已有节点 `node.depth == depth && node.branch == branch`，直接返回该节点（包括特殊楼层）。
- 否则：
  - 对 `branch == 0 && 1 <= depth <= DEFAULT_MAIN_MAX_DEPTH`，返回 `"main:depth"` 对应节点。
  - 其它情况则创建/返回一个 `"branch{branch}:{depth}"` 风格的节点，以便兼容旧逻辑。

> 注意：这只是底层实现的兼容桥接，逻辑上推荐使用 `id` 作为楼层标识，而不是直接使用 `depth/branch`。

#### 2.4 已生成楼层列表

`LevelGraph.markGenerated(int depth, int branch)` 和 `LevelGraph.initFromGeneratedCodes(...)`：

- `Dungeon.newLevel()` 在生成非 `DeadEndLevel` 时会调用 `LevelGraph.markGenerated(depth, branch)`，标记楼层已生成。
- 读档时，从旧的 `generatedLevels`（`depth + 1000 * branch` 编码）恢复，调用 `initFromGeneratedCodes` 还原哪些节点已经生成过。

`LevelGraph.generatedNodes()`：

- 返回所有 `generated == true` 的 `LevelNode` 列表，用于显示“已去过的楼层”列表（例如调试工具）。

---

### 3. 特殊楼层：createSpecialNode

为支持“只通过某个道具/事件才能到达的地图”，新系统引入了 `createSpecialNode`：

```java
public static LevelNode createSpecialNode(String parentId, int parentDepth, int parentBranch)
```

- 作用：
  - 在 `LevelGraph` 中创建一个新的 `LevelNode`，它：
    - `id = "special:" + nodes.size()`（全局唯一，增量分配）。
    - `depth = parentDepth`（难度沿用当前楼层的深度）。
    - `branch = nextSpecialBranch++`（从 100 开始的高位分支，防止和常规分支冲突）。
    - `special = true`。
    - 记录父层信息：
      - `parentId = parentId`
      - `parentDepth = parentDepth`
      - `parentBranch = parentBranch`
- 使用场景：
  - 通过某个道具（如 `StoneOfLevelSelect`）或事件，为当前楼层“动态挂载”一个只读配置的特殊地图节点。
  - 这些节点**不会**被主线 `next/prev` 串起来，因此自然下楼/上楼不会进入，只能通过显式导航到达。

---

### 4. Dungeon 与特殊楼层的出入口逻辑

#### 4.1 生成新层：Dungeon.newLevel()

`Dungeon.newLevel()` 主要流程：

1. 清理当前 `Dungeon.level` 和 `Actor`。
2. 通过 `ThemeManager.createLevel(depth, branch)` 创建 `Level` 实例。
3. 若不是 `DeadEndLevel`：
   - 维护旧的 `generatedLevels` 列表（兼容老存档格式）。
   - 调用 `LevelGraph.markGenerated(depth, branch)` 更新图节点。
   - 按旧逻辑更新 `Statistics.deepestFloor` 等统计。
4. `level.create()` 生成具体地图内容。
5. **特殊层处理**：
   - 通过 `LevelGraph.forDepthBranch(depth, branch)` 拿到当前节点。
   - 若 `node.special == true`：
     - 获取本层的 `REGULAR_EXIT` 传送门：
       ```java
       LevelTransition exit = level.getTransition(LevelTransition.Type.REGULAR_EXIT);
       ```
     - 若存在，则重写其目的地：
       ```java
       exit.destDepth = node.parentDepth;
       exit.destBranch = node.parentBranch;
       exit.destType = LevelTransition.Type.REGULAR_ENTRANCE;
       ```
     - 这样，玩家从这个出口离开时，`InterlevelScene` 会把他送回原来的父层入口。

#### 4.2 切换层与 currentLevelId

`Dungeon.switchLevel(Level level, int pos)` 会：

- 设置 `Dungeon.level = level`，并安置 `hero.pos`。
- 调用 `LevelGraph.markGenerated(depth, branch)`。
- 通过 `LevelGraph.forDepthBranch(depth, branch)` 更新：
  - `Dungeon.currentLevelId = node.id;`

`saveGame` / `loadGame` 会额外保存和恢复 `CURRENT_LEVEL_ID`，确保重新加载存档时还能知道当前逻辑 ID。

---

### 5. StoneOfLevelSelect 的行为与特殊楼层示例

`StoneOfLevelSelect` 是一个用于调试/管理的符文石，位于：

- `core/src/main/java/com/zootdungeon/items/stones/StoneOfLevelSelect.java`
- 已被加入 `DebugSupply`（`DebugSupply` 会给出此石头）。

#### 5.1 基本功能：在已生成的楼层之间跳转

- 使用石头（默认动作）时，会打开一个 `WndOptions`：
  - 选项列表来自 `LevelGraph.generatedNodes()`。
  - 每项显示为：`id (depth X, branch Y)`，便于调试。
- 选择某一项时：
  - 调用内部 `teleportTo(hero, node)`：
    - 施加短暂隐身 buff。
    - 设置：
      ```java
      InterlevelScene.mode = InterlevelScene.Mode.RETURN;
      InterlevelScene.returnDepth = node.depth;
      InterlevelScene.returnBranch = node.branch;
      InterlevelScene.returnPos = -1; // 在目标层入口处生成
      ```
    - 切换到 `InterlevelScene`，最终到达目标楼层入口。

#### 5.2 新增功能：动态创建“只能通过石头到达的随机楼层”

为展示图结构的威力，我们给 `StoneOfLevelSelect` 增加了一个示例功能：

- 在楼层列表最后追加一项：
  - 文本为：`"新建随机特殊楼层"`。
- 选择该项时：
  1. 使用当前层的逻辑 ID 与 `(depth, branch)` 创建一个 `special` 节点：
     ```java
     LevelNode special = LevelGraph.createSpecialNode(
         Dungeon.currentLevelId,
         Dungeon.depth,
         Dungeon.branch
     );
     ```
     - 这个新节点：
       - `id` 形如 `"special:0"`、`"special:1"`……
       - `special = true`。
       - `parentId` / `parentDepth` / `parentBranch` 记录当前层。
       - `depth` = 当前 `Dungeon.depth`（保持难度一致），`branch` = 一个高位分支号。
  2. 立即调用 `teleportTo(hero, special)`：
     - 通过 `RETURN` 模式把玩家送到新节点对应的 `(depth, branch)` 楼层。
     - 实际地图生成仍走 `Dungeon.newLevel()` + `ThemeManager.createLevel(...)`，但逻辑 ID 已经是 `"special:N"`。

> 由于没有任何普通楼梯指向这些 `"special:*"` 节点，它们**只会出现在 StoneOfLevelSelect 的选单里**，因此：
> - 只能通过这块石头进入；
> - 且每个特殊层都有一个退出口会自动被设为“返回父层入口”，保证有路可退。

#### 5.3 activate 行为

- `StoneOfLevelSelect` 继承自 `Runestone`，必须实现：
  ```java
  @Override
  protected void activate(int cell) {
      if (Dungeon.hero != null) {
          openSelector(Dungeon.hero);
      }
  }
  ```
- 因为这是一个“跨楼层传送”的工具，`cell` 并不参与逻辑：  
  不论丢到地图哪里，都会打开楼层选择窗口，而不是基于落点做传送。

---

### 6. 扩展与使用建议

1. **添加更多特殊楼层类型**
   - 可以在 `LevelGraph.LevelNode` 上增加更多元数据（如专属 `theme`、`levelClass` 等），并在 `Dungeon.newLevel()` 中根据 `node.special` 和这些信息选择不同的 `Level` 子类。

2. **脚本化/JSON 化地图表**
   - 目前主线图结构由 `initMainPath` 硬编码。
   - 后续可以将节点和连边（`nextId/prevId/links` 等）定义在 JSON 里，通过 `LevelGraph` 加载，从而让 Mod 自由添加地图节点。

3. **调试时的典型用法**
   - 在任意层使用 DebugSupply：
     - 拿到 `StoneOfLevelSelect`。
     - 列表中选择现有层进行快速回到/跳转。
     - 或点击“新建随机特殊楼层”自动生成 side-level，并体验从该层出口返回原层的全流程。

通过本设计，ColaDungeon 的地图系统从单一的 `depth/branch` 线性流程，升级为一个可以自由扩展、具备**图拓扑结构**的系统；同时又确保与旧系统兼容，不破坏现有存档和玩法逻辑。 


