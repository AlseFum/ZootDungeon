# CellEntity 地面实体架构指南

`CellEntity` 是 Cola Dungeon 在 Shattered Pixel Dungeon 原有的 `Plant` / `Trap` / `Heap` 体系之外，新增的一种**站在地面上、占一个格子、但不阻挡通行**的实体类型。

本文档说明它的设计初衷、接入点，以及扩展新 CellEntity 的典型流程。

---

## 1. 为什么新增 CellEntity

已有实体的能力：

| 类型 | 有 Actor 调度? | 有独立 sprite? | 占格子（挡路）? | 踩中触发? | 飞行触发? |
|---|---|---|---|---|---|
| `Plant` | 否（无 `act`） | 是（贴在 tile 上） | 否 | 是 | 否 |
| `Trap` | 否 | 是 | 否 | 是 | 否 |
| `Heap` | 否 | 是 | 否 | 否 | 否 |
| `Char` / `Mob` | 是 | 是 | **是**（`findChar`） | — | — |
| `Blob` | 是 | 是（全地图） | 否 | 否 | 否 |

而项目中需要的一类新物件同时满足：

- 会随时间自己跑逻辑（需要 `Actor.act()`）；
- 有自己的贴图；
- **不** 占格子、不干扰寻路、不进 `findChar`；
- 渲染层在地面之上、`hero/mob` 之下；
- 既关心「被踩到」也关心「被飞越」两种语义。

这套需求用 `Char` 子类实现代价很高：`Char` 的 ID 分配、`findChar` 寻路假设「一格最多一个 Char」、AI 的 `canAttack` / LOS 判定都会被污染。因此我们单独扩展 `Actor`，并由 `Level` 独立登记，得到 `CellEntity`。

---

## 2. 关键类

### 2.1 `com.zootdungeon.levels.entities.CellEntity`

抽象基类，继承自 `Actor`。对外暴露的核心 API：

```31:40:core/src/main/java/com/zootdungeon/levels/entities/CellEntity.java
 *     </ul>
 *     两个回调默认为空，实际作用完全由子类决定（父类不假定任何「必须执行」的效果）。</li>
 *   <li>每个格子最多登记一个 {@link CellEntity}，后放置者会替换掉旧实体。</li>
 * </ul>
 */
public abstract class CellEntity extends Actor {

    /** 所在格子的线性下标，与 {@link com.zootdungeon.plants.Plant#pos} 含义一致。 */
    public int pos;
```

可覆写的钩子：

- `act()`：默认 `spend(TICK); return true;`，即「每一 tick 占一格时间不做事」。需要周期行为时覆写。
- `onStep(Char who)`：任意 Char 进入该格时调用（含飞行单位）。
- `onFlyOver(Char who)`：飞行 Char 进入该格时**额外**再调用一次。
- `onSpawn(Level)` / `onDespawn(Level)`：被 `Level` 登记/摘下时的生命周期钩子。
- `spriteClass()`：返回 `CellEntitySprite` 子类，用于 `GameScene` 实例化贴图；返回 `null` 表示不可见。
- `name()` / `desc()`：默认通过 `Messages.get(this, "name"/"desc")` 走本地化。

`storeInBundle` / `restoreFromBundle` 已包含 `pos`，子类覆写时记得 `super.xxx(bundle)`。

### 2.2 `CellEntitySprite`

继承自 `com.watabou.noosa.Image`，提供：

- `link(CellEntity)` — 绑定并 `place()` 到对应格子；
- `place(int cell)` — 按 `DungeonTilemap.SIZE` 计算屏幕坐标；
- `reset()` / `fadeIn()` — 常用的出生动画；
- `entity()` — 拿回绑定的数据实体。

子类通常只需在构造函数里 `texture("cola/xxx.png")` 或 `copy(...)` 配好贴图即可。

### 2.3 `Level` 的登记接口

`Level` 中有三个公开方法：

- `addCellEntity(CellEntity entity, int pos)`：设置位置、登记到 `SparseArray<CellEntity>`、加入 Actor 调度、回调 `onSpawn`、调用 `GameScene.addCellEntitySprite`；若该格已有旧实体，先将旧实体 `removeCellEntity` 掉。
- `removeCellEntity(CellEntity entity)`：反向操作，含 `entity.pos` 被外部改过时的兜底查找。
- `cellEntityAt(int pos)`：按格子查询。

