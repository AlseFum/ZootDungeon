package com.zootdungeon.levels.themes.burningsun;

import com.zootdungeon.actors.mobs.Mob;
import com.zootdungeon.arknights.MainTheme.HourOfAnAwakening.HourOfAnAwakening;
import com.zootdungeon.levels.themes.Theme;

import java.util.ArrayList;

/**
 * 「烈日」主题 —— 焦土与脉冲烈焰区域遍布的灼热关卡。
 * <p>
 * 使用 {@link BurningSunLevel}，地面分布 {@link com.zootdungeon.actors.blobs.ScorchBlob}
 * 和 {@link com.zootdungeon.actors.blobs.PulseBlob}，怪物复用 {@link HourOfAnAwakening} 的轮换。
 */
public final class BurningSun {

    public static final Theme THEME = new Theme(
            BurningSunLevel.class,
            null,
            (depth, branch) -> (branch == 0 && depth >= 1 && depth <= 5) ? (short) 1 : (short) 0
    ) {
        @Override
        public ArrayList<Class<? extends Mob>> getMobRotation(int depth, int branch) {
            return HourOfAnAwakening.THEME.getMobRotation(depth, branch);
        }
    };

    public static void register() {
        Theme.registerTheme("BurningSun", THEME);
    }
}
