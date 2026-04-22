package com.zootdungeon.arknights.misc;

import com.watabou.utils.Random;
import com.zootdungeon.Assets;
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.buffs.Cripple;
import com.zootdungeon.actors.buffs.Vertigo;
import com.zootdungeon.items.weapon.fastWeapon.FastWeapon;
import com.zootdungeon.items.wands.WandOfBlastWave;
import com.zootdungeon.mechanics.Ballistica;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.sprites.ItemSpriteSheet;

/**
 * 罗德岛·冲击拳套：
 *  - 命中时有概率将敌人推开若干格，并施加短暂的“残疾/眩晕”debuff，使其失能。
 *  - 概率随 tier 和等级增长。
 */
public class RhodesKnockbackGauntlet extends FastWeapon {

    {
        image = ItemSpriteSheet.GAUNTLETS;
        hitSound = Assets.Sounds.HIT_CRUSH;
        hitSoundPitch = 1.15f;

        tier = 0;
        DLY = 0.5f;
    }

    /** 触发概率：基础 20%，每 tier +5%，每级 +2%，上限 70%。 */
    public float triggerChance() {
        float c = 0.20f + 0.05f * Math.max(0, tier - 1) + 0.02f * buffedLvl();
        return Math.min(0.70f, c);
    }

    /** 击退力度：1 + tier/2 + lvl/4，最少 1 格。 */
    public int knockbackPower() {
        return Math.max(1, 1 + tier / 2 + buffedLvl() / 4);
    }

    /** 残疾持续回合：基础 2 + tier/2 + lvl/6。 */
    public float disableDuration() {
        return 2f + tier / 2f + buffedLvl() / 6f;
    }

    @Override
    public int proc(Char attacker, Char defender, int damage) {
        int dmg = super.proc(attacker, defender, damage);
        if (defender == null || !defender.isAlive()) return dmg;
        if (defender.properties().contains(Char.Property.IMMOVABLE)) {
            // 对不可移动目标退化为“只施加残疾”
            if (Random.Float() < triggerChance()) {
                Buff.prolong(defender, Cripple.class, disableDuration());
                Buff.prolong(defender, Vertigo.class, disableDuration());
            }
            return dmg;
        }
        if (Random.Float() < triggerChance()) {
            // 计算推开方向：从攻击者指向防守者
            int from = attacker != null ? attacker.pos : defender.pos;
            int direction = defender.pos - from;
            if (direction == 0) {
                // 自击场景：随机一个相邻方向
                direction = com.watabou.utils.PathFinder.NEIGHBOURS8[Random.Int(8)];
            }
            int targetCell = defender.pos + direction;
            Ballistica trajectory = new Ballistica(defender.pos, targetCell, Ballistica.MAGIC_BOLT);
            WandOfBlastWave.throwChar(defender, trajectory, knockbackPower(), false, true, this);

            if (defender.isAlive()) {
                float dur = disableDuration();
                Buff.prolong(defender, Cripple.class, dur);
                Buff.prolong(defender, Vertigo.class, dur);
            }
            // 顺便做一下地图更新，和 WandOfBlastWave.throwChar 内部一致
            if (Dungeon.level != null) Dungeon.observe();
        }
        return dmg;
    }

    @Override
    public String desc() {
        int pct = Math.round(triggerChance() * 100f);
        return Messages.get(this, "desc", pct, knockbackPower(), (int) disableDuration());
    }
}
