package com.zootdungeon.levels.entities.mines;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.effects.CellEmitter;
import com.zootdungeon.effects.particles.BlastParticle;
import com.zootdungeon.levels.entities.CellEntity;
import com.zootdungeon.levels.entities.CellEntitySprite;
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
        return CrossMineSprite.class;
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
        CellEntity other = Dungeon.level.cellEntityAt(cell);
        if (other instanceof Mine && other != this) {
            ((Mine) other).detonate();
        }
    }

    @Override
    public String desc() {
        return Messages.get(this, "desc", reach(), damage());
    }
}
