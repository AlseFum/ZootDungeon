package com.zootdungeon.actors.mapDevice;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.mechanics.Damage;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

/**
 * 伤害光环装置：周期性对周围敌人造成魔法伤害。
 *
 * - 每 {@link #INTERVAL} 回合脉冲一次，对范围内所有 {@link Char.Alignment#ENEMY}
 *   目标造成 {@link #DMG_MIN}–{@link #DMG_MAX} 魔法伤害。
 * - 伤害走 {@link Damage#additional} 链路，不触发反击、不带武器属性。
 * - 受攻击时无特殊行为。
 */
public class DamageAuraDevice extends MapDevice {

    private static final int RADIUS = 3;
    private static final int INTERVAL = 5;
    private static final int DMG_MIN = 5;
    private static final int DMG_MAX = 10;

    private int cooldown = 0;

    {
        spriteClass = MapDevice.Sprite.class;
        HP = HT = 30;
        properties.add(Property.IMMOVABLE);
        properties.add(Property.INORGANIC);
        properties.add(Property.STATIC);
    }

    public DamageAuraDevice() {
        super();
    }

    @Override
    protected boolean act() {
        if (cooldown <= 0) {
            for (Char ch : Actor.chars()) {
                if (ch == this || !ch.isAlive()) continue;
                if (Dungeon.level.distance(pos, ch.pos) > RADIUS) continue;
                if (ch.alignment == Alignment.ENEMY) {
                    int dmg = Random.NormalIntRange(DMG_MIN, DMG_MAX);
                    Damage.additional(this, ch, Damage.MAGIC, dmg, this);
                }
            }
            cooldown = INTERVAL;
        } else {
            cooldown--;
        }
        spend(TICK);
        return true;
    }

    @Override
    public void receiveDamage(Object src) {
        // 无特殊反应
    }

    private static final String COOLDOWN = "cooldown";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(COOLDOWN, cooldown);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        cooldown = bundle.getInt(COOLDOWN);
    }
}
