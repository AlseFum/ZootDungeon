package com.zootdungeon.actors.buffs;

import java.util.ArrayList;

import com.watabou.utils.BArray;
import com.watabou.utils.PathFinder;
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.effects.CellEmitter;
import com.zootdungeon.effects.particles.BlastParticle;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.sprites.CharSprite;
import com.zootdungeon.ui.BuffIndicator;

/**
 * {@link StackingTriggerBuff} 的示例实现。
 * <p>
 * 行为：
 * <ol>
 *   <li>叠层到 {@link #threshold()} 时「爆发」：在附着单位所在格为中心、半径
 *       {@link #explosionRadius()} 的 FOV 区域里，对所有与目标<strong>同阵营</strong>
 *       （{@code alignment} 相同）的 {@link Char} 造成 {@link #explosionDamage()}
 *       点伤害。<br>
 *       伤害走 {@link Char#damage(int, Object)}，不经物理 DR 管线，因此「不受减免」。
 *   </li>
 *   <li>爆发后清零叠层，附加 {@link ExplosiveStackCooldown} 冷却
 *       {@link #cooldownDuration()} 回合。</li>
 *   <li>冷却期间若再有 {@link #tryAddStacks(int)}，不再累加叠层，而是直接对附着单位
 *       造成 {@code amount ×} {@link #backlashDamagePerStack()} 的<strong>无视防御</strong>
 *       伤害（同样走 {@code Char#damage}，绕过物理 DR）。</li>
 * </ol>
 */
public class ExplosiveStackBuff extends StackingTriggerBuff {

    {
        type = buffType.NEUTRAL;
    }

    // ====== 可覆写参数 ======

    @Override
    public int threshold() {
        return 5;
    }

    @Override
    public Class<? extends TriggerCooldownBuff> cooldownBuffClass() {
        return ExplosiveStackCooldown.class;
    }

    @Override
    public float cooldownDuration() {
        return 10f;
    }

    /** 爆发时对同阵营目标造成的单体伤害（不走 DR）。 */
    public int explosionDamage() {
        return 15;
    }

    /** 爆发波及半径（格）。 */
    public int explosionRadius() {
        return 2;
    }

    /** 冷却期再次尝试叠层时，每点层数造成的无视防御伤害。 */
    public int backlashDamagePerStack() {
        return 4;
    }

    // ====== 核心行为 ======

    @Override
    public void onTrigger(Char target) {
        if (target == null || Dungeon.level == null) return;

        int origin = target.pos;
        Char.Alignment myAlignment = target.alignment;

        // 以目标所在格为中心做一次「可通行」半径扩散，得到影响区域。
        PathFinder.buildDistanceMap(
                origin,
                BArray.not(Dungeon.level.solid, null),
                explosionRadius());

        ArrayList<Char> victims = new ArrayList<>();
        int dmg = Math.max(0, explosionDamage());

        for (int cell = 0; cell < PathFinder.distance.length; cell++) {
            if (PathFinder.distance[cell] == Integer.MAX_VALUE) continue;

            if (Dungeon.level.heroFOV[cell]) {
                CellEmitter.center(cell).burst(BlastParticle.FACTORY, 4);
            }

            Char ch = Actor.findChar(cell);
            if (ch != null && ch.isAlive() && ch.alignment == myAlignment) {
                victims.add(ch);
            }
        }

        for (Char ch : victims) {
            ch.damage(dmg, this);
            if (ch.sprite != null) {
                ch.sprite.showStatus(CharSprite.NEGATIVE,
                        Messages.get(this, "explode_status", dmg));
            }
        }
    }

    @Override
    protected void onBlockedByCooldown(int blockedAmount) {
        if (target == null || !target.isAlive()) return;
        int dmg = Math.max(0, blockedAmount * backlashDamagePerStack());
        if (dmg <= 0) return;

        target.damage(dmg, this);
        if (target.sprite != null) {
            target.sprite.showStatus(CharSprite.WARNING,
                    Messages.get(this, "backlash_status", dmg));
        }
    }

    // ====== UI ======

    @Override
    public int icon() {
        return BuffIndicator.CORRUPT;
    }

    @Override
    public String desc() {
        return Messages.get(this, "desc",
                stacks(),
                threshold(),
                explosionDamage(),
                explosionRadius(),
                dispTurns(cooldownDuration()),
                backlashDamagePerStack());
    }

    // ====== 配对冷却 buff ======

    /**
     * {@link ExplosiveStackBuff} 的配对冷却。只要它还附着，
     * {@link ExplosiveStackBuff#tryAddStacks(int)} 就不会再累加层数，
     * 而是直接对宿主造成无视防御的反噬伤害。
     */
    public static class ExplosiveStackCooldown extends TriggerCooldownBuff {

        {
            type = buffType.NEGATIVE;
            announced = false;
        }

        @Override
        public int icon() {
            return BuffIndicator.TIME;
        }
    }
}