Bundle 持久化：`storeInBundle` 存 `cellEntities.valueList()`，`restoreFromBundle` 再重建 `SparseArray`。

### 2.4 `Actor` 调度接入

`Actor.init()` 在加载/生成关卡时会遍历 `Dungeon.level.cellEntities`，把每个 `CellEntity` 加入 Actor 调度队列，这样读档后 `act()` 会继续跑。

### 2.5 `GameScene` 渲染接入

`GameScene` 里新增了一个 `cellEntities` `Group`，插入顺序为 **`heaps` 之后、`mobs` 之前**，保证：

- 在地面装饰、掉落物之上（不会被地面盖住）；
- 在 Hero / Mob 之下（Char 走过来时会挡住它，符合「地面上的东西」视觉）。

对外两个静态代理方法：

- `GameScene.addCellEntitySprite(entity)`：内部用 `Reflection.newInstance(entity.spriteClass())` 构造 sprite → `link(entity)` → 加入 `cellEntities` group → 回写 `entity.sprite`；`scene` 未就绪时安全 no-op。
- `GameScene.removeCellEntitySprite(entity)`：`entity.sprite.killAndErase()` + 清引用。

### 2.6 触发点 `Level.occupyCell`

```1270:1281:core/src/main/java/com/zootdungeon/levels/Level.java
		// CellEntity 触发：飞行 / 非飞行单位都会调用 onStep；飞行单位另外调用 onFlyOver。
		// 放在最后，确保水、草、陷阱等地块效果已经先处理过。
		if (ch.isAlive()) {
			CellEntity entity = cellEntities.get( ch.pos );
			if (entity != null) {
				entity.onStep( ch );
				if (ch.flying) {
					entity.onFlyOver( ch );
				}
			}
		}
```

> 注意：`onStep` 对**所有**进入该格的 Char 都触发；`onFlyOver` 只针对当下正在飞行的 Char 额外追加一次调用。若 CellEntity 只想影响非飞行目标，自行在 `onStep` 里 `if (!who.flying)` 过滤即可。

---

## 3. 完整扩展流程

以仓库里内置的 `DebugCellMarker` 为例，完整步骤如下：

### (1) 写数据类（继承 `CellEntity`）

```java
public class DebugCellMarker extends CellEntity {

    public int stepCount = 0;
    public int flyCount = 0;

    @Override
    public Class<? extends CellEntitySprite> spriteClass() {
        return DebugCellMarkerSprite.class;
    }

    @Override
    public void onStep(Char who) {
        stepCount++;
        GLog.i(Messages.get(this, "step_hero", who.name(), stepCount));
    }

    @Override
    public void onFlyOver(Char who) {
        flyCount++;
        GLog.i(Messages.get(this, "fly_over", who.name(), flyCount));
    }

    private static final String STEP_COUNT = "stepCount";
    private static final String FLY_COUNT = "flyCount";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(STEP_COUNT, stepCount);
        bundle.put(FLY_COUNT, flyCount);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        stepCount = bundle.getInt(STEP_COUNT);
        flyCount = bundle.getInt(FLY_COUNT);
    }
}
```

要点：

- 所有运行时状态都要 `storeInBundle` / `restoreFromBundle` 存盘，父类只帮你存 `pos`。
- 不要覆写 `Actor.onAdd/onRemove` 处理 spawn/despawn，请使用 `onSpawn(Level)` / `onDespawn(Level)`，那才是 `Level` 真正调用的时机。
- 若只想读懂当前踩在该格上的 Char，用父类里的 `charHere()` 便捷方法（内部调用 `Actor.findChar(pos)`）。

### (2) 写贴图类（继承 `CellEntitySprite`）

```java
public class DebugCellMarkerSprite extends CellEntitySprite {
    public DebugCellMarkerSprite() {
        super();
        texture("cola/trashbin.png");
    }
}
```

需要动画或多帧时，可在构造函数里补 `frame(...)`，或覆写 `update()` 做逐帧动画。`perspectiveRaise` 字段控制贴图相对地面抬升的像素高度（默认 `5/16`）。

### (3) 注册本地化

