package com.zootdungeon.levels.entities.mines;

import com.zootdungeon.levels.entities.CellEntitySprite;

/**
 * 地雷族的视觉基类。
 * <p>
 * 当前项目没有独立的地雷美术资源，这里统一复用 {@code cola/trashbin.png} 作为 16×16
 * 占位贴图；各子类通过 {@link #hardlight(int)} 着色以示区分（红/黄/橙/青等）。
 * 未来换成真实贴图时，只要改这里或子类的 {@code texture(...)} 即可，
 * 数据层 {@link Mine} 不受影响。
 */
public class MineSprite extends CellEntitySprite {

    public MineSprite() {
        super();
        texture("cola/trashbin.png");
    }

    /** 子类可覆盖这个颜色常量以改变着色，避免额外 API。 */
    public int tint() {
        return 0xFFFFFF;
    }

    @Override
    public void reset() {
        super.reset();
        int color = tint();
        if (color != 0xFFFFFF) {
            hardlight(color);
        } else {
            resetColor();
        }
    }
}
