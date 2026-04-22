package com.zootdungeon.arknights.misc;

import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.zootdungeon.Assets;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.buffs.DodgeBoostBuff;
import com.zootdungeon.actors.buffs.Invisibility;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.items.KindOfWeapon;
import com.zootdungeon.items.weapon.fastWeapon.FastWeapon;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.sprites.ItemSpriteSheet;

/**
 * 罗德岛·闪避拳套：
 *  - 装备时提升英雄的闪避值（通过 {@link DodgeBoostBuff} 实现）；
 *  - 每次成功闪避后会立刻触发一次“免费反击”（不消耗行动时间）。
 */
public class RhodesDodgeGauntlet extends FastWeapon implements DodgeBoostBuff.Source {

    {
        image = ItemSpriteSheet.GAUNTLETS;
        hitSound = Assets.Sounds.HIT;
        hitSoundPitch = 1.25f;

        tier = 0;
        DLY = 0.5f;
    }

    /** 闪避率加成：基础 +30%，每 tier +5%，每级 +2%，上限 +120%。 */
    public float selfEvasionBonus() {
        float bonus = 0.30f + 0.05f * Math.max(0, tier - 1) + 0.02f * buffedLvl();
        return Math.min(1.20f, bonus);
    }

    @Override
    public float dodgeBuffMultiplier() {
        return 1f + selfEvasionBonus();
    }

    @Override
    public boolean stillWieldedBy(Hero hero) {
        if (hero == null) return false;
        KindOfWeapon main = hero.belongings.weapon();
        KindOfWeapon off = hero.belongings.secondWep();
        return main == this || off == this;
    }

    @Override
    public void activate(Char ch) {
        super.activate(ch);
        if (ch instanceof Hero) {
            DodgeBoostBuff buff = Buff.affect(ch, DodgeBoostBuff.class);
            buff.setSource(this);
        }
    }

    /** Damage.physical 的 dodge 分支经 {@link RhodesDodgeHooks} 回调。 */
    public void onHeroDodged(Hero hero, Char attacker) {
        if (hero == null || attacker == null) return;
        if (!hero.isAlive() || !attacker.isAlive()) return;
        if (!hero.canAttack(attacker)) return;
        if (hero.buff(CounterTracker.class) != null) return;

        CounterTracker tracker = Buff.affect(hero, CounterTracker.class);
        tracker.enemy = attacker;
    }

    /**
     * 参考 Combo.RiposteTracker：在下一个 Actor tick 立刻攻击目标一次，随后 detach 并返回 false，
     * 不调用 spendAndNext，从而实现“无时间消耗反击”。
     */
    public static class CounterTracker extends Buff {
        { actPriority = VFX_PRIO; }

        public Char enemy;

        @Override
        public boolean act() {
            if (enemy == null || !enemy.isAlive() || !target.isAlive() || !(target instanceof Hero)) {
                detach();
                return true;
            }
            final Hero hero = (Hero) target;
            if (!hero.canAttack(enemy)) {
                detach();
                return true;
            }
            final Char en = enemy;
            final CounterTracker self = this;
            hero.sprite.attack(en.pos, new Callback() {
                @Override
                public void call() {
                    hero.attack(en, 1f, 0, Char.INFINITE_ACCURACY);
                    Invisibility.dispel();
                    Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG, 1f, 1.15f);
                    self.next();
                }
            });
            detach();
            return false;
        }
    }

    @Override
    public String desc() {
        int bonusPct = Math.round(selfEvasionBonus() * 100f);
        return Messages.get(this, "desc", bonusPct);
    }
}
