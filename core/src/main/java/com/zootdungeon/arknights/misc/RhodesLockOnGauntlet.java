package com.zootdungeon.arknights.misc;

import com.zootdungeon.Assets;
import com.zootdungeon.actors.Char;
import com.zootdungeon.items.weapon.fastWeapon.FastWeapon;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Bundle;

/**
 * 类似 {@link com.zootdungeon.items.weapon.fastWeapon.FastWeapon} 的快速武器：
 * 命中后锁定当前目标；对同一目标的连续命中会叠加伤害加成。
 */
public class RhodesLockOnGauntlet extends FastWeapon {

    private static final String BUNDLE_TARGET_ID = "lockon_target_id";
    private static final String BUNDLE_STACKS = "lockon_stacks";

    private int lockOnTargetId = -1;
    /** 连续命中同一目标的次数（用于计算下一次加成）。 */
    private int lockOnStacks = 0;

    {
        image = ItemSpriteSheet.GAUNTLETS;
        hitSound = Assets.Sounds.HIT_CRUSH;
        hitSoundPitch = 1.2f;

        // 这里用 FastWeapon 的模板数值，tier 可按需要再调
        tier = 0;
        DLY = 0.5f;
    }

    /**
     * 每层增伤比例：基础 10%，并随 tier 小幅成长。
     * - tier=1 -> 10%
     * - tier=5 -> 18%
     */
    private float bonusPerStack() {
        return 0.10f + 0.05f * Math.max(0, tier - 1);
    }

    @Override
    public int proc(Char attacker, Char defender, int damage) {
        if (defender == null) {
            lockOnTargetId = -1;
            lockOnStacks = 0;
            return super.proc(attacker, defender, damage);
        }

        int defId = defender.id();
        if (defId != lockOnTargetId) {
            lockOnTargetId = defId;
            lockOnStacks = 0;
        }

        // 首次命中不加成；对同一目标第 N 次命中时按 (N-1) 层加成
        float mult = 1f + lockOnStacks * bonusPerStack();
        int boosted = Math.round(damage * mult);

        lockOnStacks++;

        return super.proc(attacker, defender, boosted);
    }

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(BUNDLE_TARGET_ID, lockOnTargetId);
        bundle.put(BUNDLE_STACKS, lockOnStacks);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        lockOnTargetId = bundle.getInt(BUNDLE_TARGET_ID);
        lockOnStacks = bundle.getInt(BUNDLE_STACKS);
    }
}

