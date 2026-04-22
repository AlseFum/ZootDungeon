package com.zootdungeon.levels.entities.mines;

import java.util.ArrayList;

import com.watabou.utils.BArray;
import com.watabou.utils.PathFinder;
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.effects.CellEmitter;
import com.zootdungeon.effects.particles.BlastParticle;
import com.zootdungeon.levels.entities.CellEntitySprite;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.sprites.CharSprite;

/**
 * 遥爆地雷。
 * <p>
 * 与其它地雷不同，<b>敌人踩上去不会触发</b>；必须通过外部（玩家手里的
 * {@link com.zootdungeon.items.cheat.MinePlacer MinePlacer} 里的「远程引爆」动作，
 * 或其它任何持有其引用的代码）主动调用 {@link #detonate()}。
 * <p>
 * 爆炸效果以 3×3 为范围：
 * <ol>
 *     <li>对 3×3 内的<strong>所有敌方角色</strong>造成一次定值 {@link #baseDamage()}
 *         加上 {@link #bonusPerExtraTarget()} × (敌人数量 - 1) 的伤害。
 *         敌人越多，群伤越高，鼓励等敌人聚团后引爆。</li>
 *     <li>伤害走 {@link Char#damage(int, Object)}，不经物理 DR（与其它地雷一致）。</li>
 * </ol>
 */
public class RemoteMine extends Mine {

    @Override
    public Class<? extends CellEntitySprite> spriteClass() {
        return RemoteMineSprite.class;
    }

    /** 爆炸半径。默认 1 格，即 3×3。 */
    public int blastRadius() {
        return 1;
    }

    /** 基础伤害（无论有几个敌人都至少结算一次这个值）。 */
    public int baseDamage() {
        return 10;
    }

    /** 每多一个目标，为每人追加的额外伤害。 */
    public int bonusPerExtraTarget() {
        return 6;
    }

    /** 远程地雷对踩到的敌人完全无反应。 */
    @Override
    public void onStep(Char who) {
        // intentionally empty
    }

    @Override
    protected void onDetonate() {
        if (Dungeon.level == null) {
            return;
        }

        PathFinder.buildDistanceMap(pos,
                BArray.not(Dungeon.level.solid, null),
                blastRadius());

        ArrayList<Char> victims = new ArrayList<>();
        for (int cell = 0; cell < PathFinder.distance.length; cell++) {
            if (PathFinder.distance[cell] == Integer.MAX_VALUE) {
                continue;
            }
            if (Dungeon.level.heroFOV[cell]) {
                CellEmitter.center(cell).burst(BlastParticle.FACTORY, 4);
            }
            Char ch = Actor.findChar(cell);
            if (isEnemy(ch)) {
                victims.add(ch);
            }
        }

        if (victims.isEmpty()) {
            return;
        }

        int n = victims.size();
        int dmg = Math.max(0, baseDamage() + bonusPerExtraTarget() * (n - 1));

        for (Char victim : victims) {
            if (!victim.isAlive()) {
                continue;
            }
            victim.damage(dmg, this);
            if (victim.sprite != null) {
                victim.sprite.showStatus(CharSprite.NEGATIVE,
                        Messages.get(this, "status", dmg, n));
            }
        }
    }

    @Override
    public String desc() {
        return Messages.get(this, "desc",
                blastRadius() * 2 + 1,
                baseDamage(),
                bonusPerExtraTarget());
    }
}
