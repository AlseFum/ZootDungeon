package com.zootdungeon.actors.entities.mines;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.buffs.Amok;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.buffs.Cripple;
import com.zootdungeon.actors.buffs.FlavourBuff;
import com.zootdungeon.actors.buffs.Slow;
import com.zootdungeon.actors.buffs.Terror;
import com.zootdungeon.actors.buffs.Vertigo;
import com.zootdungeon.actors.buffs.Weakness;
import com.zootdungeon.effects.CellEmitter;
import com.zootdungeon.effects.particles.BlastParticle;
import com.zootdungeon.effects.particles.EnergyParticle;
import com.zootdungeon.effects.particles.ShadowParticle;
import com.zootdungeon.effects.particles.SparkParticle;
import com.watabou.noosa.particles.Emitter;
import com.zootdungeon.levels.Level;
import com.zootdungeon.actors.Entity;
import com.zootdungeon.actors.entities.CellEntitySprite;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.sprites.CharSprite;
import com.watabou.noosa.TextureFilm;

/**
 * 图案地雷（共鸣型）。
 * <p>
 * 被敌人踩中时，扫描周围 {@link #scanRadius()} 格内的所有同类地雷，
 * 尝试以每个地雷为中心匹配预定义图案，选择<strong>点数最多的形状</strong>。
 * 匹配成功时在普通爆炸伤害之外触发<strong>特殊共鸣效果</strong>。
 * <p>
 * 效果回调接收两个参数：<br>
 * - {@code centerPos}: 图案中心位置（匹配形状时的中心地雷坐标）<br>
 * - {@code triggerPos}: 触发地雷的位置（敌人踩中的位置）
 * <p>
 * 只有 {@link Char.Alignment#ENEMY} 能触发，英雄/盟友安全。
 * <p>
 * <b>扩展方式：</b>
 * <pre>{@code
 * // 方式1: 使用 registerPattern() 注册新图案
 * PatternMine.registerPattern(
 *     "my_pattern",                          // 名称
 *     new int[][]{{-1,0}, {-1,-1}, {0,-1}},  // 相对坐标（不含原点）
 *     (centerPos, triggerPos) -> { /* 自定义效果 *\/ },   // 效果回调
 *     () -> MyParticle.FACTORY               // 粒子工厂
 * );
 *
 * // 方式2: 继承 PatternMine 重写 registerPatterns()
 * public class MyMine extends PatternMine {
 *     static {
 *         registerPattern("custom", ...);
 *     }
 * }
 * }</pre>
 */
public class PatternMine extends Mine {

    // ==================== 注册表 ====================

    /** 图案注册表 */
    private static final List<PatternDef> PATTERNS = new ArrayList<>();

    static {
        registerDefaultPatterns();
    }

    // ==================== Sprite ====================

    @Override
    public Class<? extends CellEntitySprite> spriteClass() {
        return Sprite.class;
    }

    // ==================== Tuning ====================

    /** 普通爆炸对单体的伤害。 */
    public int baseDamage() {
        return 6;
    }

    // ==================== Triggers ====================

    private Char triggeredBy;

    @Override
    public void onStep(Char who) {
        if (isEnemy(who)) {
            triggeredBy = who;
            detonate();
            triggeredBy = null;
        }
    }

    // ==================== Lifecycle ====================

    @Override
    public void onSpawn(Level level) {
        sprite.place(pos);
    }

    // ==================== Core ====================

    @Override
    protected void onDetonate() {
        if (Dungeon.level == null) {
            return;
        }

        // 1. 收集周围同类地雷
        List<PatternMine> group = collectGroup();

        // 2. 收集所有可能匹配的形状（每个地雷作为中心）
        List<MatchedShape> candidates = findAllMatches(group);

        // 3. 选择点数最多（最复杂）的形状
        MatchedShape best = selectBestMatch(candidates);

        // 4. 基础爆炸伤害（3×3 范围）
        applyBaseBlast();

        // 5. 共鸣特殊效果
        if (best != null) {
            best.def.effect.accept(best.centerPos, triggeredBy != null ? triggeredBy.pos : best.centerPos);
        }

        // 6. 播放引爆动画
        if (sprite instanceof MineSprite) {
            ((MineSprite) sprite).detonate();
        }
    }

    // ---------- Collection ----------

