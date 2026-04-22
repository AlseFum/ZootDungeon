package com.zootdungeon.arknights.misc;

import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;
import com.zootdungeon.Assets;
import com.zootdungeon.actors.Char;
import com.zootdungeon.items.weapon.fastWeapon.FastWeapon;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.sprites.CharSprite;
import com.zootdungeon.sprites.ItemSpriteSheet;

/**
 * 罗德岛·暴击拳套：
 *  - 每次命中有概率提升本次攻击伤害（暴击）；
 *  - 触发概率与倍率均随 tier / 等级提升。
 */
public class RhodesCritGauntlet extends FastWeapon {

    {
        image = ItemSpriteSheet.GAUNTLETS;
        hitSound = Assets.Sounds.HIT_STRONG;
        hitSoundPitch = 1.05f;

        tier = 0;
        DLY = 0.5f;
    }

    /** 暴击概率：基础 15%，每 tier +4%，每级 +1.5%，上限 75%。 */
    public float critChance() {
        float c = 0.15f + 0.04f * Math.max(0, tier - 1) + 0.015f * buffedLvl();
        return Math.min(0.75f, c);
    }

    /** 暴击倍率：基础 1.75x，每 tier +0.1，每级 +0.05，上限 3.0x。 */
    public float critMultiplier() {
        float m = 1.75f + 0.10f * Math.max(0, tier - 1) + 0.05f * buffedLvl();
        return Math.min(3.0f, m);
    }

    @Override
    public int proc(Char attacker, Char defender, int damage) {
        int dmg = damage;
        if (defender != null && defender.isAlive() && Random.Float() < critChance()) {
            dmg = Math.round(dmg * critMultiplier());
            if (defender.sprite != null) {
                defender.sprite.showStatus(CharSprite.NEGATIVE, Messages.get(this, "crit_status"));
                defender.sprite.flash();
            }
            Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG, 1f, 0.95f);
        }
        return super.proc(attacker, defender, dmg);
    }

    @Override
    public String desc() {
        int pct = Math.round(critChance() * 100f);
        int multPct = Math.round(critMultiplier() * 100f);
        return Messages.get(this, "desc", pct, multPct);
    }
}
