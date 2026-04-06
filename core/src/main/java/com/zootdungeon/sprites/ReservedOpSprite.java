package com.zootdungeon.sprites;

import com.zootdungeon.Assets;
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.hero.HeroClass;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.TextureFilm;
import com.watabou.utils.Callback;

public class ReservedOpSprite extends HeroSprite {

    private Animation fly;

    public ReservedOpSprite() {
        super();
        // Override the texture and animations after parent constructor
        HeroClass cls = Dungeon.hero != null ? Dungeon.hero.heroClass : HeroClass.WARRIOR;
        SpriteRegistry.texture("hero." + cls.name(), cls.spritesheet());
        Object handle = SpriteRegistry.the("hero." + cls.name()).textureHandle();
        Object resolved = handle instanceof String ? Assets.getTexture((String) handle) : handle;
        texture(TextureCache.get(resolved));
        updateArmor();
    }

    @Override
    public void updateArmor() {
        TextureFilm film = new TextureFilm(texture, 23, 22);

        idle = new Animation(5, true);
        idle.frames(film, 0, 1, 0, 2);

        run = new Animation(15, true);
        run.frames(film, 2,3, 4, 5, 6,7);

        attack = new Animation(12, false);
        attack.frames(film, 14,15,16,17,18,19);

        die = new Animation(12, false);
        die.frames(film, 8,9,10, 11, 12, 13);

        zap = attack.clone();

        operate = new Animation(8, false);
        operate.frames(film,20,21,22,23,24,25,26,27);

        fly = new Animation(1, true);
        fly.frames(film, 28); // 设置飞行动画帧，你可以根据需要调整帧号

        if (Dungeon.hero.isAlive())
            idle();
        else
            die();
    }

    @Override
    public int blood() {
        return 0xFFFFEA80;
    }

    @Override
    public void move(int from, int to) {
        super.move(from, to);
        if (ch != null && ch.flying) {
            play(fly);
        }
    }

    @Override
    public void idle() {
        super.idle();
        if (ch != null && ch.flying) {
            play(fly);
        }
    }

    @Override
    public void jump(int from, int to, float height, float duration, Callback callback) {
        super.jump(from, to, height, duration, callback);
        play(fly);
    }
}

