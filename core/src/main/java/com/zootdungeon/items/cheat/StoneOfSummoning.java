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

/**
 * Debug runestone which lets the player spawn a monster at a chosen cell.
 *
 * Use the special action to cycle through a small curated list of mobs,
 * then throw the stone and a copy of that mob will appear where it lands.
 */
public class StoneOfSummoning extends Runestone {

    private static final String AC_SELECT_MOB = "SELECT_MOB";

    private static final String BUNDLE_INDEX = "mob_index";

    @SuppressWarnings("unchecked")
    private static final Class<? extends Mob>[] MOB_TYPES = new Class[]{
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

    private int mobIndex = 0;

    {
        image = ItemSpriteSheet.STONE_AGGRESSION;
    }

    @Override
    public ArrayList<String> actions(Hero hero) {
        ArrayList<String> actions = super.actions(hero);
        actions.add(AC_SELECT_MOB);
        return actions;
    }

    @Override
    public void execute(Hero hero, String action) {
        super.execute(hero, action);

        if (AC_SELECT_MOB.equals(action)) {
            cycleMob();
            updateQuickslot();
        }
    }

    private void cycleMob() {
        mobIndex = (mobIndex + 1) % MOB_TYPES.length;
        GLog.p("当前选择召唤: " + getCurrentMobName());
    }

    private String getCurrentMobName() {
        Class<? extends Mob> cls = MOB_TYPES[mobIndex];
        try {
            Mob m = Reflection.newInstance(cls);
            return m != null ? m.name() : cls.getSimpleName();
        } catch (Throwable t) {
            return cls.getSimpleName();
        }
    }

    @Override
    protected void activate(int cell) {
        if (cell < 0) return;

        Class<? extends Mob> cls = MOB_TYPES[mobIndex];
        Mob mob;
        try {
            mob = Reflection.newInstance(cls);
        } catch (Throwable t) {
            GLog.w("无法生成该怪物。");
            return;
        }

        if (mob == null) {
            GLog.w("无法生成该怪物。");
            return;
        }

        mob.pos = cell;
        mob.state = mob.PASSIVE;
        GameScene.add(mob);
        GLog.p("生成了: " + mob.name());
    }

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(BUNDLE_INDEX, mobIndex);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        mobIndex = bundle.getInt(BUNDLE_INDEX);
        if (mobIndex < 0 || mobIndex >= MOB_TYPES.length) {
            mobIndex = 0;
        }
    }

    @Override
    public String name() {
        return "召唤魔石";
    }

    @Override
    public String desc() {
        return "调试用符文石，可以在指定位置召唤怪物。\n"
                + "使用特殊动作切换要召唤的怪物类型，然后像普通符文石一样丢掷到地图上。";
    }
}


