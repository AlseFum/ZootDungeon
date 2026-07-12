package com.zootdungeon.levels.themes.burningsun;

import com.zootdungeon.Assets;
import com.zootdungeon.actors.blobs.Blob;
import com.zootdungeon.actors.blobs.PulseBlob;
import com.zootdungeon.actors.blobs.ScorchBlob;
import com.zootdungeon.levels.RegularLevel;
import com.zootdungeon.levels.painters.Painter;
import com.zootdungeon.levels.painters.SewerPainter;
import com.zootdungeon.levels.rooms.Room;
import com.zootdungeon.levels.traps.AlarmTrap;
import com.zootdungeon.levels.traps.ConfusionTrap;
import com.zootdungeon.levels.traps.FlockTrap;
import com.zootdungeon.levels.traps.GatewayTrap;
import com.zootdungeon.levels.traps.OozeTrap;
import com.zootdungeon.levels.traps.ShockingTrap;
import com.zootdungeon.levels.traps.SummoningTrap;
import com.zootdungeon.levels.traps.TeleportationTrap;
import com.watabou.noosa.audio.Music;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class BurningSunLevel extends RegularLevel {

    {
        color1 = 0xcc4400;
        color2 = 0xff6600;
    }

    public static final String[] TRACK_LIST = new String[]{
            Assets.Music.SEWERS_1, Assets.Music.SEWERS_2,
            Assets.Music.SEWERS_1, Assets.Music.SEWERS_3
    };
    public static final float[] TRACK_CHANCES = new float[]{1f, 1f, 0.5f, 0.5f};

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
        return new SewerPainter()
                .setWater(0.25f, 4)
                .setGrass(0.10f, 2)
                .setTraps(nTraps(), trapClasses(), trapChances());
    }

    @Override
    public String tilesTex() {
        return Assets.Environment.TILES_SEWERS;
    }

    @Override
    public String waterTex() {
        return Assets.Environment.WATER_SEWERS;
    }

    @Override
    protected Class<?>[] trapClasses() {
        return new Class<?>[]{
                ShockingTrap.class, FlockTrap.class,
                AlarmTrap.class, OozeTrap.class,
                ConfusionTrap.class, SummoningTrap.class, TeleportationTrap.class, GatewayTrap.class};
    }

    @Override
    protected float[] trapChances() {
        return new float[]{4, 4, 2, 2, 1, 1, 1, 1};
    }

    @Override
    protected void createMobs() {
        // 焦土区域（ScorchBlob）：在部分房间的地面生成大块区域
        for (Room r : rooms) {
            if (r instanceof com.zootdungeon.levels.rooms.standard.StandardRoom
                    && Random.Float() < 0.4f) {
                seedBlobInRoom(r, ScorchBlob.class, 3 + Random.Int(4));
            }
        }

        // 脉冲烈焰（PulseBlob）：在部分房间生成 3×3 小块
        for (Room r : rooms) {
            if (r instanceof com.zootdungeon.levels.rooms.standard.StandardRoom
                    && Random.Float() < 0.3f) {
                seedBlobInRoom(r, PulseBlob.class, 1);
            }
        }
    }

    /** 在房间内随机位置播种一块 Blob 区域。 */
    private void seedBlobInRoom(Room r, Class<? extends Blob> blobClass, int amount) {
        ArrayList<com.watabou.utils.Point> points = r.getPoints();
        if (points.isEmpty()) return;

        // 随机选 3~8 个相邻格作为一个块
        int count = 3 + Random.Int(6);
        for (int i = 0; i < count && i < points.size(); i++) {
            com.watabou.utils.Point p = Random.element(points);
            int cell = pointToCell(p);
            if (cell >= 0 && cell < length() && !solid[cell]) {
                Blob.seed(cell, amount, blobClass, this);
            }
        }
    }

    @Override
    public String tileName(int tile) {
        return super.tileName(tile);
    }

    @Override
    public String tileDesc(int tile) {
        return super.tileDesc(tile);
    }
}
