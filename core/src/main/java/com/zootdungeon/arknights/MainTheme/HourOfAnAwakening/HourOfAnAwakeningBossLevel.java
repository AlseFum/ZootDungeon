package com.zootdungeon.arknights.MainTheme.HourOfAnAwakening;

import com.zootdungeon.Assets;
import com.zootdungeon.Bones;
import com.zootdungeon.Statistics;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.mobs.Mob;
import com.zootdungeon.arknights.SarkazCenturion;
import com.zootdungeon.items.Heap;
import com.zootdungeon.items.Item;
import com.zootdungeon.levels.Terrain;
import com.zootdungeon.levels.builders.Builder;
import com.zootdungeon.levels.builders.FigureEightBuilder;
import com.zootdungeon.levels.painters.Painter;
import com.zootdungeon.levels.painters.SewerPainter;
import com.zootdungeon.levels.rooms.Room;
import com.zootdungeon.levels.rooms.standard.StandardRoom;
import com.zootdungeon.levels.themes.sewer.sewerboss.SewerBossEntranceRoom;
import com.zootdungeon.levels.themes.sewer.sewerboss.SewerBossExitRoom;
import com.zootdungeon.scenes.GameScene;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.audio.Music;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.Point;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class HourOfAnAwakeningBossLevel extends HourOfAnAwakeningLevel {

    {
        color1 = 0x48763c;
        color2 = 0x59994a;
    }

    private Room bossRoom;

    @Override
    public void playLevelMusic() {
        if (locked) {
            Music.INSTANCE.play(Assets.Music.SEWERS_BOSS, true);
            return;
        }

        boolean bossAlive = false;
        for (Mob m : mobs) {
            if (m instanceof SarkazCenturion) {
                bossAlive = true;
                break;
            }
        }

        if (bossAlive) {
            Music.INSTANCE.end();
        } else {
            Music.INSTANCE.playTracks(TRACK_LIST, TRACK_CHANCES, false);
        }
    }

    @Override
    protected ArrayList<Room> initRooms() {
        ArrayList<Room> initRooms = new ArrayList<>();

        initRooms.add(roomEntrance = new SewerBossEntranceRoom());
        initRooms.add(roomExit = new SewerBossExitRoom());

        int standards = standardRooms(true);
        for (int i = 0; i < standards; i++) {
            StandardRoom s = StandardRoom.createRoom();
            s.setSizeCat(0, 0);
            initRooms.add(s);
        }

        // Boss room: use a standard room, SarkazCenturion is spawned inside createMobs
        bossRoom = StandardRoom.createRoom();
        initRooms.add(bossRoom);
        ((FigureEightBuilder) builder).setLandmarkRoom(bossRoom);

        return initRooms;
    }

    @Override
    protected int standardRooms(boolean forceMax) {
        if (forceMax) return 3;
        return 2 + Random.chances(new float[]{1, 1});
    }

    @Override
    protected Builder builder() {
        return new FigureEightBuilder()
                .setLoopShape(2, Random.Float(0.3f, 0.8f), 0f)
                .setPathLength(1f, new float[]{1})
                .setTunnelLength(new float[]{1, 2}, new float[]{1});
    }

    @Override
    protected Painter painter() {
        return new SewerPainter()
                .setWater(0.50f, 5)
                .setGrass(0.20f, 4)
                .setTraps(nTraps(), trapClasses(), trapChances());
    }

    @Override
    protected int nTraps() {
        return 0;
    }

    @Override
    protected void createMobs() {
        if (bossRoom != null) {
            SarkazCenturion boss = new SarkazCenturion();
            int pos;
            do {
                pos = pointToCell(bossRoom.random());
            } while (pos == entrance() || solid[pos] || Actor.findChar(pos) != null);
            boss.pos = pos;
            mobs.add(boss);
        }
    }

    @Override
    public Actor addRespawner() {
        return null;
    }

    @Override
    protected void createItems() {
        Random.pushGenerator(Random.Long());
        ArrayList<Item> bonesItems = Bones.get();
        if (bonesItems != null) {
            int pos;
            do {
                pos = pointToCell(roomEntrance.random());
            } while (pos == entrance() || solid[pos]);
            for (Item i : bonesItems) {
                drop(i, pos).setHauntedIfCursed().type = Heap.Type.REMAINS;
            }
        }
        Random.popGenerator();
    }

    @Override
    public int randomRespawnCell(Char ch) {
        ArrayList<Integer> candidates = new ArrayList<>();
        for (Point p : roomEntrance.getPoints()) {
            int cell = pointToCell(p);
            if (passable[cell]
                    && roomEntrance.inside(p)
                    && Actor.findChar(cell) == null
                    && (!Char.hasProp(ch, Char.Property.LARGE) || openSpace[cell])) {
                candidates.add(cell);
            }
        }
        if (candidates.isEmpty()) {
            return -1;
        } else {
            return Random.element(candidates);
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
                    Music.INSTANCE.play(Assets.Music.SEWERS_BOSS, true);
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

    @Override
    public Group addVisuals() {
        super.addVisuals();
        if (map[exit() - 1] != Terrain.WALL_DECO)
            visuals.add(new Drip(exit() - 1));
        if (map[exit() + 1] != Terrain.WALL_DECO)
            visuals.add(new Drip(exit() + 1));
        return visuals;
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        roomExit = roomEntrance;
    }
}