    private List<PatternMine> collectGroup() {
        List<PatternMine> group = new ArrayList<>();
        int width = Dungeon.level.width();
        int height = Dungeon.level.height();

        // 收集所有同类地雷，不限制范围
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int cell = x + y * width;
                Entity entity = Dungeon.level.cellEntityAt(cell);
                if (entity instanceof PatternMine && entity != this) {
                    group.add((PatternMine) entity);
                }
            }
        }
        return group;
    }

    // ---------- Pattern matching ----------

    /**
     * 收集所有可能的匹配结果。
     * 每个地雷（包含自身）都作为中心尝试匹配。
     */
    private List<MatchedShape> findAllMatches(List<PatternMine> group) {
        List<MatchedShape> candidates = new ArrayList<>();

        // 以自身为中心匹配
        addMatchesForCenter(this.pos, group, candidates);

        // 以周围每个地雷为中心匹配
        for (PatternMine mine : group) {
            addMatchesForCenter(mine.pos, group, candidates);
        }

        return candidates;
    }

    /**
     * 以指定位置为中心，收集所有匹配的图案。
     */
    private void addMatchesForCenter(int center, List<PatternMine> others, List<MatchedShape> candidates) {
        int w = Dungeon.level.width();
        int cx = center % w;
        int cy = center / w;

        // 收集以 center 为中心的所有地雷坐标（不限制范围）
        Set<Point> points = new HashSet<>();
        for (PatternMine m : others) {
            int mx = m.pos % w;
            int my = m.pos / w;
            int relX = mx - cx;
            int relY = my - cy;
            points.add(new Point(relX, relY));
        }

        // 检查每个图案
        for (PatternDef def : PATTERNS) {
            if (def.matches(points)) {
                candidates.add(new MatchedShape(def, center));
            }
        }
    }

    /**
     * 选择点数最多（最复杂）的匹配。
     */
    private MatchedShape selectBestMatch(List<MatchedShape> candidates) {
        if (candidates.isEmpty()) {
            return null;
        }

        return candidates.stream()
                .max(Comparator.comparingInt(c -> c.def.points.size()))
                .orElse(null);
    }

    // ---------- Base blast ----------

    private void applyBaseBlast() {
        int width = Dungeon.level.width();
        int dmg = Math.max(0, baseDamage());

        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int cell = pos + dx + dy * width;
                if (!Dungeon.level.insideMap(cell)) {
                    continue;
                }
                if (Dungeon.level.heroFOV[cell]) {
                    CellEmitter.center(cell).burst(BlastParticle.FACTORY, 3);
                }
                Char ch = Actor.findChar(cell);
                if (isEnemy(ch)) {
                    ch.damage(dmg, this);
                    if (ch.sprite != null) {
                        ch.sprite.showStatus(CharSprite.NEGATIVE,
                                Messages.get(this, "status", dmg));
                    }
                }
            }
        }
    }

    // ---------- Helper methods for effects ----------

    /**
     * 对圆形范围内的敌人施加 Buff。
     *
     * @param centerPos 效果中心位置
     * @param radius    效果半径（曼哈顿距离）
     * @param buffClass Buff 类型
     * @param duration  持续时间（秒）
     */
    protected <T extends FlavourBuff> void applyBuffToArea(
            int centerPos, int radius, Class<T> buffClass, float duration) {
        applyBuffToArea(centerPos, radius, buffClass, duration, EnergyParticle.FACTORY);
    }

    /**
     * 对圆形范围内的敌人施加 Buff（带自定义粒子）。
     */
    protected <T extends FlavourBuff> void applyBuffToArea(
            int centerPos, int radius, Class<T> buffClass, float duration, Emitter.Factory particle) {
        int width = Dungeon.level.width();

        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                if (Math.abs(dx) + Math.abs(dy) > radius) continue; // 圆形范围

                int cell = centerPos + dx + dy * width;
                if (!Dungeon.level.insideMap(cell)) {
                    continue;
                }
                if (Dungeon.level.heroFOV[cell]) {
                    CellEmitter.get(cell).burst(particle, 1);
                }
                Char ch = Actor.findChar(cell);
                if (isEnemy(ch)) {
                    Buff.prolong(ch, buffClass, duration);
                    if (ch.sprite != null) {
                        ch.sprite.showStatus(CharSprite.NEGATIVE,
                                Messages.get(this, "pattern_" + buffClass.getSimpleName().toLowerCase()));
                    }
                }
            }
        }
    }

    /**
     * 对矩形范围内的敌人施加伤害。
     */
    protected void applyDamageToArea(int centerPos, int radius, int damage) {
        int width = Dungeon.level.width();
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int cell = centerPos + dx + dy * width;
                if (!Dungeon.level.insideMap(cell)) {
                    continue;
                }
                Char ch = Actor.findChar(cell);
                if (isEnemy(ch)) {
                    ch.damage(Math.max(0, damage), this);
                }
            }
        }
    }

    /**
     * 产生冲击波效果，向 triggerPos 方向扩散。
     */
    protected void applyBlastWave(int centerPos, int triggerPos, int maxDist, Emitter.Factory particle) {
        int w = Dungeon.level.width();
        int cx = centerPos % w;
        int cy = centerPos / w;
        int tx = triggerPos % w;
        int ty = triggerPos / w;

        // 计算方向
        int dx = Integer.signum(tx - cx);
        int dy = Integer.signum(ty - cy);

        int cur = centerPos;
        for (int i = 0; i < maxDist; i++) {
            cur += dx + dy * w;
            if (!Dungeon.level.insideMap(cur)) break;
            if (Dungeon.level.solid[cur]) break;

            if (Dungeon.level.heroFOV[cur]) {
                CellEmitter.center(cur).burst(particle, 3);
            }
            Char ch = Actor.findChar(cur);
            if (isEnemy(ch)) {
                ch.damage(Math.max(0, baseDamage() / 2), this);
                if (ch.isAlive() && ch.sprite != null) {
                    ch.sprite.showStatus(CharSprite.NEGATIVE,
                            Messages.get(this, "pattern_line"));
                }
            }
        }
    }

    // ---------- Description ----------

    @Override
    public String desc() {
        return Messages.get(this, "desc", baseDamage());
    }

    // ==================== 图案注册 API ====================

    /**
     * 注册新图案。
     *
     * @param name     图案名称（唯一标识）
     * @param offsets  相对坐标数组，如 new int[][]{{-1,0}, {-1,-1}} 表示 L 形
     * @param effect   效果回调，(centerPos, triggerPos) -> { ... }
     * @param particle 粒子工厂
     */
    public static void registerPattern(
            String name,
            int[][] offsets,
            BiConsumer<Integer, Integer> effect,
            Emitter.Factory particle) {

        Set<Point> points = new HashSet<>();
        for (int[] offset : offsets) {
            points.add(new Point(offset[0], offset[1]));
        }

        PATTERNS.add(new PatternDef(name, points, effect, particle));
    }

    /**
     * 注册新图案（无自定义粒子，使用默认能量粒子）。
     */
    public static void registerPattern(
            String name,
            int[][] offsets,
            BiConsumer<Integer, Integer> effect) {
        registerPattern(name, offsets, effect, EnergyParticle.FACTORY);
    }

    /**
     * 注册默认图案。可被子类覆盖并选择性调用。
     */
    protected static void registerDefaultPatterns() {
        // L 形：3 格，Cripple 3 回合
        registerPattern("L", new int[][]{{-1, 0}, {-1, -1}},
                (centerPos, triggerPos) -> {
                    PatternMine mine = new PatternMine();
                    mine.applyBuffToArea(centerPos, 1, Cripple.class, 3f);
                });

        // T 形：4 格，Slow 3 回合
        registerPattern("T", new int[][]{{-1, 0}, {+1, 0}, {0, -1}},
                (centerPos, triggerPos) -> {
                    PatternMine mine = new PatternMine();
                    mine.applyBuffToArea(centerPos, 1, Slow.class, 3f);
                });

        // 水平直线：冲击波
        registerPattern("LINE_H", new int[][]{{-1, 0}, {+1, 0}},
                (centerPos, triggerPos) -> {
                    PatternMine mine = new PatternMine();
                    mine.applyBlastWave(centerPos, triggerPos, 4, SparkParticle.FACTORY);
                }, SparkParticle.FACTORY);

        // 垂直直线
        registerPattern("LINE_V", new int[][]{{0, -1}, {0, +1}},
                (centerPos, triggerPos) -> {
                    PatternMine mine = new PatternMine();
                    mine.applyBlastWave(centerPos, triggerPos, 4, SparkParticle.FACTORY);
                }, SparkParticle.FACTORY);

        // 对角线：Weakness 4 回合
        registerPattern("DIAGONAL", new int[][]{{-1, -1}, {+1, +1}},
                (centerPos, triggerPos) -> {
                    PatternMine mine = new PatternMine();
                    mine.applyBuffToArea(centerPos, 1, Weakness.class, 4f);
                });

        // Z 形（宽）：6 格，Cripple 3 回合
        registerPattern("Z_WIDE", new int[][]{{-1, -1}, {0, -1}, {-2, 0}, {+1, 0}, {-1, +1}, {0, +1}},
                (centerPos, triggerPos) -> {
                    PatternMine mine = new PatternMine();
                    mine.applyBuffToArea(centerPos, 1, Cripple.class, 3f);
                });

        // 菱形：4 格，Vertigo 3 回合
        registerPattern("DIAMOND", new int[][]{{-1, 0}, {0, -1}, {+1, 0}},
                (centerPos, triggerPos) -> {
                    PatternMine mine = new PatternMine();
                    mine.applyBuffToArea(centerPos, 2, Vertigo.class, 3f);
                });

        // 十字形：5 格，Terror + Amok
        registerPattern("CROSS", new int[][]{{-1, 0}, {+1, 0}, {0, -1}, {0, +1}},
                (centerPos, triggerPos) -> {
                    int width = Dungeon.level.width();
                    for (int dy = -2; dy <= 2; dy++) {
                        for (int dx = -2; dx <= 2; dx++) {
                            int cell = centerPos + dx + dy * width;
                            if (!Dungeon.level.insideMap(cell)) continue;
                            if (Dungeon.level.heroFOV[cell]) {
                                CellEmitter.get(cell).burst(ShadowParticle.UP, 2);
                            }
                            Char ch = Actor.findChar(cell);
                            if (isEnemy(ch)) {
                                Buff.prolong(ch, Terror.class, Terror.DURATION);
                                Buff.prolong(ch, Amok.class, 3f);
                                if (ch.sprite != null) {
                                    ch.sprite.showStatus(CharSprite.WARNING,
                                            Messages.get(PatternMine.class, "pattern_cross"));
                                }
                            }
                        }
                    }
                }, ShadowParticle.UP);
    }

    // ==================== 内部类型 ====================

    /** 二维整数坐标点。 */
    private static final class Point {
        final int x, y;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Point))
                return false;
            Point other = (Point) o;
            return x == other.x && y == other.y;
        }

        @Override
        public int hashCode() {
            return 31 * x + y;
        }

        @Override
        public String toString() {
            return "(" + x + "," + y + ")";
        }
    }

    /**
     * 已注册的图案定义。
     */
    private static final class PatternDef {
        final String name;
        final Set<Point> points;
        final BiConsumer<Integer, Integer> effect;
        final Emitter.Factory particle;

        PatternDef(String name, Set<Point> points,
                   BiConsumer<Integer, Integer> effect,
                   Emitter.Factory particle) {
            this.name = name;
            this.points = points;
            this.effect = effect;
            this.particle = particle;
        }

        /** 检查给定点集是否匹配此图案（支持 90° 旋转匹配）。 */
        boolean matches(Set<Point> candidate) {
            if (candidate.size() != points.size()) {
                return false;
            }
            // 尝试 0°、90°、180°、270° 旋转
            for (int r = 0; r < 4; r++) {
                Set<Point> rotated = rotateAll(candidate, r);
                if (rotated.equals(points)) {
                    return true;
                }
            }
            return false;
        }

        private Set<Point> rotateAll(Set<Point> pts, int times) {
            Set<Point> result = new HashSet<>();
            for (Point p : pts) {
                result.add(rotate(p, times));
            }
            return result;
        }

        private Point rotate(Point p, int times) {
            int x = p.x, y = p.y;
            for (int i = 0; i < times; i++) {
                int nx = -y;
                int ny = x;
                x = nx;
                y = ny;
            }
            return new Point(x, y);
        }
    }

    /**
     * 匹配结果。
     */
    private static final class MatchedShape {
        final PatternDef def;
        final int centerPos; // 图案中心位置

        MatchedShape(PatternDef def, int centerPos) {
            this.def = def;
            this.centerPos = centerPos;
        }
    }

    // ==================== Sprite ====================

    public static class Sprite extends MineSprite {
        public Sprite() {
            String tex = "cola/wang_chess.png";
            texture(tex);
            scale.set(0.5f);
            TextureFilm film = new TextureFilm(tex, 32, 32);

            idle = new Animation(4, true);
            idle.frames(film, 0, 1, 0,2);

            place = new Animation(10, false);
            place.frames(film, 5, 4, 3, 1, 0);

            disarm = new Animation(6, false);
            disarm.frames(film, 2, 1, 0);

            detonate = null;

            hardlight(0x9090FF);
            play(place);
        }

        @Override
        protected float shakeMagnitude() {
            return 1f;
        }

        @Override
        protected float detonateScaleTo() {
            return 4.0f;
        }

        @Override
        protected int detonateColor() {
            return 0x9090FF;
        }
    }
}
