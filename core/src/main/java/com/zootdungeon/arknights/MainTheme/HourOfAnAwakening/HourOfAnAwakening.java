package com.zootdungeon.arknights.MainTheme.HourOfAnAwakening;

import com.zootdungeon.actors.mobs.Hound;
import com.zootdungeon.actors.mobs.Mob;
import com.zootdungeon.actors.mobs.Yokai;
import com.zootdungeon.arknights.Junkman;
import com.zootdungeon.arknights.OriginumSlug;

import com.zootdungeon.levels.themes.Theme;

import java.util.ArrayList;
// WE SHOULD REORGANIZE THE THEME
public final class HourOfAnAwakening {

    public static final Theme THEME = new Theme(
            HourOfAnAwakeningLevel.class,
            null,
            (depth, branch) -> (branch == 0 && depth >= 1 && depth <= 5) ? (short) 0: (short) 0
    ){
        @Override
        public void enterTheme() {
        }

        @Override
        public ArrayList<Class<? extends Mob>> getMobRotation(int depth, int branch) {
            ArrayList<Class<? extends Mob>> mobs = new ArrayList<>();
            mobs.add(Infantry.class);
            mobs.add(OriginumSlug.class);
            mobs.add(AgileOriginumSlug.class);
            return mobs;
        }
    };

    public static void register() {
        Theme.registerTheme("Arknights_HourOfAnAwakening", THEME);
    }
}
