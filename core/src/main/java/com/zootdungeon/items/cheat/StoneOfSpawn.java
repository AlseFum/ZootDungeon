package com.zootdungeon.items.cheat;

import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.actors.mobs.Bat;
import com.zootdungeon.actors.mobs.Brute;
import com.zootdungeon.actors.mobs.Crab;
import com.zootdungeon.actors.mobs.DM300;
import com.zootdungeon.actors.mobs.FetidRat;
import com.zootdungeon.actors.mobs.GreatCrab;
import com.zootdungeon.actors.mobs.Mob;
import com.zootdungeon.actors.mobs.Rat;
import com.zootdungeon.actors.mobs.Skeleton;
import com.zootdungeon.actors.mobs.Slime;
import com.zootdungeon.actors.mobs.Succubus;
import com.zootdungeon.actors.mobs.Warlock;
import com.zootdungeon.items.stones.Runestone;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.zootdungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

public class StoneOfSpawn extends Runestone {

    public static final String AC_SELECT_TARGET = "SELECT_TARGET";
    public static final String BUNDLE_INDEX = "spawn_index";
    public static final int DUMMY_HP = 114514;

    @SuppressWarnings("unchecked")
    public static final Class<? extends Mob>[] MOB_TYPES = new Class[]{
            Rat.class,
            FetidRat.class,
            Bat.class,
            Slime.class,
            Crab.class,
            GreatCrab.class,
            Brute.class,
            Skeleton.class,
            Warlock.class,
            Succubus.class,
            DM300.class
    };

    public int spawnIndex = -1; // -1 means dummy target.

    {
        image = ItemSpriteSheet.STONE_AGGRESSION;
    }

    @Override
    public ArrayList<String> actions(Hero hero) {
        ArrayList<String> actions = super.actions(hero);
        actions.add(AC_SELECT_TARGET);
        return actions;
    }

    @Override
    public String actionName(String action, Hero hero) {
        if (AC_SELECT_TARGET.equals(action)) {
            return "切换召唤目标";
        }
        return super.actionName(action, hero);
    }

    @Override
    public void execute(Hero hero, String action) {
        super.execute(hero, action);
        if (AC_SELECT_TARGET.equals(action)) {
            cycleTarget();
            updateQuickslot();
        }
    }

    public void cycleTarget() {
        spawnIndex++;
        if (spawnIndex >= MOB_TYPES.length) {
            spawnIndex = -1;
        }
        GLog.p("当前选择召唤: " + currentTargetName());
    }

    public String currentTargetName() {
        if (spawnIndex < 0) {
            return "Dummy老鼠";
        }
        Class<? extends Mob> cls = MOB_TYPES[spawnIndex];
        try {
            Mob mob = Reflection.newInstance(cls);
            return mob != null ? mob.name() : cls.getSimpleName();
        } catch (Throwable t) {
            return cls.getSimpleName();
        }
    }

    @Override
    protected void activate(int cell) {
        if (cell < 0) return;

        Mob mob = spawnIndex < 0 ? createDummy() : createMob(MOB_TYPES[spawnIndex]);
        if (mob == null) {
            GLog.w("无法生成该怪物。");
            return;
        }

        mob.pos = cell;
        mob.state = mob.PASSIVE;
        GameScene.add(mob);
        GLog.p("生成了: " + mob.name());
    }

    public Mob createMob(Class<? extends Mob> cls) {
        try {
            return Reflection.newInstance(cls);
        } catch (Throwable t) {
            return null;
        }
    }

    public Mob createDummy() {
        return new Rat() {
            @Override
            protected boolean act() {
                spend(TICK);
                return true;
            }
            {
                defenseSkill = 0;
                HT = DUMMY_HP;
                HP = DUMMY_HP;
            }
        };
    }

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(BUNDLE_INDEX, spawnIndex);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        spawnIndex = bundle.getInt(BUNDLE_INDEX);
        if (spawnIndex < -1 || spawnIndex >= MOB_TYPES.length) {
            spawnIndex = -1;
        }
    }

    @Override
    public String name() {
        return "召唤魔石";
    }

    @Override
    public String desc() {
        return "调试用符文石，可在指定位置召唤怪物或Dummy老鼠。"
                + "\n使用特殊动作切换召唤目标，然后像普通符文石一样丢掷到地图上。";
    }
}
