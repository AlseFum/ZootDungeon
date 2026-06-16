package com.zootdungeon.actors.entities.mines;

import com.watabou.noosa.TextureFilm;

/**
 * 十字地雷（连锁型）的视觉。
 * <p>
 * 十字地雷强调"连锁"感：放置时有旋转下落效果，待机时缓慢呼吸，
 * 引爆时强烈白光 + 强力震动。
 */
public class CrossMineSprite extends MineSprite {

    public CrossMineSprite() {
        super();
        String tex = "cola/trashbin.png";
        texture(tex);
        TextureFilm film = new TextureFilm(tex, 16, 16);

        // 缓慢待机呼吸
        idle = new Animation(1, true);
        idle.frames(film, 0);

        place = new Animation(4, false);
        place.frames(film, 0, 1, 2, 3);

        disarm = new Animation(1, false);
        disarm.frames(film, 3);

        detonate = new Animation(1, false);
        detonate.frames(film, 3);

        hardlight(0xFF9040);
    }

    @Override
    protected int baseColor() {
        return 0xFF9040;
    }

    @Override
    protected float shakeMagnitude() {
        return 10f;
    }

    @Override
    protected float detonateScaleTo() {
        return 3.5f;
    }
}
