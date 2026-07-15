package com.zootdungeon.actors.mapDevice;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.buffs.Chill;
import com.watabou.utils.Bundle;

/**
 * 寒冷光环装置：周期性对周围敌人施加 {@link Chill} 效果。
 *
 * - 每 {@link #INTERVAL} 回合脉冲一次，范围内所有 {@link Char.Alignment#ENEMY} 目标
 *   获得 {@link #CHILL_DURATION} 回合的 Chill。
 * - 受攻击时无特殊行为（仅充作占格交互）。
 * - 继承 MapDevice 的 damage → receiveDamage 链，HP 不变。
 */
public class CryoFieldDevice extends MapDevice {

    private static final int RADIUS = 3;
    private static final int INTERVAL = 5;
    private static final float CHILL_DURATION = 5f;

    private int cooldown = 0;

    {
        spriteClass = MapDevice.Sprite.class;
        HP = HT = 30;
        properties.add(Property.IMMOVABLE);
        properties.add(Property.INORGANIC);
        properties.add(Property.STATIC);
    }

    public CryoFieldDevice() {
        super();
    }

    @Override
    protected boolean act() {
        if (cooldown <= 0) {
            for (Char ch : Actor.chars()) {
                if (ch == this || !ch.isAlive()) continue;
                if (Dungeon.level.distance(pos, ch.pos) > RADIUS) continue;
                if (ch.alignment == Alignment.ENEMY) {
                    Buff.affect(ch, Chill.class, CHILL_DURATION);
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
        // 寒冷装置受攻击时无反应；需要者子类可重写
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
