package com.zootdungeon.levels.entities.mines;

import java.util.ArrayList;

import com.watabou.utils.BArray;
import com.watabou.utils.PathFinder;
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.buffs.Blindness;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.buffs.Paralysis;
import com.zootdungeon.effects.CellEmitter;
import com.zootdungeon.effects.particles.BlastParticle;
import com.zootdungeon.levels.entities.CellEntitySprite;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.sprites.CharSprite;

/**
 * 感应（闪光）地雷。
 * <p>
 * 每个 tick 检查以自身为中心的 3×3 格子：一旦出现任何敌方角色就立即爆炸。
 * 爆炸不追求击杀，而是把 3×3 内的所有敌方
 * <ul>
 *     <li>施加 {@link Paralysis}（麻痹）持续 {@link #paralysisDuration()} 回合；</li>
 *     <li>附带短时间 {@link Blindness}，表达「闪光」语义。</li>
 * </ul>
 * 伤害较低（仅为「震爆」象征值），主要用于压制。
 */
public class ProximityMine extends Mine {

    @Override
    public Class<? extends CellEntitySprite> spriteClass() {
        return ProximityMineSprite.class;
    }

    /** 爆炸波及半径（格，用来构造 3×3 影响范围）。 */
    public int sensorRadius() {
        return 1;
    }

    /** 麻痹持续回合数。 */
    public float paralysisDuration() {
        return 4f;
    }

    /** 闪光（致盲）持续回合数。 */
    public float blindnessDuration() {
        return 3f;
    }

    /** 象征性伤害（体现「震爆」而非击杀）。 */
    public int flashDamage() {
        return 4;
    }

    @Override
    protected boolean act() {
        // 每个 tick 扫一次 3×3，防止敌人在没有 occupyCell 调用的情况下静默移入。
        if (!detonated && shouldTrigger()) {
            detonate();
            return true;
        }
        return super.act();
    }

    /** onStep 不单独触发（act 已经扫到），但如果直接被踩也立即爆。 */
    @Override
    public void onStep(Char who) {
        if (isEnemy(who)) {
            detonate();
        }
    }

    private boolean shouldTrigger() {
        if (Dungeon.level == null) {
            return false;
        }
        PathFinder.buildDistanceMap(pos,
                BArray.not(Dungeon.level.solid, null),
                sensorRadius());

        for (int cell = 0; cell < PathFinder.distance.length; cell++) {
            if (PathFinder.distance[cell] == Integer.MAX_VALUE) {
                continue;
            }
            Char ch = Actor.findChar(cell);
            if (isEnemy(ch)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onDetonate() {
        if (Dungeon.level == null) {
            return;
        }
        PathFinder.buildDistanceMap(pos,
                BArray.not(Dungeon.level.solid, null),
                sensorRadius());

        ArrayList<Char> victims = new ArrayList<>();
        for (int cell = 0; cell < PathFinder.distance.length; cell++) {
            if (PathFinder.distance[cell] == Integer.MAX_VALUE) {
                continue;
            }
            if (Dungeon.level.heroFOV[cell]) {
                CellEmitter.center(cell).burst(BlastParticle.FACTORY, 3);
            }
            Char ch = Actor.findChar(cell);
            if (isEnemy(ch)) {
                victims.add(ch);
            }
        }

        int dmg = Math.max(0, flashDamage());
        float paraTurns = Math.max(0f, paralysisDuration());
        float blindTurns = Math.max(0f, blindnessDuration());

        for (Char victim : victims) {
            if (dmg > 0) {
                victim.damage(dmg, this);
            }
            if (!victim.isAlive()) {
                continue;
            }
            if (paraTurns > 0f) {
                Buff.prolong(victim, Paralysis.class, paraTurns);
            }
            if (blindTurns > 0f) {
                Buff.prolong(victim, Blindness.class, blindTurns);
            }
            if (victim.sprite != null) {
                victim.sprite.showStatus(CharSprite.NEGATIVE,
                        Messages.get(this, "status"));
            }
        }
    }

    @Override
    public String desc() {
        return Messages.get(this, "desc",
                sensorRadius() * 2 + 1,
                flashDamage(),
                (int) paralysisDuration(),
                (int) blindnessDuration());
    }
}
