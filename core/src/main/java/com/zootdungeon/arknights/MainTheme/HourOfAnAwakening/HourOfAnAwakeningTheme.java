package com.zootdungeon.arknights.MainTheme.HourOfAnAwakening;

import com.zootdungeon.levels.themes.Theme;
import com.zootdungeon.utils.GLog;

/**
 * 「醒时」主题：Arknights MainTheme Hour of an Awakening.
 * 默认可用深度 1–5（与下水道区间重叠，权重相同时随机二选一）。
 */
public final class HourOfAnAwakeningTheme {

    public static final Theme THEME = new Theme(
            HourOfAnAwakeningLevel.class,
            HourOfAnAwakeningBossLevel.class,
            (depth, branch) -> (branch == 0 && depth >= 1 && depth <= 5) ? (short) 12 : (short) 0
    ){
        @Override
        public void enterTheme() {
            GLog.n("awc");
        }
    };

    public static void register() {
        Theme.registerTheme("Arknights_HourOfAnAwakening", THEME);
    }
}
