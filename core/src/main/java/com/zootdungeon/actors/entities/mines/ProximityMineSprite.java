package com.zootdungeon.actors.entities.mines;

import com.watabou.noosa.TextureFilm;

/**
 * 感应（闪光）地雷的视觉。
 * <p>
 * 感应地雷强调"警戒"感：放置后持续上下颤动（快速循环待机），
 * 引爆时发出强烈黄光，无屏幕震动。
 */
public class ProximityMineSprite extends MineSprite {

    public ProximityMineSprite() {
        super();
        String tex = "cola/trashbin.png";
        texture(tex);
        TextureFilm film = new TextureFilm(tex, 16, 16);

        // 快速待机循环：上下颤动
        idle = new Animation(6, true);
        idle.frames(film, 0, 1, 0, 2);

        place = new Animation(4, false);
        place.frames(film, 0, 0, 1, 2);

        disarm = new Animation(1, false);
        disarm.frames(film, 3);

        detonate = new Animation(1, false);
        detonate.frames(film, 3);

        hardlight(0xFFFF50);
    }

    @Override
    protected int baseColor() {
        return 0xFFFF50;
    }

    @Override
    protected float shakeMagnitude() {
        return 0f;
    }

    @Override
    protected int detonateColor() {
        return 0xFFFF88;
    }
}
