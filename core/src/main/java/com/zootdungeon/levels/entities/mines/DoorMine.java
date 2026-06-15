package com.zootdungeon.levels.entities.mines;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.effects.CellEmitter;
import com.zootdungeon.effects.particles.BlastParticle;
import com.zootdungeon.levels.Level;
import com.zootdungeon.levels.Terrain;
import com.zootdungeon.levels.entities.CellEntitySprite;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.sprites.CharSprite;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 门后地雷（绊索型陷阱）。
 * <p>
 * 放置于任意格子，监控其上下左右 4 个相邻格是否为 {@link Terrain#DOOR}。
 * 当任意角色打开监控范围内的门时立即触发：
 * <ul>
 *   <li>对 3×3 范围内的所有敌人造成 {@link #blastDamage()} 点物理伤害。</li>
 * </ul>
 * <p>
 * 触发检测由 {@link Watcher} 通过轮询实现，不修改 {@link com.zootdungeon.levels.features.Door}。
 * <p>
 * 适用于房间入口/出口的防守布置，诱使敌人在追击时自投罗网。
 */
public class DoorMine extends Mine {

    /** 监控的门格坐标集合（由 {@link #detectDoors()} 填充）。 */
    private int[] monitoredDoors = new int[0];

    @Override
    public Class<? extends CellEntitySprite> spriteClass() {
        return PatternMine.Sprite.class;
    }

    /** 3×3 范围爆炸伤害。 */
    public int blastDamage() {
        return 8;
    }

    @Override
    public void onSpawn(Level level) {
        super.onSpawn(level);
        detectDoors();
        Watcher.instance().watch(this, monitoredDoors);
    }

    /**
     * 扫描上下左右 4 格，收集所有当前为 {@link Terrain#DOOR} 的门格坐标。
     */
    private void detectDoors() {
        if (Dungeon.level == null) return;
        java.util.List<Integer> doors = new java.util.ArrayList<>();
        int[] adjacent = Watcher.adjacentCells(pos, Dungeon.level.width());
        for (int cell : adjacent) {
            if (cell >= 0 && cell < Dungeon.level.length()
                    && Dungeon.level.map[cell] == Terrain.DOOR) {
                doors.add(cell);
            }
        }
        monitoredDoors = doors.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * 由 {@link Watcher} 调用，触发本枚地雷。
     */
    void triggerWith() {
        detonate();
    }

    @Override
    protected void onDetonate() {
        Watcher.instance().forget(this);

        if (Dungeon.level == null) {
            return;
        }

        int w = Dungeon.level.width();

        // 1. 3×3 范围爆炸视觉效果
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int cell = pos + dx + dy * w;
                if (!Dungeon.level.insideMap(cell)) {
                    continue;
                }
                if (Dungeon.level.heroFOV[cell]) {
                    CellEmitter.center(cell).burst(BlastParticle.FACTORY, 3);
                }
            }
        }

        // 2. 对范围内敌人造成物理伤害
        int dmg = Math.max(0, blastDamage());
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int cell = pos + dx + dy * w;
                if (!Dungeon.level.insideMap(cell)) {
                    continue;
                }
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
    }

    @Override
    protected float blastVisualRadius() {
        return 1.5f;
    }

    @Override
    public String desc() {
        return Messages.get(this, "desc", blastDamage());
    }

    // ==================== Watcher（内部类）====================

    /**
     * 门后地雷的统一监听器。
     * <p>
     * 继承 {@link Actor}，每回合轮询所有监控的门格。
     * 当检测到任意监控的门已从 {@link Terrain#DOOR} 变为 {@code OPEN_DOOR} 时，
     * 触发对应的 {@link DoorMine} 引爆。
     * <p>
     * 工作流程：
     * <ol>
     *   <li>放置 {@link DoorMine} 时，{@code DoorMine.Watcher.instance().watch(...)} 注册。</li>
     *   <li>每回合 {@link #act()} 被调用时，轮询所有监控门格。</li>
     *   <li>发现门已打开，则触发对应地雷。</li>
     *   <li>地雷引爆后通过 {@link #forget(DoorMine)} 注销。</li>
     * </ol>
     */
    public static class Watcher extends Actor {

        private static Watcher INSTANCE;

        /**
         * 获取 Watcher 单例（首次调用时创建并登记到 Actor）。
         * 保留 static 引用防止 GC 回收。
         */
        public static Watcher instance() {
            if (INSTANCE == null) {
                INSTANCE = new Watcher();
                Actor.add(INSTANCE);
            }
            return INSTANCE;
        }

        /** 门格坐标 → 监控该门的全部 DoorMine。 */
        private final Map<Integer, Set<DoorMine>> doorToMines = new HashMap<>();

        /** 门格坐标 → 当前是否已打开（防止同一扇门在同一帧多次触发）。 */
        private final Map<Integer, Boolean> doorOpened = new HashMap<>();

        private Watcher() {}

        /**
         * 注册一枚门后地雷及其监控的门格。
         * 由 {@link DoorMine#onSpawn(Level)} 调用。
         *
         * @param mine      门后地雷实例
         * @param doorCells 地雷监控的门格坐标数组
         */
        public void watch(DoorMine mine, int[] doorCells) {
            for (int cell : doorCells) {
                doorToMines.computeIfAbsent(cell, k -> new HashSet<>()).add(mine);
            }
        }

        /**
         * 注销一枚门后地雷。
         * 由 {@link DoorMine#onDetonate()} 调用。
         */
        public void forget(DoorMine mine) {
            doorToMines.values().forEach(s -> s.remove(mine));
        }

        @Override
        protected boolean act() {
            if (Dungeon.level == null || Dungeon.level.locked) {
                spend(TICK);
                return true;
            }

            // 遍历所有监控门格的副本，防止迭代中修改集合
            for (Map.Entry<Integer, Set<DoorMine>> entry : new HashMap<>(doorToMines).entrySet()) {
                int doorCell = entry.getKey();
                Set<DoorMine> mines = entry.getValue();
                if (mines.isEmpty()) {
                    continue;
                }

                int terrain = Dungeon.level.map[doorCell];
                boolean isOpen = terrain == Terrain.OPEN_DOOR;
                Boolean wasOpen = doorOpened.get(doorCell);

                // 检测：之前未记录/未打开 → 现在打开了
                if (!Boolean.TRUE.equals(wasOpen) && isOpen) {
                    doorOpened.put(doorCell, true);
                    for (DoorMine mine : new HashSet<>(mines)) {
                        if (!mine.detonated) {
                            mine.triggerWith();
                        }
                    }
                } else if (wasOpen == null) {
                    // 首次观测到该门，记录初始状态
                    doorOpened.put(doorCell, isOpen);
                }
            }

            spend(TICK);
            return true;
        }

        /** 计算上下左右相邻坐标。 */
        private static int[] adjacentCells(int pos, int width) {
            return new int[]{
                    pos - width,  // 上
                    pos + width,  // 下
                    pos - 1,      // 左
                    pos + 1       // 右
            };
        }
    }
}
