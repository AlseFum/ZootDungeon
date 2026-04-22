package com.zootdungeon.actors.buffs;

import com.watabou.noosa.Image;
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.items.KindOfWeapon;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.ui.BuffIndicator;

/**
 * 闪避率提升 Buff：由装备时的武器（如 RhodesDodgeGauntlet）在 activate 中附着。
 * <p>
 * {@link com.zootdungeon.actors.hero.Hero#defenseSkill} 会查询本 Buff 并把
 * {@link #multiplier()} 的返回值乘到英雄的闪避计算上。
 * <p>
 * Buff 自身不存倍率常量，而是持有对来源武器的弱引用，在每次被查询时动态读取。
 * 这样武器升级/强化后倍率能自动跟进；武器被卸下时，本 Buff 会在 act() 中自行 detach。
 */
public class DodgeBoostBuff extends Buff {

    {
        type = buffType.POSITIVE;
        announced = false;
    }

    public interface Source {
        /** 提供当前的闪避倍率（>=1f）。 */
        float dodgeBuffMultiplier();
        /** 某个角色当前是否实际持有本 Source（主副手都算）。 */
        boolean stillWieldedBy(Hero hero);
    }

    private Source source;

    public void setSource(Source src) {
        this.source = src;
    }

    public float multiplier() {
        if (source == null) return 1f;
        float m = source.dodgeBuffMultiplier();
        return m < 1f ? 1f : m;
    }

    @Override
    public boolean act() {
        if (!(target instanceof Hero)) {
            detach();
            return true;
        }
        Hero hero = (Hero) target;
        if (source == null || !source.stillWieldedBy(hero)) {
            detach();
            return true;
        }
        spend(TICK);
        return true;
    }

    @Override
    public int icon() {
        // 没装备任何能触发本 Buff 的武器时不显示图标
        if (Dungeon.hero != null) {
            KindOfWeapon w1 = Dungeon.hero.belongings.weapon();
            KindOfWeapon w2 = Dungeon.hero.belongings.secondWep();
            if (source != null && (w1 == source || w2 == source)) {
                return BuffIndicator.MOMENTUM;
            }
        }
        return BuffIndicator.NONE;
    }

    @Override
    public void tintIcon(Image icon) {
        icon.hardlight(0.35f, 0.85f, 1.0f);
    }

    @Override
    public String iconTextDisplay() {
        int pct = Math.round((multiplier() - 1f) * 100f);
        return "+" + pct + "%";
    }

    @Override
    public String desc() {
        int pct = Math.round((multiplier() - 1f) * 100f);
        return Messages.get(this, "desc", pct);
    }

    @Override
    public String toString() {
        return Messages.get(this, "name");
    }
}
