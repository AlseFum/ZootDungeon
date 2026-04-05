package com.zootdungeon.arknights.MainTheme.HourOfAnAwakening;

import com.zootdungeon.actors.mobs.Hound;
import com.zootdungeon.actors.mobs.Mob;
import com.zootdungeon.actors.mobs.Yokai;
import com.zootdungeon.arknights.Junkman;
import com.zootdungeon.arknights.OriginumSlug;

import com.zootdungeon.levels.themes.Theme;

import java.util.ArrayList;

public final class HourOfAnAwakening {

    public static final Theme THEME = new Theme(
            HourOfAnAwakeningLevel.class,
            HourOfAnAwakeningBossLevel.class,
            (depth, branch) -> (branch == 0 && depth >= 1 && depth <= 5) ? (short) 1: (short) 0
    ){
        @Override
        public void enterTheme() {
            // GLog.n("awc");
        }

        @Override
        public ArrayList<Class<? extends Mob>> getMobRotation(int depth, int branch) {
            ArrayList<Class<? extends Mob>> mobs = new ArrayList<>();
            mobs.add(Infantry.class);
            mobs.add(MobileShieldSoldier.class);
            mobs.add(Rebellist.class);
            mobs.add(OriginumSlug.class);
            mobs.add(Hound.class);
            mobs.add(Yokai.class);
            mobs.add(Junkman.class);
            return mobs;
        }
    };

    public static void register() {
        Theme.registerTheme("Arknights_HourOfAnAwakening", THEME);
    }
}
