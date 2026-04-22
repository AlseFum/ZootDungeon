package com.zootdungeon.items.cheat;

import java.util.ArrayList;

import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.buffs.ExplosiveStackBuff;
import com.zootdungeon.actors.buffs.ExplosiveStackBuff.ExplosiveStackCooldown;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.items.Item;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.CellSelector;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.sprites.SpriteRegistry;
import com.zootdungeon.utils.GLog;

/**
 * 调试工具：驱动 {@link ExplosiveStackBuff} + {@link ExplosiveStackCooldown} 的完整生命周期。
 * <ul>
 *   <li>PLUS_SELF / PLUS_CELL：给自己或指定格子上的单位加 1 层。</li>
 *   <li>BURST_CELL：一次性把目标叠满（threshold 层）触发爆发。</li>
 *   <li>CLEAR_CELL：移除目标身上的叠层 buff 和冷却 buff。</li>
 *   <li>INSPECT_CELL：打印目标当前叠层数与冷却剩余回合。</li>
 * </ul>
 * 所有对外文本都走 {@link Messages}。
 */
public class StackingBuffTester extends Item {

    public static final String AC_PLUS_SELF = "PLUS_SELF";
    public static final String AC_PLUS_CELL = "PLUS_CELL";
    public static final String AC_BURST_CELL = "BURST_CELL";
    public static final String AC_CLEAR_CELL = "CLEAR_CELL";
    public static final String AC_INSPECT_CELL = "INSPECT_CELL";

    {
        image = SpriteRegistry.byLabel("debug_bag");
        stackable = false;
        unique = true;
        defaultAction = AC_PLUS_CELL;
    }

    @Override
    public ArrayList<String> actions(Hero hero) {
        ArrayList<String> actions = super.actions(hero);
        actions.add(AC_PLUS_SELF);
        actions.add(AC_PLUS_CELL);
        actions.add(AC_BURST_CELL);
        actions.add(AC_CLEAR_CELL);
        actions.add(AC_INSPECT_CELL);
        return actions;
    }

    @Override
    public String actionName(String action, Hero hero) {
        if (action.equals(AC_PLUS_SELF))    return Messages.get(this, "ac_plus_self");
        if (action.equals(AC_PLUS_CELL))    return Messages.get(this, "ac_plus_cell");
        if (action.equals(AC_BURST_CELL))   return Messages.get(this, "ac_burst_cell");
        if (action.equals(AC_CLEAR_CELL))   return Messages.get(this, "ac_clear_cell");
        if (action.equals(AC_INSPECT_CELL)) return Messages.get(this, "ac_inspect_cell");
        return super.actionName(action, hero);
    }

    @Override
    public void execute(Hero hero, String action) {
        super.execute(hero, action);
        if (action.equals(AC_PLUS_SELF)) {
            addOne(hero);
        } else if (action.equals(AC_PLUS_CELL)) {
            GameScene.selectCell(plusCellTarget);
        } else if (action.equals(AC_BURST_CELL)) {
            GameScene.selectCell(burstCellTarget);
        } else if (action.equals(AC_CLEAR_CELL)) {
            GameScene.selectCell(clearCellTarget);
        } else if (action.equals(AC_INSPECT_CELL)) {
            GameScene.selectCell(inspectCellTarget);
        }
    }

    @Override
    public String name() {
        return Messages.get(this, "name");
    }

    @Override
    public String desc() {
        return Messages.get(this, "desc");
    }

    @Override
    public boolean isUpgradable() { return false; }

    @Override
    public boolean isIdentified() { return true; }

    // ==== helpers ====

    private void addOne(Char ch) {
        if (ch == null || !ch.isAlive()) {
            GLog.w(Messages.get(StackingBuffTester.class, "no_target"));
            return;
        }
        ExplosiveStackBuff buff = Buff.affect(ch, ExplosiveStackBuff.class);
        boolean onCd = buff.isOnCooldown();
        int before = buff.stacks();
        int applied = buff.tryAddStacks(1);
        int after = buff.stacks();

        if (onCd && applied == 0) {
            GLog.i(Messages.get(StackingBuffTester.class, "blocked", ch.name()));
        } else if (after == 0 && before + 1 >= buff.threshold()) {
            GLog.p(Messages.get(StackingBuffTester.class, "triggered", ch.name()));
        } else {
            GLog.i(Messages.get(StackingBuffTester.class, "added", ch.name(), after, buff.threshold()));
        }
    }

    private static Char charAt(Integer cell) {
        if (cell == null || cell < 0) return null;
        return Actor.findChar(cell);
    }

    private final CellSelector.Listener plusCellTarget = new CellSelector.Listener() {
        @Override public void onSelect(Integer cell) { addOne(charAt(cell)); }
        @Override public String prompt() { return Messages.get(StackingBuffTester.class, "prompt_plus"); }
    };

    private final CellSelector.Listener burstCellTarget = new CellSelector.Listener() {
        @Override
        public void onSelect(Integer cell) {
            Char ch = charAt(cell);
            if (ch == null || !ch.isAlive()) {
                GLog.w(Messages.get(StackingBuffTester.class, "no_target"));
                return;
            }
            ExplosiveStackBuff buff = Buff.affect(ch, ExplosiveStackBuff.class);
            if (buff.isOnCooldown()) {
                GLog.i(Messages.get(StackingBuffTester.class, "blocked", ch.name()));
                buff.tryAddStacks(buff.threshold());
                return;
            }
            int fill = Math.max(1, buff.threshold() - buff.stacks());
            buff.tryAddStacks(fill);
            GLog.p(Messages.get(StackingBuffTester.class, "burst_forced", ch.name()));
        }
        @Override public String prompt() { return Messages.get(StackingBuffTester.class, "prompt_burst"); }
    };

    private final CellSelector.Listener clearCellTarget = new CellSelector.Listener() {
        @Override
        public void onSelect(Integer cell) {
            Char ch = charAt(cell);
            if (ch == null) {
                GLog.w(Messages.get(StackingBuffTester.class, "no_target"));
                return;
            }
            int cleared = 0;
            ExplosiveStackBuff stack = ch.buff(ExplosiveStackBuff.class);
            if (stack != null) { stack.detach(); cleared++; }
            ExplosiveStackCooldown cd = ch.buff(ExplosiveStackCooldown.class);
            if (cd != null) { cd.detach(); cleared++; }
            GLog.i(Messages.get(StackingBuffTester.class, "cleared", ch.name(), cleared));
        }
        @Override public String prompt() { return Messages.get(StackingBuffTester.class, "prompt_clear"); }
    };

    private final CellSelector.Listener inspectCellTarget = new CellSelector.Listener() {
        @Override
        public void onSelect(Integer cell) {
            Char ch = charAt(cell);
            if (ch == null) {
                GLog.i(Messages.get(StackingBuffTester.class, "inspect_empty"));
                return;
            }
            ExplosiveStackBuff stack = ch.buff(ExplosiveStackBuff.class);
            ExplosiveStackCooldown cd = ch.buff(ExplosiveStackCooldown.class);
            int stacks = stack != null ? stack.stacks() : 0;
            int threshold = stack != null ? stack.threshold() : 0;
            int cdLeft = cd != null ? (int) Math.ceil(cd.visualcooldown()) : 0;
            GLog.i(Messages.get(StackingBuffTester.class, "inspect_hit",
                    ch.name(), stacks, threshold, cdLeft));
        }
        @Override public String prompt() { return Messages.get(StackingBuffTester.class, "prompt_inspect"); }
    };
}
