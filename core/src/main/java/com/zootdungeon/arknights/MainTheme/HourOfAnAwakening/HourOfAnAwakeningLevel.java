package com.zootdungeon.arknights.MainTheme.HourOfAnAwakening;

import com.zootdungeon.Assets;
import com.zootdungeon.levels.Level;
import com.zootdungeon.levels.RegularLevel;
import com.zootdungeon.levels.painters.Painter;
import com.zootdungeon.levels.painters.RegularPainter;
import com.zootdungeon.levels.rooms.Room;
import com.zootdungeon.levels.traps.BurningTrap;
import com.zootdungeon.levels.traps.ChillingTrap;
import com.zootdungeon.levels.traps.ShockingTrap;
import com.zootdungeon.levels.traps.WornDartTrap;
import com.watabou.noosa.audio.Music;
import com.watabou.utils.Random;

public class HourOfAnAwakeningLevel extends RegularLevel {

    {
        color1 = 0x4a5568;
        color2 = 0x718096;
    }

    public static final String[] TRACK_LIST = new String[]{
            Assets.Music.SEWERS_1, Assets.Music.SEWERS_2, Assets.Music.SEWERS_3
    };
    public static final float[] TRACK_CHANCES = new float[]{1f, 1f, 0.5f};

    @Override
    public void playLevelMusic() {
        Music.INSTANCE.playTracks(TRACK_LIST, TRACK_CHANCES, false);
    }

    @Override
    protected int standardRooms(boolean forceMax) {
        if (forceMax) return 6;
        return 4 + Random.chances(new float[]{1, 3, 1});
    }

    @Override
    protected int specialRooms(boolean forceMax) {
        if (forceMax) return 2;
        return 1 + Random.chances(new float[]{1, 4});
    }

    @Override
    protected Painter painter() {
        return new RegularPainter() {
            @Override
            protected void decorate(Level level, java.util.ArrayList<Room> rooms) {
                // 默认装饰留空，可在此扩展
            }
        }
                .setWater(feeling == Feeling.WATER ? 0.85f : 0.25f, 4)
                .setGrass(feeling == Feeling.GRASS ? 0.75f : 0.18f, 3)
                .setTraps(nTraps(), trapClasses(), trapChances());
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
    protected Class<?>[] trapClasses() {
        return new Class<?>[]{
                WornDartTrap.class, ChillingTrap.class, BurningTrap.class, ShockingTrap.class
        };
    }

    @Override
    protected float[] trapChances() {
        return new float[]{4, 2, 2, 2};
    }
}
