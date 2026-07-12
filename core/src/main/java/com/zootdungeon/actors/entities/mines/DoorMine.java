package com.zootdungeon.actors.entities.mines;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.entities.Mine;
import com.zootdungeon.effects.CellEmitter;
import com.zootdungeon.effects.particles.BlastParticle;
import com.zootdungeon.effects.particles.SparkParticle;
import com.zootdungeon.levels.Level;
import com.zootdungeon.levels.Terrain;
import com.zootdungeon.sprites.CellEntitySprite;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.sprites.CharSprite;

/**
 * 门后地雷（绊索型陷阱）。
 * <p>
 * 放置于任意格子，监控其上下左右 4 个相邻格是否为 {@link Terrain#DOOR}。
 * 当任意角色打开监控范围内的门时立即触发，沿门方向三格造成伤害。
 * <p>
 * 触发检测由自身的 {@link #act()} 每回合轮询实现，不依赖外部 Actor。
 */
public class DoorMine extends Mine {

    /** 监控的门格坐标集合（由 {@link #detectDoors()} 填充）。 */
    private int[] monitoredDoors = new int[0];

    /** 用于记录各门上一次检测时的状态，避免重复触发。 */
    private final java.util.HashMap<Integer, Boolean> doorStates = new java.util.HashMap<>();

    /** 触发本次爆炸的门格坐标。 */
    private int triggeredByDoor = -1;

    @Override
    public Class<? extends CellEntitySprite> spriteClass() {
        return PatternMine.Sprite.class;
    }

    /** 门方向每格伤害。 */
    public int blastDamage() {
        return 8;
    }

    @Override
    public void onSpawn(Level level) {
        super.onSpawn(level);
        detectDoors();
    }

    /**
     * 扫描上下左右 4 格，收集所有当前为 {@link Terrain#DOOR} 的门格坐标。
     */
    private void detectDoors() {
        if (Dungeon.level == null) return;
        java.util.List<Integer> doors = new java.util.ArrayList<>();
        int w = Dungeon.level.width();
        int[] adjacent = new int[]{ pos - w, pos + w, pos - 1, pos + 1 };
        for (int cell : adjacent) {
            if (cell >= 0 && cell < Dungeon.level.length()
                    && Dungeon.level.map[cell] == Terrain.DOOR) {
                doors.add(cell);
            }
        }
        monitoredDoors = doors.stream().mapToInt(Integer::intValue).toArray();
        for (int d : monitoredDoors) {
            doorStates.put(d, false);
        }
    }

    // ===== 每回合轮询 =====

    @Override
    protected boolean act() {
        if (Dungeon.level == null || Dungeon.level.locked || detonated) {
            spend(TICK);
            return true;
        }

        for (int doorCell : monitoredDoors) {
            boolean isOpen = Dungeon.level.map[doorCell] == Terrain.OPEN_DOOR;
            if (Boolean.FALSE.equals(doorStates.get(doorCell)) && isOpen) {
                triggeredByDoor = doorCell;
                detonate();
                return true;
            }
        }

        spend(TICK);
        return true;
    }

    // ===== 爆炸 =====

    @Override
    protected void onDetonate() {
        if (Dungeon.level == null) return;

        int w = Dungeon.level.width();

        // 计算门方向（归一化到 -1/0/1）
        int bDX = 0, bDY = 0;
        if (triggeredByDoor >= 0) {
            bDX = Integer.signum((triggeredByDoor % w) - (pos % w));
            bDY = Integer.signum((triggeredByDoor / w) - (pos / w));
        }

        // 沿门方向三格：地雷格 → 门格 → 门后一格
        int[] line = new int[3];
        line[0] = pos;
        line[1] = pos + bDX + bDY * w;
        line[2] = pos + bDX * 2 + bDY * 2 * w;

        int dmg = Math.max(0, blastDamage());

        for (int i = 0; i < 3; i++) {
            int cell = line[i];
            if (!Dungeon.level.insideMap(cell)) continue;

            // 特效
            if (Dungeon.level.heroFOV[cell]) {
                if (i < 2) {
                    CellEmitter.center(cell).burst(BlastParticle.FACTORY, 4);
                } else {
                    CellEmitter.center(cell).burst(SparkParticle.FACTORY, 8);
                    CellEmitter.center(cell).burst(BlastParticle.FACTORY, 3);
                }
            }

            // 伤害
            Char ch = Actor.findChar(cell);
            if (ch != null && ch.isAlive() && ch.alignment == Char.Alignment.ENEMY) {
                ch.damage(dmg, this);
                if (ch.sprite != null) {
                    ch.sprite.showStatus(CharSprite.NEGATIVE,
                            Messages.get(this, "status", dmg));
                }
            }
        }
    }

    @Override
    protected float blastVisualRadius() {
        return 1.5f;
    }

    @Override
    public String desc() {
        return Messages.get(this, "desc", blastDamage());
    }
}
