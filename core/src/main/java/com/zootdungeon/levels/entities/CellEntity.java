/*
 * CellEntity 体系。
 * 不打算扩展 {@link com.zootdungeon.actors.Char}：Char 的 ID、findChar、LOS 等机制
 * 都默认「一格一 Char 且阻挡移动」，若强行继承再做例外会扰动 Mob AI / 寻路的大量判定。
 * 这里选择直接扩展 {@link com.zootdungeon.actors.Actor}，与 Plant、Trap、Heap 一样由
 * {@link com.zootdungeon.levels.Level} 独立登记。
 */
package com.zootdungeon.levels.entities;

import com.watabou.utils.Bundle;
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.levels.Level;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.GameScene;

/**
 * 地面实体（Cell Entity）：
 * <p>
 * 行为与 {@link com.zootdungeon.plants.Plant} 相似（占一个格子、被踩时触发），
 * 但额外拥有自己的 {@link Actor} 调度，可以随时间跑 {@link #act()} 逻辑
 * （倒计时、周期性效果等）。
 * <p>
 * 约束：
 * <ul>
 *   <li>不占据格子：该位置的 {@link com.zootdungeon.actors.Char#blockSight()} /
 *       寻路 / {@link Actor#findChar(int)} 完全不受它影响；普通 Char 可以与之共存。</li>
 *   <li>渲染层位于地面图层之上、hero/mob 之下（见
 *       {@link com.zootdungeon.scenes.GameScene} 中 {@code cellEntities} group 的插入位置）。</li>
 *   <li>触发点：
 *     <ul>
 *       <li>任何 {@link Char} 移动后占据该格 → 调用 {@link #onStep(Char)}；</li>
 *       <li>若该 Char 此刻正在飞行 → 额外调用 {@link #onFlyOver(Char)}。</li>
 *     </ul>
 *     两个回调默认为空，实际作用完全由子类决定（父类不假定任何「必须执行」的效果）。</li>
 *   <li>每个格子最多登记一个 {@link CellEntity}，后放置者会替换掉旧实体。</li>
 * </ul>
 */
public abstract class CellEntity extends Actor {

    /** 所在格子的线性下标，与 {@link com.zootdungeon.plants.Plant#pos} 含义一致。 */
    public int pos;

    /**
     * 绑定的视觉 sprite（由 {@link GameScene#addCellEntitySprite} 填入）。
     * 实体被移除或场景切换时由 {@link GameScene#removeCellEntitySprite} 清空。
     * 可以为 null（例如 {@link #spriteClass()} 返回 null、或当前不在活动 GameScene 中）。
     */
    public CellEntitySprite sprite;

    {
        // 默认在 Blob / Mob 之后、Buff 之前行动，避免与关键角色抢 tick 顺序。
        actPriority = BLOB_PRIO - 1;
    }

    /**
     * 默认 act()：无衰减、无副作用，只推进自身时间避免把调度器饿死。
     * 子类可以覆写实现周期逻辑。
     */
    @Override
    protected boolean act() {
        spend(TICK);
        return true;
    }

    // ==== 触发回调 ====

    /**
     * 当任意 {@link Char} 进入所在格子（含飞行单位）时调用。默认空实现。
     * 这里称之为「踩中」只是为了术语统一，实际上飞行单位也会进入这里。
     */
    public void onStep(Char who) {
        // 默认什么都不做；子类按需覆写。
    }

    /**
     * 当一个飞行 {@link Char} 进入所在格子时额外调用一次（已先调用过 {@link #onStep(Char)}）。
     * 用来区分「飞过」与「踩到」两种语义。默认空实现。
     */
    public void onFlyOver(Char who) {
        // 默认什么都不做；子类按需覆写。
    }

    // ==== 生命周期钩子（由 Level 调用，非 Actor.onAdd/onRemove） ====

    /** 实体被 {@link Level#addCellEntity} 登记到关卡后。 */
    public void onSpawn(Level level) {}

    /** 实体被 {@link Level#removeCellEntity} 从关卡摘下前。 */
    public void onDespawn(Level level) {}

    /** 便捷方法：把自己从当前 Level 上摘掉（会触发 {@link #onDespawn} 与场景侧清理）。 */
    public void despawn() {
        if (Dungeon.level != null) {
            Dungeon.level.removeCellEntity(this);
        }
    }

    /** 便捷方法：查询当前踩在此格子上的 {@link Char}（若无返回 null）。 */
    protected Char charHere() {
        return Actor.findChar(pos);
    }

    // ==== 视觉 ====

    /**
     * 返回用于渲染的 Sprite 类。返回 null 表示完全不可见。
     * {@link GameScene#addCellEntitySprite(CellEntity)} 会据此实例化并挂到
     * 「地面之上、mob 之下」的专用渲染组。
     */
    public Class<? extends CellEntitySprite> spriteClass() {
        return null;
    }

    // ==== Bundle ====

    private static final String POS = "pos";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(POS, pos);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        pos = bundle.getInt(POS);
    }

    // ==== 文本 ====

    public String name() {
        return Messages.get(this, "name");
    }

    public String desc() {
        return Messages.get(this, "desc");
    }
}