在 `core/src/main/assets/messages/items/items.properties`（英文）和 `items_zh.properties`（中文）里：

```
levels.entities.debugcellmarker.name=Debug Cell Marker
levels.entities.debugcellmarker.desc=...
levels.entities.debugcellmarker.step_hero=...
```

Key 前缀来自 `Messages.get(this, ...)` 对类的全限定名转成小写后的结果。新建 `CellEntity` 子类时建议都走 `Messages.get`，不要硬编码中英文。

### (4) 把它放到地图上

在任何能拿到 `Dungeon.level` 的地方：

```java
DebugCellMarker marker = new DebugCellMarker();
Dungeon.level.addCellEntity(marker, cellPos);
```

就会自动完成：

1. 登记到 `Level.cellEntities`；
2. 加入 `Actor` 调度（可跑 `act`）；
3. 回调 `onSpawn`；
4. 生成 sprite 并挂到场景 `cellEntities` group 里。

移除反之用 `level.removeCellEntity(marker)` 或 `marker.despawn()`。

---

## 4. 调试入口：`CellEntityPlacer` + DebugSupply

仓库里内置了一个调试用的放置器：

- `com.zootdungeon.items.cheat.CellEntityPlacer`：
  - **Place**（默认动作）：点一个格子，往上面放一个 `DebugCellMarker`。
  - **Remove**：点一个格子，移除其上的 `CellEntity`。
  - **Inspect**：点一个格子，向日志打印该格 `CellEntity` 的名字、类名和 `desc()`。
- 进入方式：拿到 `DebugSupply` → 打开 → **Cell Entities** 标签页 → 选中 `Cell Entity Placer`。

对应 DebugSupply 侧的布线：

- `CAT_CELL_ENTITIES = "cat_cell_entities"` 作为新 tab；
- tab 标签文本：`items.supply.debugsupply.cat_cell_entities`；
- 内容只有一项 `CellEntityPlacer`（后续要加更多调试工具都追加到同一 tab 即可）。

> 标签页过多时 `WndGeneral` 会自动把按钮折成最多 **3 行**，因此 `DebugSupply` 现在即便新增更多 tab 也能正常布局，无需手动缩短按钮文字。

---

## 5. 常见陷阱

1. **别忘 `super.storeInBundle(bundle)`。** 不调父类就不会存 `pos`，读档后实体会出现在格子 0。
2. **`entity.pos` 不要随便改。** 真要「把实体从 A 移到 B」，请用 `level.removeCellEntity(e); e = new ...; level.addCellEntity(e, B);`，因为 `SparseArray` 的 key 是登记瞬间的 `pos`，外部改 `pos` 后 `removeCellEntity` 需要走遍历兜底。
3. **不要继承 `Char`。** 已经解释过原因。如果未来确实需要某个 CellEntity 能被攻击/有 HP，建议在 CellEntity 上组合 `Buff` / 自定义 `HP` 字段，而不是改继承树。
4. **Sprite 不要自己 add 到 `GameScene`。** 统一走 `GameScene.addCellEntitySprite(entity)`，这样图层顺序 & scene 重建时的清理才会对。
5. **飞行单位的触发是两次回调。** 先 `onStep`、再 `onFlyOver`；别在 `onStep` 里再判一次 `flying` 然后又做了一遍副作用。

---

## 6. 相关文件速查

| 路径 | 作用 |
|---|---|
| `core/.../levels/entities/CellEntity.java` | 基类 |
| `core/.../levels/entities/CellEntitySprite.java` | 贴图基类 |
| `core/.../levels/entities/DebugCellMarker.java` | 内置示例实体 |
| `core/.../levels/entities/DebugCellMarkerSprite.java` | 内置示例贴图 |
| `core/.../levels/Level.java` | `cellEntities` 存储、`add/remove/cellEntityAt`、`occupyCell` 触发 |
| `core/.../actors/Actor.java` | `init()` 里把 CellEntity 加入 Actor 调度 |
| `core/.../scenes/GameScene.java` | `cellEntities` 渲染 group + 静态代理方法 |
| `core/.../items/cheat/CellEntityPlacer.java` | 调试放置器 |
| `core/.../items/supply/DebugSupply.java` | Debug 物资包入口（`CAT_CELL_ENTITIES` tab） |
