package com.zootdungeon.levels.entities.mines;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.buffs.Amok;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.buffs.FlavourBuff;
import com.zootdungeon.actors.buffs.Cripple;
import com.zootdungeon.actors.buffs.Slow;
import com.zootdungeon.actors.buffs.Terror;
import com.zootdungeon.actors.buffs.Vertigo;
import com.zootdungeon.actors.buffs.Weakness;
import com.zootdungeon.effects.CellEmitter;
import com.zootdungeon.effects.particles.BlastParticle;
import com.zootdungeon.effects.particles.EnergyParticle;
import com.zootdungeon.effects.particles.ShadowParticle;
import com.zootdungeon.effects.particles.SparkParticle;
import com.zootdungeon.levels.entities.CellEntity;
import com.zootdungeon.levels.entities.CellEntitySprite;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.sprites.CharSprite;
import com.watabou.noosa.Image;

/**
 * 图案地雷（共鸣型）。
 * <p>
 * 被踩中或主动引爆时，扫描周围 {@link #scanRadius()} 格内的所有同类地雷，
 * 将自身与它们的相对位置归一化为图案，与预定义形状集逐一比对。
 * 匹配成功时在普通爆炸伤害之外触发<strong>特殊共鸣效果</strong>：
 * <ul>
 *   <li><b>L 形（3 格）</b>：对 3×3 范围内所有敌方施加 {@link Cripple}（3 回合）。</li>
 *   <li><b>T 形（4 格）</b>：对 4×4 范围内所有敌方施加 {@link Slow}（3 回合）。</li>
 *   <li><b>直线形（3 格）</b>：沿直线方向产生冲击波，推开最多 4 格路径上的所有敌方。</li>
 *   <li><b>对角线形（3 格）</b>：对 3×3 范围内所有敌方施加 {@link Weakness}（4 回合）。</li>
 *   <li><b>菱形（4 格）</b>：对 5×5 范围内所有敌方施加 {@link Vertigo}（3 回合）。</li>
 *   <li><b>十字形（5 格）</b>：对 5×5 范围内所有敌方施加 {@link Terror}（持续至受伤），并附加
 *       {@link Amok}（3 回合），使敌人相互攻击。</li>
 * </ul>
 * 无匹配时退化为普通 3×3 爆炸伤害（{@link #baseDamage()}）。
 * <p>
 * 只有 {@link Char.Alignment#ENEMY} 能触发，英雄/盟友安全。
 */
public class PatternMine extends Mine {

    // ==== Sprite ====

    @Override
    public Class<? extends CellEntitySprite> spriteClass() {
        return Sprite.class;
    }

    // ==== Tuning ====

    /** 扫描周围同类地雷的最大半径（格）。 */
    public int scanRadius() {
        return 3;
    }

    /** 普通爆炸对单体的伤害。 */
    public int baseDamage() {
        return 12;
    }

    /** 图案共鸣波及半径。图案越大此值越大。 */
    public int effectRadius(Shape shape) {
        switch (shape) {
            case L:
            case DIAGONAL:
                return 1;
            case T:
            case DIAMOND:
            case CROSS:
                return 2;
            case LINE_H:
            case LINE_V:
                return 2;
            default:
                return 1;
        }
    }

    // ==== Triggers ====

    @Override
    public void onStep(Char who) {
        if (isEnemy(who)) {
            detonate();
        }
    }

    // ==== Core ====

    @Override
    protected void onDetonate() {
        if (Dungeon.level == null) {
            return;
        }

        // 1. 收集周围同类地雷
        List<PatternMine> group = collectGroup();

        // 2. 归一化坐标
        Set<Point> normalized = normalize(group, this.pos);

        // 3. 形状匹配
        Shape shape = matchShape(normalized);

        // 4. 基础爆炸伤害（3×3 范围）
        applyBaseBlast();

        // 5. 共鸣特殊效果
        if (shape != null) {
            applyPatternEffect(shape, group);
        }
    }

    // ---------- Collection ----------

