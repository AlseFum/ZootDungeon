package com.zootdungeon.arknights.MainTheme.HourOfAnAwakening;

import com.zootdungeon.Assets;
import com.zootdungeon.Statistics;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.mobs.Mob;
import com.zootdungeon.arknights.MainTheme.SkullShatterer;
import com.zootdungeon.levels.Level;
import com.zootdungeon.levels.Terrain;
import com.zootdungeon.levels.features.LevelTransition;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Music;
import com.watabou.utils.Callback;

public class HourOfAnAwakeningBossLevel extends Level {

    private static final int W = 15;
    private static final int H = 15;

    {
        color1 = 0x4a5568;
        color2 = 0x718096;
        viewDistance = 10;
    }

    @Override
    public String tilesTex() {
        return com.zootdungeon.sprites.SpriteRegistry.tilemapTilesTextureOr(
                Assets.Environment.TILES_PRISON,
                tilemapKey
        );
    }

    @Override
    public String waterTex() {
        return com.zootdungeon.sprites.SpriteRegistry.tilemapWaterTextureOr(
                Assets.Environment.WATER_PRISON,
                tilemapKey
        );
    }

    @Override
    protected boolean build() {
        setSize(W, H);
        for (int i = 0; i < length(); i++) {
            map[i] = Terrain.EMPTY;
        }
        // 入口与出口（出口为 LOCKED_EXIT，击败 Boss 掉落钥匙后解锁）
        int entrance = 1 * width() + width() / 2;
        int exit = (height() - 2) * width() + width() / 2;
        transitions.add(new LevelTransition(this, entrance, LevelTransition.Type.REGULAR_ENTRANCE));
        map[entrance] = Terrain.ENTRANCE;
        transitions.add(new LevelTransition(this, exit, LevelTransition.Type.REGULAR_EXIT));
        map[exit] = Terrain.LOCKED_EXIT;
        return true;
    }

    @Override
    public Mob createMob() {
        return null;
    }

    @Override
    protected void createMobs() {
        GLog.w("theme debug: HourOfAnAwakeningBossLevel.createMobs");
        SkullShatterer skull = new SkullShatterer();
        int center = (height() / 2) * width() + (width() / 2);
        skull.pos = center;
        mobs.add(skull);
        Actor.add(skull);
    }

    @Override
    public Actor addRespawner() {
        return null;
    }

    @Override
    protected void createItems() {
    }

    @Override
    public int randomRespawnCell(Char ch) {
        return entrance();
    }

    @Override
    public void playLevelMusic() {
        if (locked) {
            Music.INSTANCE.play(Assets.Music.PRISON_BOSS, true);
        } else {
            Music.INSTANCE.end();
        }
    }

    @Override
    public void seal() {
        if (!locked) {
            super.seal();
            Statistics.qualifiedForBossChallengeBadge = true;
            set(entrance(), Terrain.WATER);
            GameScene.updateMap(entrance());
            GameScene.ripple(entrance());
            Game.runOnRenderThread(new Callback() {
                @Override
                public void call() {
                    Music.INSTANCE.play(Assets.Music.PRISON_BOSS, true);
                }
            });
        }
    }

    @Override
    public void unseal() {
        if (locked) {
            super.unseal();
            set(entrance(), Terrain.ENTRANCE);
            GameScene.updateMap(entrance());
            Game.runOnRenderThread(new Callback() {
                @Override
                public void call() {
                    Music.INSTANCE.fadeOut(5f, new Callback() {
                        @Override
                        public void call() {
                            Music.INSTANCE.end();
                        }
                    });
                }
            });
        }
    }
}
