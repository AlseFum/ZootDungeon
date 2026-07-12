package com.zootdungeon.arknights.MainTheme.HourOfAnAwakening;

import com.zootdungeon.Assets;
import com.zootdungeon.Dungeon;
import com.zootdungeon.Statistics;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.actors.mobs.npcs.Ghost;
import com.zootdungeon.effects.Ripple;
import com.zootdungeon.levels.Level;
import com.zootdungeon.levels.RegularLevel;
import com.zootdungeon.levels.Terrain;
import com.zootdungeon.levels.features.LevelTransition;
import com.zootdungeon.levels.painters.Painter;
import com.zootdungeon.levels.painters.SewerPainter;
import com.zootdungeon.levels.traps.AlarmTrap;
import com.zootdungeon.levels.traps.ChillingTrap;
import com.zootdungeon.levels.traps.ConfusionTrap;
import com.zootdungeon.levels.traps.FlockTrap;
import com.zootdungeon.levels.traps.GatewayTrap;
import com.zootdungeon.levels.traps.OozeTrap;
import com.zootdungeon.levels.traps.ShockingTrap;
import com.zootdungeon.levels.traps.SummoningTrap;
import com.zootdungeon.levels.traps.TeleportationTrap;
import com.zootdungeon.levels.traps.ToxicTrap;
import com.zootdungeon.levels.traps.WornDartTrap;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.scenes.InterlevelScene;
import com.zootdungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.audio.Music;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.ColorMath;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

public class HourOfAnAwakeningLevel extends RegularLevel {

    {
        color1 = 0x48763c;
        color2 = 0x59994a;
    }

    public static final String[] TRACK_LIST = new String[]{
            Assets.Music.SEWERS_1, Assets.Music.SEWERS_2,
            Assets.Music.SEWERS_2, Assets.Music.SEWERS_1,
            Assets.Music.SEWERS_3, Assets.Music.SEWERS_3
    };
    public static final float[] TRACK_CHANCES = new float[]{1f, 1f, 0.5f, 0.25f, 1f, 0.5f};

    @Override
    public void playLevelMusic() {
        if (Ghost.Quest.active() || Statistics.amuletObtained) {
            if (Statistics.amuletObtained && Dungeon.depth == 1) {
                Music.INSTANCE.play(Assets.Music.THEME_FINALE, true);
            } else {
                Music.INSTANCE.play(Assets.Music.SEWERS_TENSE, true);
            }
        } else {
            Music.INSTANCE.playTracks(TRACK_LIST, TRACK_CHANCES, false);
        }
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
                .setWater(feeling == Feeling.WATER ? 0.85f : 0.30f, 5)
                .setGrass(feeling == Feeling.GRASS ? 0.80f : 0.20f, 4)
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
        return Dungeon.depth == 1
                ? new Class<?>[]{WornDartTrap.class}
                : new Class<?>[]{
                ChillingTrap.class, ShockingTrap.class, ToxicTrap.class, WornDartTrap.class,
                AlarmTrap.class, OozeTrap.class,
                ConfusionTrap.class, FlockTrap.class, SummoningTrap.class, TeleportationTrap.class, GatewayTrap.class};
    }

    @Override
    protected float[] trapChances() {
        return Dungeon.depth == 1
                ? new float[]{1}
                : new float[]{
                4, 4, 4, 4,
                2, 2,
                1, 1, 1, 1, 1};
    }

    @Override
    public boolean activateTransition(Hero hero, LevelTransition transition) {
        if (transition.type == LevelTransition.Type.REGULAR_ENTRANCE && transition.destDepth == 0) {
            Dungeon.depth = 0;
            InterlevelScene.mode = InterlevelScene.Mode.ASCEND;
            Game.switchScene(InterlevelScene.class);
            return true;
        } else {
            return super.activateTransition(hero, transition);
        }
    }

    @Override
    public Group addVisuals() {
        super.addVisuals();
        addVisuals(this, visuals);
        return visuals;
    }

    public static void addVisuals(Level level, Group group) {
        for (int i = 0; i < level.length(); i++) {
            if (level.map[i] == Terrain.WALL_DECO) {
                group.add(new Drip(i));
            }
        }
    }

    @Override
    public String tileName(int tile) {
        return switch (tile) {
            case Terrain.WATER -> Messages.get(HourOfAnAwakeningLevel.class, "water_name");
            default -> super.tileName(tile);
        };
    }

    @Override
    public String tileDesc(int tile) {
        return switch (tile) {
            case Terrain.EMPTY_DECO -> Messages.get(HourOfAnAwakeningLevel.class, "empty_deco_desc");
            case Terrain.BOOKSHELF -> Messages.get(HourOfAnAwakeningLevel.class, "bookshelf_desc");
            default -> super.tileDesc(tile);
        };
    }

    static class Drip extends Emitter {

        private int pos;
        private float rippleDelay = 0;

        private static final Factory factory = new Factory() {
            @Override
            public void emit(Emitter emitter, int index, float x, float y) {
                WaterParticle p = (WaterParticle) emitter.recycle(WaterParticle.class);
                p.reset(x, y);
            }
        };

        public Drip(int pos) {
            super();
            this.pos = pos;
            PointF p = DungeonTilemap.tileCenterToWorld(pos);
            pos(p.x - 2, p.y + 3, 4, 0);
            pour(factory, 0.1f);
        }

        @Override
        public void update() {
            if (visible = (pos < Dungeon.level.heroFOV.length && Dungeon.level.heroFOV[pos])) {
                super.update();
                if (!isFrozen() && (rippleDelay -= Game.elapsed) <= 0) {
                    Ripple ripple = GameScene.ripple(pos + Dungeon.level.width());
                    if (ripple != null) {
                        ripple.y -= DungeonTilemap.SIZE / 2;
                        rippleDelay = Random.Float(0.4f, 0.6f);
                    }
                }
            }
        }
    }

    public static final class WaterParticle extends PixelParticle {
        public WaterParticle() {
            super();
            acc.y = 50;
            am = 0.5f;
            color(ColorMath.random(0xb6ccc2, 0x3b6653));
            size(2);
        }

        public void reset(float x, float y) {
            revive();
            this.x = x;
            this.y = y;
            speed.set(Random.Float(-2, +2), 0);
            left = lifespan = 0.4f;
        }
    }
}