    private List<PatternMine> collectGroup() {
        List<PatternMine> group = new ArrayList<>();
        int radius = Math.max(1, scanRadius());

        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int cell = pos + dx + dy * Dungeon.level.width();
                if (!Dungeon.level.insideMap(cell)) {
                    continue;
                }
                CellEntity entity = Dungeon.level.cellEntityAt(cell);
                if (entity instanceof PatternMine && entity != this) {
                    group.add((PatternMine) entity);
                }
            }
        }
        return group;
    }

    // ---------- Normalization ----------

    /**
     * 将 group 中所有地雷的坐标归一化到以 detonator 为原点的坐标系中。
     * 自身不在结果中（原点恒为 (0,0)）。
     */
    private Set<Point> normalize(List<PatternMine> group, int center) {
        int w = Dungeon.level.width();
        int cx = center % w;
        int cy = center / w;

        Set<Point> points = new HashSet<>();
        for (PatternMine m : group) {
            int mx = m.pos % w;
            int my = m.pos / w;
            points.add(new Point(mx - cx, my - cy));
        }
        return points;
    }

    // ---------- Pattern matching ----------

    private Shape matchShape(Set<Point> points) {
        for (Shape candidate : Shape.values()) {
            if (matches(points, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean matches(Set<Point> points, Shape shape) {
        int targetSize = shape.points.size();

        // 首先检查数量：必须恰好匹配
        // 图案中的点数不含原点，所以实际收集的地雷数应该等于 targetSize
        if (points.size() != targetSize) {
            return false;
        }

        // 逐个旋转尝试
        for (int r = 0; r < shape.symmetries; r++) {
            Set<Point> rotated = rotateAll(points, r);
            if (rotated.equals(shape.points)) {
                return true;
            }
        }
        return false;
    }

    private Set<Point> rotateAll(Set<Point> points, int times) {
        Set<Point> result = new HashSet<>();
        for (Point p : points) {
            result.add(rotate(p, times));
        }
        return result;
    }

    /** 顺时针旋转 (x,y) 90° × times。 */
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

    // ---------- Pattern effects ----------

    private void applyPatternEffect(Shape shape, List<PatternMine> group) {
        int radius = Math.max(1, effectRadius(shape));

        switch (shape) {
            case L:
                applyCrippleEffect(radius);
                break;
            case T:
                applySlowEffect(radius);
                break;
            case LINE_H:
            case LINE_V:
                applyBlastWaveEffect(shape, group);
                break;
            case DIAGONAL:
                applyWeaknessEffect(radius);
                break;
            case DIAMOND:
                applyVertigoEffect(radius);
                break;
            case CROSS:
                applyTerrorEffect(radius);
                break;
        }
    }

    // L-shape: Cripple 3 turns
    private void applyCrippleEffect(int radius) {
        applyBuffToArea(Cripple.class, radius, 3f);
    }

    // T-shape: Slow 3 turns
    private void applySlowEffect(int radius) {
        applyBuffToArea(Slow.class, radius, 3f);
    }

    // Diagonal: Weakness 4 turns
    private void applyWeaknessEffect(int radius) {
        applyBuffToArea(Weakness.class, radius, 4f);
    }

    // Diamond: Vertigo 3 turns
    private void applyVertigoEffect(int radius) {
        applyBuffToArea(Vertigo.class, radius, 3f);
    }

    // Cross: Terror + Amok 3 turns
    private void applyTerrorEffect(int radius) {
        int width = Dungeon.level.width();
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int cell = pos + dx + dy * width;
                if (!Dungeon.level.insideMap(cell)) {
                    continue;
                }
                if (Dungeon.level.heroFOV[cell]) {
                    CellEmitter.get(cell).burst(ShadowParticle.UP, 2);
                }
                Char ch = Actor.findChar(cell);
                if (isEnemy(ch)) {
                    Buff.prolong(ch, Terror.class, Terror.DURATION);
                    Buff.prolong(ch, Amok.class, 3f);
                    if (ch.sprite != null) {
                        ch.sprite.showStatus(CharSprite.WARNING,
                                Messages.get(this, "pattern_cross"));
                    }
                }
            }
        }
    }

    // Line: directional blast wave — push all enemies along the line direction
    private void applyBlastWaveEffect(Shape shape, List<PatternMine> group) {
        int dir = determineDirection(shape, group);
        int w = Dungeon.level.width();

        int step = 0;
        int maxStep = 4;
        int cur = pos;
        while (step < maxStep) {
            int next;
            if (shape == Shape.LINE_V) {
                next = cur + (dir < 0 ? -w : +w);
            } else {
                next = cur + (dir < 0 ? -1 : +1);
            }
            if (!Dungeon.level.insideMap(next)) {
                break;
            }
            if (Dungeon.level.solid[next]) {
                break;
            }
            if (Dungeon.level.heroFOV[next]) {
                CellEmitter.center(next).burst(SparkParticle.FACTORY, 3);
            }
            Char ch = Actor.findChar(next);
            if (isEnemy(ch)) {
                ch.damage(Math.max(0, baseDamage() / 2), this);
                if (ch.isAlive()) {
                    ch.sprite.showStatus(CharSprite.NEGATIVE,
                            Messages.get(this, "pattern_line"));
                }
            }
            cur = next;
            step++;
        }
    }

    private int determineDirection(Shape shape, List<PatternMine> group) {
        int w = Dungeon.level.width();
        int cx = pos % w;
        int cy = pos / w;

        int minX = cx, maxX = cx, minY = cy, maxY = cy;
        for (PatternMine m : group) {
            int mx = m.pos % w;
            int my = m.pos / w;
            minX = Math.min(minX, mx);
            maxX = Math.max(maxX, mx);
            minY = Math.min(minY, my);
            maxY = Math.max(maxY, my);
        }

        if (shape == Shape.LINE_H) {
            return (maxX > cx) ? +1 : -1;
        } else {
            return (maxY > cy) ? +1 : -1;
        }
    }

    // ---------- Helpers ----------

    private <T extends FlavourBuff> void applyBuffToArea(
            Class<T> buffClass, int radius, float duration) {
        int width = Dungeon.level.width();
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int cell = pos + dx + dy * width;
                if (!Dungeon.level.insideMap(cell)) {
                    continue;
                }
                if (Dungeon.level.heroFOV[cell]) {
                    CellEmitter.get(cell).burst(EnergyParticle.FACTORY, 1);
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

    // ---------- Description ----------

    @Override
    public String desc() {
        return Messages.get(this, "desc", baseDamage());
    }

    // ==== Inner types ====

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
     * 预定义图案。points 中不含原点 (0,0)，原点由引爆的地雷自身充当。
     * symmetries 表示通过 90° 旋转可达的等价形态数量，用于剪枝。
     */
    enum Shape {
        /** L 形：3 个点（含原点）。对称 4。 */
        L(new Point(-1, 0), new Point(-1, -1)),

        /** T 形：4 个点（含原点）。对称 4。 */
        T(new Point(-1, 0), new Point(+1, 0), new Point(0, -1)),

        /** 水平直线形：3 点在一条水平线上（自身 + 2 点）。对称 2。 */
        LINE_H(new Point(-1, 0), new Point(+1, 0)),

        /** 垂直直线形：3 点在一条垂直线上（自身 + 2 点）。对称 2。 */
        LINE_V(new Point(0, -1), new Point(0, +1)),

        /** 对角线形：3 点在斜线上（自身 + 2 点）。对称 2。 */
        DIAGONAL(new Point(-1, -1), new Point(+1, +1)),

        /** 菱形：4 个点形成转 45° 的正方形（自身 + 3 点）。对称 4。 */
        DIAMOND(new Point(-1, 0), new Point(0, -1), new Point(+1, 0)),

        /** 十字形：5 个点（自身 + 4 个方向各 1 格）。对称 4。 */
        CROSS(new Point(-1, 0), new Point(+1, 0), new Point(0, -1), new Point(0, +1));

        /** 图案点集（不含原点）。 */
        final Set<Point> points;

        /** 90° 旋转对称次数（1~4），用于剪枝匹配尝试。 */
        final int symmetries;

        Shape(Point... pts) {
            this.points = new HashSet<>();
            for (Point p : pts) {
                this.points.add(p);
            }
            this.symmetries = initSymmetries(this);
        }

        private static int initSymmetries(Shape s) {
            if (s == LINE_H || s == LINE_V || s == DIAGONAL) {
                return 2;
            }
            return 4;
        }
    }

    public static class Sprite extends CellEntitySprite {
        public Sprite() {
            texture("cola/wang_chess.png");
            scale.set(0.5f);
            perspectiveRaise = 1 / 16f;
        }
    }
}
