package com.zootdungeon.actors.entities.mines;

import com.watabou.noosa.TextureFilm;

/**
 * 遥控地雷的视觉。
 * <p>
 * 遥控地雷强调"待命"感：放置后完全静态，引爆时橙色闪白，无屏幕震动。
 */
public class RemoteMineSprite extends MineSprite {

    public RemoteMineSprite() {
        super();
        String tex = "cola/trashbin.png";
        texture(tex);
        TextureFilm film = new TextureFilm(tex, 16, 16);

        idle = new Animation(1, true);
        idle.frames(film, 0);

        place = new Animation(4, false);
        place.frames(film, 0, 1, 0, 2);

        disarm = new Animation(1, false);
        disarm.frames(film, 3);

        detonate = new Animation(1, false);
        detonate.frames(film, 3);

        hardlight(0x60FFFF);
    }

    @Override
    protected int baseColor() {
        return 0x60FFFF;
    }

    @Override
    protected float shakeMagnitude() {
        return 4f;
    }

    @Override
    protected int detonateColor() {
        return 0xFF9040;
    }
}
