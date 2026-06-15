package com.zootdungeon.arknights.misc;

import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.items.KindOfWeapon;

/**
 * 罗德岛系武器闪避事件的中央分发器。
 * 仅在 {@link com.zootdungeon.mechanics.Damage#physical} 的"miss"分支调用。
 * 放在这里是为了把对 Damage.java 的入侵降到最小（一行）。
 */
public final class RhodesDodgeHooks {

    private RhodesDodgeHooks() {}

    /** 当 defender 成功闪避了来自 attacker 的一次物理攻击时调用。 */
    public static void onDodge(Char attacker, Char defender) {
        if (attacker == null || defender == null) return;
        if (!(defender instanceof Hero)) return;
        if (attacker == defender) return;

        Hero hero = (Hero) defender;
        if (!hero.isAlive()) return;

        KindOfWeapon main = hero.belongings.weapon();
        KindOfWeapon off = hero.belongings.secondWep();

        if (main instanceof RhodesGauntlet) {
            RhodesGauntlet gauntlet = (RhodesGauntlet) main;
            gauntlet.onHeroDodged(hero, attacker);
            gauntlet.onHeroDodged(hero);
        } else if (off instanceof RhodesGauntlet) {
            RhodesGauntlet gauntlet = (RhodesGauntlet) off;
            gauntlet.onHeroDodged(hero, attacker);
            gauntlet.onHeroDodged(hero);
        }
    }
}
