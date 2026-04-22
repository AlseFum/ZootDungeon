package com.zootdungeon.arknights.misc;

import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;
import com.zootdungeon.Assets;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.items.weapon.fastWeapon.FastWeapon;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.zootdungeon.ui.BuffIndicator;

/**
 * 罗德岛·穿甲拳套：
 *  - 每次命中都会在敌人身上施加“破甲”Debuff；层数可叠加，每层永久降低
 *    该敌人的一部分实际 DR（仅对本武器后续命中生效）。
 *  - 同时刷新持续时间。
 *
 * 机制说明：游戏原版 {@link com.zootdungeon.actors.buffs.DefenseDown} 只负责 DR/2，
 *          此处另外提供一个“按层数减免 DR 固定值”的 ArmorShatter 叠层 Buff。
 */
public class RhodesDefBreakerGauntlet extends FastWeapon {

    {
        image = ItemSpriteSheet.GAUNTLETS;
        hitSound = Assets.Sounds.HIT_CRUSH;
        hitSoundPitch = 1.3f;

        tier = 0;
        DLY = 0.5f;
    }

    /** 每层减少的 DR 固定值。基础 1，每 tier +1，每级 +0.5（四舍五入）。 */
    public int drReductionPerStack() {
        return Math.max(1, Math.round(1f + Math.max(0, tier - 1) + 0.5f * buffedLvl()));
    }

    /** 最大层数：基础 4，每 tier +1，每 2 级 +1，上限 12。 */
    public int maxStacks() {
        return Math.min(12, 4 + Math.max(0, tier - 1) + buffedLvl() / 2);
    }

    /** 持续时间（命中后刷新）：基础 6 + tier + lvl/3。 */
    public float debuffDuration() {
        return 6f + tier + buffedLvl() / 3f;
    }

    @Override
    public int proc(Char attacker, Char defender, int damage) {
        int dmg = damage;
        if (defender != null && defender.isAlive()) {
            // 先按“命中前的已有层数”把 DR 减免转换成额外伤害
            ArmorShatter existing = defender.buff(ArmorShatter.class);
            if (existing != null && existing.totalReduction() > 0) {
                dmg = dmg + existing.totalReduction();
            }
            // 再追加本次命中产生的新层
            ArmorShatter shatter = Buff.affect(defender, ArmorShatter.class);
            shatter.addStack(maxStacks(), drReductionPerStack(), debuffDuration());
        }
        return super.proc(attacker, defender, dmg);
    }

    @Override
    public String desc() {
        return Messages.get(this, "desc", maxStacks(), drReductionPerStack(), (int) debuffDuration());
    }

    /**
     * 叠层破甲 Debuff。
     * 无法直接 hook Char.drRoll，因此采用等价折算：在本武器 {@link #proc} 中，
     * 将当前层数对应的 DR 减免量作为额外伤害加回最终伤害，从而等效于该次命中
     * “忽略 stacks × perStackDr 点 DR”。
     */
    public static class ArmorShatter extends Buff {
        {
            type = buffType.NEGATIVE;
            announced = false;
        }

        public int stacks = 0;
        public int perStackDr = 1;
        private float remaining = 0f;

        public void addStack(int maxStacks, int dr, float duration) {
            if (stacks < maxStacks) stacks++;
            // 每次都以当前武器读到的值为准（支持升级/更换同系列武器）
            perStackDr = Math.max(perStackDr, dr);
            remaining = Math.max(remaining, duration);
            BuffIndicator.refreshHero();
        }

        /** 供伤害管线查询的“当前穿甲值”。 */
        public int totalReduction() {
            return stacks * perStackDr;
        }

        @Override
        public boolean act() {
            remaining -= TICK;
            spend(TICK);
            if (remaining <= 0f) {
                detach();
            }
            return true;
        }

        @Override
        public int icon() {
            return BuffIndicator.VULNERABLE;
        }

        @Override
        public void tintIcon(Image icon) {
            icon.hardlight(0.9f, 0.55f, 0.1f);
        }

        @Override
        public String iconTextDisplay() {
            return Integer.toString(stacks);
        }

        @Override
        public float iconFadePercent() {
            float full = 10f; // 视觉用的大致总时长
            return Math.max(0, (full - remaining) / full);
        }

        @Override
        public String desc() {
            return Messages.get(this, "desc", stacks, totalReduction(), (int) Math.ceil(remaining));
        }

        private static final String STACKS = "armorshatter_stacks";
        private static final String PER    = "armorshatter_per";
        private static final String REMAIN = "armorshatter_remain";

        @Override
        public void storeInBundle(Bundle bundle) {
            super.storeInBundle(bundle);
            bundle.put(STACKS, stacks);
            bundle.put(PER, perStackDr);
            bundle.put(REMAIN, remaining);
        }

        @Override
        public void restoreFromBundle(Bundle bundle) {
            super.restoreFromBundle(bundle);
            stacks = bundle.getInt(STACKS);
            perStackDr = bundle.getInt(PER);
            remaining = bundle.getFloat(REMAIN);
        }
    }
}
