package com.zootdungeon.arknights.MainTheme.HourOfAnAwakening;

import com.zootdungeon.actors.mobs.Mob;
import com.zootdungeon.arknights.OriginumSlug;
import com.watabou.utils.Random;
import com.zootdungeon.levels.themes.Theme;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * 「破晓时分」主题 — 仿 SewerTheme 的关卡结构，使用整合运动敌人。
 */
public final class HourOfAnAwakening {

    public static final Theme THEME = new Theme(
            HourOfAnAwakeningLevel.class,
            HourOfAnAwakeningBossLevel.class,
            (depth, branch) -> (branch == 0 && depth >= 1 && depth <= 5) ? (short) 1 : (short) 0
    ) {
        @Override
        public ArrayList<Class<? extends Mob>> getMobRotation(int depth, int branch) {
            ArrayList<Class<? extends Mob>> mobs;
            if (depth == 1) {
                mobs = new ArrayList<>(Arrays.asList(
                        OriginumSlug.class, OriginumSlug.class, OriginumSlug.class,
                        Infantry.class));
            } else if (depth == 2) {
                mobs = new ArrayList<>(Arrays.asList(
                        OriginumSlug.class, OriginumSlug.class,
                        Infantry.class, Infantry.class,
                        AgileOriginumSlug.class));
            } else if (depth == 3) {
                mobs = new ArrayList<>(Arrays.asList(
                        OriginumSlug.class, Infantry.class, Infantry.class,
                        Infantry.class, AgileOriginumSlug.class));
            } else {
                mobs = new ArrayList<>(Arrays.asList(
                        Infantry.class, Infantry.class,
                        Infantry.class, Infantry.class,
                        AgileOriginumSlug.class, AgileOriginumSlug.class));
            }
            addRareMobs(depth, mobs);
            swapMobAlts(mobs);
            Random.shuffle(mobs);
            return mobs;
        }
    };

    public static void register() {
        Theme.registerTheme("Arknights_HourOfAnAwakening", THEME);
    }
}
