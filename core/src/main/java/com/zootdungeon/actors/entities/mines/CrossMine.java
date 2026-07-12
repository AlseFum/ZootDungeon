package com.zootdungeon.actors.entities.mines;

import com.watabou.noosa.TextureFilm;
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.effects.CellEmitter;
import com.zootdungeon.effects.particles.BlastParticle;
import com.zootdungeon.effects.particles.SparkParticle;
import com.zootdungeon.actors.Entity;
import com.zootdungeon.actors.entities.Mine;
import com.zootdungeon.sprites.CellEntitySprite;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.sprites.CharSprite;

/**
 * 十字地雷（连锁型）。
 * <p>
 * 敌方角色踩上即爆炸：沿上下左右四个方向各向外延伸 {@link #reach()} 格，
 * 对每一格的敌方造成 {@link #damage()} 点伤害。此外，如果途经格子上还有<strong>别的
 * {@link Mine}</strong>，则同样引爆它们 —— 由于
 * {@link Mine#detonate()} 自带去重，所以多枚十字雷互相连锁也不会无限递归。
 * <p>
 * 爆炸格沿路径遇到 {@link com.zootdungeon.levels.Level#solid 实心地形} 会截断，
 * 不会穿墙误伤墙后敌人。
 */
public class CrossMine extends Mine {

    @Override
    public Class<? extends CellEntitySprite> spriteClass() {
        return Sprite.class;
    }

    /** 每次沿单一方向延伸的格数（默认 2 格）。 */
    public int reach() {
        return 2;
    }

    /** 爆炸命中单体的伤害。 */
    public int damage() {
        return 15;
    }

    @Override
    public void onStep(Char who) {
        if (isEnemy(who)) {
            detonate();
        }
    }

    @Override
    protected void onDetonate() {
        if (Dungeon.level == null) {
            return;
        }
        int width = Dungeon.level.width();
        int reach = Math.max(1, reach());
        int dmg = Math.max(0, damage());

        // 中心爆炸特效（浓烈一点）
        if (Dungeon.level.heroFOV[pos]) {
            CellEmitter.center(pos).burst(BlastParticle.FACTORY, 6);
            CellEmitter.center(pos).burst(SparkParticle.FACTORY, 12);
        }

        // 中心格：可能此时敌人刚踩上来，也可能是被连锁引爆，安全起见先处理中心。
        damageAt(pos, dmg);

        int[] dirs = new int[] { -width, width, -1, +1 };
        for (int dir : dirs) {
            int cell = pos;
            for (int step = 1; step <= reach; step++) {
                cell += dir;
                if (!Dungeon.level.insideMap(cell)) {
                    break;
                }
                // 穿墙判定：碰到 solid 就截断。
                if (Dungeon.level.solid[cell]) {
                    break;
                }
                if (Dungeon.level.heroFOV[cell]) {
                    CellEmitter.center(cell).burst(BlastParticle.FACTORY, 3);
                    CellEmitter.center(cell).burst(SparkParticle.FACTORY, 6);
                }
                damageAt(cell, dmg);
                chainAt(cell);
            }
        }
    }

    /** 对某格上的敌方造成一次爆炸伤害。走 {@code Char.damage(int, Object)} 绕过物理 DR。 */
    private void damageAt(int cell, int dmg) {
        if (dmg <= 0) {
            return;
        }
        Char ch = Actor.findChar(cell);
        if (!isEnemy(ch)) {
            return;
        }
        ch.damage(dmg, this);
        if (ch.isAlive() && ch.sprite != null) {
            ch.sprite.showStatus(CharSprite.NEGATIVE,
                    Messages.get(this, "status", dmg));
        }
    }

    /**
     * 如果某格上还有其他 {@link Mine}，就顺带引爆它。
     * 利用 {@link Mine#detonate()} 的幂等性避免无限递归，
     * 对本雷自己亦自然 no-op。
     */
    private void chainAt(int cell) {
        Entity other = Dungeon.level.cellEntityAt(cell);
        if (other instanceof Mine && other != this) {
            ((Mine) other).detonate();
        }
    }

    @Override
    public String desc() {
        return Messages.get(this, "desc", reach(), damage());
    }

    // ===== Sprite =====

    public static class Sprite extends Mine.Sprite {
        public Sprite() {
            super();
            String tex = "cola/trashbin.png";
            texture(tex);
            TextureFilm film = new TextureFilm(tex, 16, 16);

            idle = new Animation(1, true);
            idle.frames(film, 0);

            place = new Animation(4, false);
            place.frames(film, 0, 1, 2, 3);

            disarm = new Animation(1, false);
            disarm.frames(film, 3);

            detonate = new Animation(1, false);
            detonate.frames(film, 3);

            hardlight(0xFF9040);
        }

        @Override
        protected int baseColor() {
            return 0xFF9040;
        }

        @Override
        protected float shakeMagnitude() {
            return 10f;
        }

        @Override
        protected float detonateScaleTo() {
            return 3.5f;
        }
    }
}
