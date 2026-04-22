package com.zootdungeon.arknights.misc;

import com.zootdungeon.Assets;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.buffs.AscensionChallenge;
import com.zootdungeon.actors.buffs.DefenseDown;
import com.zootdungeon.items.weapon.fastWeapon.FastWeapon;
import com.zootdungeon.sprites.ItemSpriteSheet;

/**
 * 快速拳套：每次命中会按比例「无视」目标防御（DR）。
 * <p>
 * 由于伤害流程里 DR 会在 {@link com.zootdungeon.actors.hero.Hero#attackProc} 之前扣除，
 * 这里通过「返还一部分 DR」来达到等效穿甲效果。
 */
public class RhodesArmorPierceGauntlet extends FastWeapon {

    {
        image = ItemSpriteSheet.GAUNTLETS;
        hitSound = Assets.Sounds.HIT_CRUSH;
        hitSoundPitch = 1.15f;

        tier = 0;
        DLY = 0.5f;
    }

    /**
     * 穿甲比例：基础 15%，并随 tier/强化等级成长，上限 75%。
     * - tier 越高：每档 +3%
     * - 强化等级越高：每级 +1%
     */
    private float pierceRate() {
        float p = 0.15f + 0.03f * Math.max(0, tier - 1) + 0.01f * Math.max(0, buffedLvl());
        if (p < 0f) return 0f;
        if (p > 0.75f) return 0.75f;
        return p;
    }

    private static int computeDrLikeDamageSystem(Char attacker, Char defender) {
        if (defender == null) return 0;
        int dr = Math.round(defender.drRoll() * AscensionChallenge.statModifier(defender));
        if (!defender.buffs(DefenseDown.class).isEmpty()) {
            dr /= 2;
        }
        // 这里不复刻 sniper/monk 等特殊分支：本武器为近战快速武器，默认按常规 DR 处理
        return Math.max(dr, 0);
    }

    @Override
    public int proc(Char attacker, Char defender, int damage) {
        int dr = computeDrLikeDamageSystem(attacker, defender);
        int bonus = Math.round(dr * pierceRate());
        return super.proc(attacker, defender, damage + bonus);
    }
}

