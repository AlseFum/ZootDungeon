package com.zootdungeon.actors.entities.mines;

import com.watabou.noosa.TextureFilm;

/**
 * 接触地雷的视觉。
 * <p>
 * 接触地雷强调瞬时爆发感：放置后快速待机循环，引爆时快速闪白放大。
 */
public class ContactMineSprite extends MineSprite {

    public ContactMineSprite() {
        super();
        String tex = "cola/trashbin.png";
        texture(tex);
        TextureFilm film = new TextureFilm(tex, 16, 16);

        idle = new Animation(1, true);
        idle.frames(film, 0);

        place = new Animation(4, false);
        place.frames(film, 0, 0, 1, 2);

        disarm = new Animation(1, false);
        disarm.frames(film, 3);

        detonate = new Animation(1, false);
        detonate.frames(film, 3);

        hardlight(0xFF5050);
    }

    @Override
    protected int baseColor() {
        return 0xFF5050;
    }

    @Override
    protected float shakeMagnitude() {
        return 8f;
    }
}
