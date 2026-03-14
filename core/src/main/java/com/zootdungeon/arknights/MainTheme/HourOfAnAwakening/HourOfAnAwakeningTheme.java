package com.zootdungeon.arknights.MainTheme.HourOfAnAwakening;

import com.zootdungeon.actors.mobs.Mob;
import com.zootdungeon.arknights.MainTheme.HourOfAnAwakening.Infantry;
import com.zootdungeon.levels.themes.Theme;
import com.zootdungeon.utils.GLog;

import java.util.ArrayList;

/**
 * 「醒时」主题：Arknights MainTheme Hour of an Awakening.
 * 默认可用深度 1–5（与下水道区间重叠，权重相同时随机二选一）。
 * 本主题不生成粘液怪（Slime）。
 */
public final class HourOfAnAwakeningTheme {

    public static final Theme THEME = new Theme(
            HourOfAnAwakeningLevel.class,
            HourOfAnAwakeningBossLevel.class,
            (depth, branch) -> (branch == 0 && depth >= 1 && depth <= 5) ? (short) 144: (short) 0
    ){
        @Override
        public void enterTheme() {
            GLog.n("awc");
        }

        @Override
        public ArrayList<Class<? extends Mob>> getMobRotation(int depth, int branch) {
            ArrayList<Class<? extends Mob>> mobs = new ArrayList<>();
            mobs.add(Infantry.class);
            return mobs;
        }
    };

    public static void register() {
        Theme.registerTheme("Arknights_HourOfAnAwakening", THEME);
    }
}
