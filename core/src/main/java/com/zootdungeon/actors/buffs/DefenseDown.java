package com.zootdungeon.actors.buffs;

import com.zootdungeon.ui.BuffIndicator;

public class DefenseDown extends FlavourBuff {

    public static final float DURATION = 6f;

    {
        type = buffType.NEGATIVE;
        announced = true;
    }

    @Override
    public int icon() {
        return BuffIndicator.VULNERABLE;
    }

    @Override
    public float iconFadePercent() {
        return Math.max(0, (DURATION - visualcooldown()) / DURATION);
    }
}
